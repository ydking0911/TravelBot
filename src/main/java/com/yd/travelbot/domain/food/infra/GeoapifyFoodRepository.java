package com.yd.travelbot.domain.food.infra;

import com.fasterxml.jackson.databind.JsonNode;
import com.yd.travelbot.domain.food.domain.entity.Food;
import com.yd.travelbot.domain.food.domain.repository.FoodRepository;
import com.yd.travelbot.global.config.GeoapifyConfig;
import com.yd.travelbot.global.resolver.GeoapifyCityResolver;
import com.yd.travelbot.global.util.JsonUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Repository
@RequiredArgsConstructor
public class GeoapifyFoodRepository implements FoodRepository {

    private final GeoapifyConfig geoapifyConfig;
    // 타임아웃 적용 클라이언트 (연결/읽기/쓰기 각 12초)
    private final OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(java.time.Duration.ofSeconds(12))
            .readTimeout(java.time.Duration.ofSeconds(12))
            .writeTimeout(java.time.Duration.ofSeconds(12))
            .build();
    private final GeoapifyCityResolver cityResolver;
    private static final String GEOAPIFY_PLACES_API_BASE = "https://api.geoapify.com/v2/places";
    private static final String GEOAPIFY_GEOCODE_API_BASE = "https://api.geoapify.com/v1/geocode/search";

    @Override
    public List<Food> search(String city, String cuisine) {
        try {
            // 1단계: 공통 CityResolver로 도시 좌표 얻기 (국가 힌트 없음)
            Double[] cityCoordinates = cityResolver.resolveCoordinates(city, null);
            if (cityCoordinates == null) {
                log.warn("도시 좌표를 찾을 수 없음: {}, 기본 데이터 반환", city);
                return getDefaultFoods(city, cuisine);
            }
            
            double lon = cityCoordinates[0];
            double lat = cityCoordinates[1];
            log.info("도시 {}의 좌표: ({}, {})", city, lat, lon);
            
            // 2단계: Places API로 음식점 검색 (도시 중심 10km 반경)
            String category = cuisine != null ? "catering.restaurant" : "catering";
            okhttp3.HttpUrl.Builder urlBuilder = okhttp3.HttpUrl.parse(GEOAPIFY_PLACES_API_BASE).newBuilder()
                    .addQueryParameter("categories", category)
                    .addQueryParameter("filter", String.format("circle:%f,%f,10000", lon, lat))
                    .addQueryParameter("limit", "20")
                    .addQueryParameter("apiKey", geoapifyConfig.getApiKey());
            
            String url = urlBuilder.build().toString();
            log.info("Geoapify Places API 요청 URL: {}", url);

            Request request = new Request.Builder()
                    .url(url)
                    .get()
                    .build();

            try (Response response = executeWithRetry(request, 2)) { // 최대 2회 재시도
                if (!response.isSuccessful()) {
                    String errorBody = response.body() != null ? response.body().string() : "";
                    log.error("Geoapify Places API 호출 실패 (status: {}): {}", response.code(), errorBody);
                    log.warn("기본 데이터 반환");
                    return getDefaultFoods(city, cuisine);
                }

                String responseBody = response.body().string();
                log.info("Geoapify Places API 응답 (음식점): {}", responseBody.substring(0, Math.min(500, responseBody.length())));
                JsonNode jsonNode = JsonUtil.fromJson(responseBody, JsonNode.class);
                List<Food> foods = parseGeoapifyResponse(jsonNode, city, cuisine);
                
                log.info("Geoapify API에서 파싱된 음식점 개수: {}", foods.size());
                
                // API 응답이 비어있거나 결과가 적을 때 (5개 미만) 기본 데이터로 보충
                if (foods.isEmpty()) {
                    log.warn("Geoapify API 응답이 비어있음, 기본 데이터 반환");
                    return getDefaultFoods(city, cuisine);
                } else if (foods.size() < 5) {
                    log.warn("Geoapify API 응답이 적음 ({}개), 기본 데이터로 보충", foods.size());
                    // 기본 데이터를 추가하여 최소 10개 유지
                    List<Food> defaultFoods = getDefaultFoods(city, cuisine);
                    foods.addAll(defaultFoods.subList(0, Math.min(10 - foods.size(), defaultFoods.size())));
                }
                
                return foods;
            }
        } catch (Exception e) {
            log.error("음식점 검색 실패: {}", e.getMessage(), e);
            return getDefaultFoods(city, cuisine);
        }
    }
    
    @Override
    public List<Food> searchNearby(Double latitude, Double longitude, Double radius) {
        try {
            // URL 인코딩을 위해 HttpUrl.Builder 사용
            okhttp3.HttpUrl.Builder urlBuilder = okhttp3.HttpUrl.parse(GEOAPIFY_PLACES_API_BASE).newBuilder()
                    .addQueryParameter("categories", "catering.restaurant")
                    .addQueryParameter("filter", String.format("circle:%f,%f,%f", longitude, latitude, radius * 1000))
                    .addQueryParameter("limit", "20")
                    .addQueryParameter("apiKey", geoapifyConfig.getApiKey());
            
            String url = urlBuilder.build().toString();
            log.info("Geoapify Places API 요청 URL (주변 검색): {}", url);

            Request request = new Request.Builder()
                    .url(url)
                    .get()
                    .build();

            try (Response response = executeWithRetry(request, 2)) { // 최대 2회 재시도
                if (!response.isSuccessful()) {
                    String errorBody = response.body() != null ? response.body().string() : "";
                    log.error("Geoapify API 호출 실패 (status: {}): {}", response.code(), errorBody);
                    log.warn("기본 데이터 반환");
                    return getDefaultFoodsNearby(latitude, longitude);
                }

                String responseBody = response.body().string();
                JsonNode jsonNode = JsonUtil.fromJson(responseBody, JsonNode.class);
                return parseGeoapifyResponse(jsonNode, null, null);
            }
        } catch (Exception e) {
            log.error("주변 음식점 검색 실패: {}", e.getMessage());
            return getDefaultFoodsNearby(latitude, longitude);
        }
    }

    /**
     * 단순 백오프 재시도 (고정 800ms) - 네트워크 일시 오류/타임아웃 완화
     */
    private Response executeWithRetry(Request request, int maxRetries) throws IOException, InterruptedException {
        IOException last = null;
        for (int attempt = 0; attempt <= maxRetries; attempt++) {
            try {
                return httpClient.newCall(request).execute();
            } catch (IOException ex) {
                last = ex;
                if (attempt == maxRetries) break;
                log.warn("Geoapify 호출 재시도 {}/{}: {}", attempt + 1, maxRetries, ex.getMessage());
                Thread.sleep(800L);
            }
        }
        throw last != null ? last : new IOException("Geoapify 호출 실패");
    }

    private List<Food> parseGeoapifyResponse(JsonNode jsonNode, String city, String cuisine) {
        List<Food> foods = new ArrayList<>();
        if (jsonNode.has("features") && jsonNode.get("features").isArray()) {
            JsonNode features = jsonNode.get("features");
            log.debug("Geoapify API features 개수: {}", features.size());
            
            for (JsonNode feature : features) {
                if (!feature.has("properties")) {
                    log.warn("feature에 properties가 없음, 건너뜀");
                    continue;
                }
                
                JsonNode properties = feature.get("properties");
                JsonNode geometry = feature.has("geometry") ? feature.get("geometry") : null;
                
                String name = properties.has("name") && !properties.get("name").isNull() 
                    ? properties.get("name").asText() 
                    : "음식점";
                
                // 이름이 비어있거나 "음식점"인 경우 건너뛰기
                if (name.isEmpty() || name.equals("음식점")) {
                    log.debug("이름이 비어있거나 기본값인 항목 건너뜀: {}", name);
                    continue;
                }
                
                foods.add(Food.builder()
                        .id(properties.has("place_id") ? properties.get("place_id").asText() : "unknown")
                        .name(name)
                        .address(properties.has("formatted") && !properties.get("formatted").isNull() 
                            ? properties.get("formatted").asText() 
                            : (properties.has("address_line2") && !properties.get("address_line2").isNull()
                                ? properties.get("address_line2").asText()
                                : ""))
                        .city(city != null ? city : "")
                        .country(properties.has("country_code") ? properties.get("country_code").asText() : "")
                        .cuisine(cuisine != null ? cuisine : "일반")
                        .priceRange(new BigDecimal("50000"))
                        .rating(properties.has("rating") && !properties.get("rating").isNull() 
                            ? properties.get("rating").asDouble() 
                            : 4.0)
                        .description("Geoapify를 통해 검색된 음식점")
                        .latitude(geometry != null && geometry.has("coordinates") && geometry.get("coordinates").isArray() ? 
                                 geometry.get("coordinates").get(1).asDouble() : null)
                        .longitude(geometry != null && geometry.has("coordinates") && geometry.get("coordinates").isArray() ? 
                                  geometry.get("coordinates").get(0).asDouble() : null)
                        .build());
            }
        } else {
            log.warn("Geoapify API 응답에 features가 없거나 배열이 아님");
        }
        return foods;
    }

    private List<Food> getDefaultFoods(String city, String cuisine) {
        List<Food> foods = new ArrayList<>();
        String[] foodNames = {
            "해물탕 전문점", "흑돼지 맛집", "갈치조림 전문점", "전복죽 맛집", "한정식",
            "돼지고기 구이", "해산물 뷔페", "제주 향토 음식", "카페", "디저트 카페"
        };
        
        String cuisineType = cuisine != null ? cuisine : "한식";
        
        for (int i = 0; i < 10; i++) {
            String foodName = city.equals("제주") || city.equals("제주도")
                ? foodNames[i % foodNames.length]
                : city + " 맛집 " + (i + 1);
            
        foods.add(Food.builder()
                    .id("food-" + (i + 1))
                    .name(foodName)
                .address(city + " 시내")
                .city(city)
                    .cuisine(cuisineType)
                .priceRange(new BigDecimal("30000"))
                    .rating(4.0 + (i % 5) * 0.1)
                .description("맛있는 음식점입니다.")
                .build());
        }
        return foods;
    }

    private List<Food> getDefaultFoodsNearby(Double latitude, Double longitude) {
        List<Food> foods = new ArrayList<>();
        String[] foodNames = {
            "주변 맛집 1", "주변 맛집 2", "주변 맛집 3", "주변 맛집 4", "주변 맛집 5",
            "주변 맛집 6", "주변 맛집 7", "주변 맛집 8", "주변 맛집 9", "주변 맛집 10"
        };
        
        for (int i = 0; i < 10; i++) {
        foods.add(Food.builder()
                    .id("food-nearby-" + (i + 1))
                    .name(foodNames[i])
                .address("근처")
                .cuisine("한식")
                .priceRange(new BigDecimal("30000"))
                    .rating(4.0 + (i % 5) * 0.1)
                .description("주변 맛집입니다.")
                .latitude(latitude)
                .longitude(longitude)
                .build());
        }
        return foods;
    }
}

