package com.yd.travelbot.domain.place.infra;

import com.fasterxml.jackson.databind.JsonNode;
import com.yd.travelbot.domain.place.domain.entity.Place;
import com.yd.travelbot.domain.place.domain.repository.PlaceRepository;
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
public class GeoapifyPlaceRepository implements PlaceRepository {

    private final GeoapifyConfig geoapifyConfig;
    private final GeoapifyCityResolver cityResolver;
    private final OkHttpClient httpClient = new OkHttpClient();
    private static final String GEOAPIFY_PLACES_API_BASE = "https://api.geoapify.com/v2/places";
    private static final String GEOAPIFY_GEOCODE_API_BASE = "https://api.geoapify.com/v1/geocode/search";
    // 한국 도시명 기본 매핑 (한글 → 영어)
    private static final java.util.Map<String, String> CITY_EN_MAP = java.util.Map.ofEntries(
            java.util.Map.entry("서울", "Seoul"),
            java.util.Map.entry("서울특별시", "Seoul"),
            java.util.Map.entry("부산", "Busan"),
            java.util.Map.entry("부산광역시", "Busan"),
            java.util.Map.entry("대구", "Daegu"),
            java.util.Map.entry("대구광역시", "Daegu"),
            java.util.Map.entry("인천", "Incheon"),
            java.util.Map.entry("인천광역시", "Incheon"),
            java.util.Map.entry("광주", "Gwangju"),
            java.util.Map.entry("광주광역시", "Gwangju"),
            java.util.Map.entry("대전", "Daejeon"),
            java.util.Map.entry("대전광역시", "Daejeon"),
            java.util.Map.entry("울산", "Ulsan"),
            java.util.Map.entry("울산광역시", "Ulsan"),
            java.util.Map.entry("세종", "Sejong"),
            java.util.Map.entry("세종특별자치시", "Sejong"),
            java.util.Map.entry("제주", "Jeju"),
            java.util.Map.entry("제주도", "Jeju"),
            java.util.Map.entry("제주특별자치도", "Jeju")
    );

    // 최종 안전 좌표 (경도, 위도)
    private static final java.util.Map<String, Double[]> KNOWN_CITY_COORDS = java.util.Map.of(
            "서울", new Double[]{126.978291, 37.5666791},
            "부산", new Double[]{129.0752365, 35.1799528},
            "제주", new Double[]{126.5311884, 33.4996213},
            "제주도", new Double[]{126.5311884, 33.4996213}
    );

    @Override
    public List<Place> search(String city, String category) {
        try {
            // 1단계: CityResolver로 도시 좌표 얻기
            Double[] cityCoordinates = cityResolver.resolveCoordinates(city, "kr");
            if (cityCoordinates == null) {
                log.warn("도시 좌표를 찾을 수 없음: {}, 기본 데이터 반환", city);
                return getDefaultPlaces(city, category);
            }
            
            double lon = cityCoordinates[0];
            double lat = cityCoordinates[1];
            log.info("도시 {}의 좌표: ({}, {})", city, lat, lon);
            
            // 2단계: Places API로 관광지 검색 (도시 중심 10km 반경)
            String categories = getCategoryFilter(category);
            okhttp3.HttpUrl.Builder urlBuilder = okhttp3.HttpUrl.parse(GEOAPIFY_PLACES_API_BASE).newBuilder()
                    .addQueryParameter("categories", categories)
                    .addQueryParameter("filter", String.format("circle:%f,%f,10000", lon, lat))
                    .addQueryParameter("limit", "20")
                    .addQueryParameter("apiKey", geoapifyConfig.getApiKey());
            
            String url = urlBuilder.build().toString();
            log.info("Geoapify Places API 요청 URL: {}", url);

            Request request = new Request.Builder()
                    .url(url)
                    .get()
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    String errorBody = response.body() != null ? response.body().string() : "";
                    log.error("Geoapify Places API 호출 실패 (status: {}): {}", response.code(), errorBody);
                    log.warn("기본 데이터 반환");
                    return getDefaultPlaces(city, category);
                }

                String responseBody = response.body().string();
                log.info("Geoapify Places API 응답 (관광지): {}", responseBody.substring(0, Math.min(500, responseBody.length())));
                JsonNode jsonNode = JsonUtil.fromJson(responseBody, JsonNode.class);
                List<Place> places = parseGeoapifyResponse(jsonNode, city, category);
                
                log.info("Geoapify API에서 파싱된 관광지 개수: {}", places.size());
                
                // API 응답이 비어있거나 결과가 적을 때 (5개 미만) 기본 데이터로 보충
                if (places.isEmpty()) {
                    log.warn("Geoapify API 응답이 비어있음, 기본 데이터 반환");
                    return getDefaultPlaces(city, category);
                } else if (places.size() < 5) {
                    log.warn("Geoapify API 응답이 적음 ({}개), 기본 데이터로 보충", places.size());
                    // 기본 데이터를 추가하여 최소 10개 유지
                    List<Place> defaultPlaces = getDefaultPlaces(city, category);
                    places.addAll(defaultPlaces.subList(0, Math.min(10 - places.size(), defaultPlaces.size())));
                }
                
                return places;
            }
        } catch (Exception e) {
            log.error("관광지 검색 실패: {}", e.getMessage(), e);
            return getDefaultPlaces(city, category);
        }
    }

    @Override
    public List<Place> searchNearby(Double latitude, Double longitude, Double radius) {
        try {
            // URL 인코딩을 위해 HttpUrl.Builder 사용
            okhttp3.HttpUrl.Builder urlBuilder = okhttp3.HttpUrl.parse(GEOAPIFY_PLACES_API_BASE).newBuilder()
                    .addQueryParameter("categories", "tourism")
                    .addQueryParameter("filter", String.format("circle:%f,%f,%f", longitude, latitude, radius * 1000))
                    .addQueryParameter("limit", "20")
                    .addQueryParameter("apiKey", geoapifyConfig.getApiKey());
            
            String url = urlBuilder.build().toString();
            log.info("Geoapify Places API 요청 URL (주변 검색): {}", url);

            Request request = new Request.Builder()
                    .url(url)
                    .get()
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    String errorBody = response.body() != null ? response.body().string() : "";
                    log.error("Geoapify API 호출 실패 (status: {}): {}", response.code(), errorBody);
                    log.warn("기본 데이터 반환");
                    return getDefaultPlacesNearby(latitude, longitude);
                }

                String responseBody = response.body().string();
                JsonNode jsonNode = JsonUtil.fromJson(responseBody, JsonNode.class);
                return parseGeoapifyResponse(jsonNode, null, null);
            }
        } catch (Exception e) {
            log.error("주변 관광지 검색 실패: {}", e.getMessage());
            return getDefaultPlacesNearby(latitude, longitude);
        }
    }

    private String getCategoryFilter(String category) {
        if (category == null || category.isEmpty()) {
            return "tourism";
        }
        String lowerCategory = category.toLowerCase();
        if (lowerCategory.contains("museum")) {
            return "tourism.museum";
        } else if (lowerCategory.contains("park")) {
            return "entertainment.park";
        } else if (lowerCategory.contains("beach")) {
            return "beach";
        } else {
            return "tourism";
        }
    }

    private List<Place> parseGeoapifyResponse(JsonNode jsonNode, String city, String category) {
        List<Place> places = new ArrayList<>();
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
                    : "관광지";
                
                // 이름이 비어있거나 "관광지"인 경우 건너뛰기
                if (name.isEmpty() || name.equals("관광지")) {
                    log.debug("이름이 비어있거나 기본값인 항목 건너뜀: {}", name);
                    continue;
                }
                
                places.add(Place.builder()
                        .id(properties.has("place_id") ? properties.get("place_id").asText() : "unknown")
                        .name(name)
                        .address(properties.has("formatted") && !properties.get("formatted").isNull() 
                            ? properties.get("formatted").asText() 
                            : (properties.has("address_line2") && !properties.get("address_line2").isNull()
                                ? properties.get("address_line2").asText()
                                : ""))
                        .city(city != null ? city : "")
                        .country(properties.has("country_code") ? properties.get("country_code").asText() : "")
                        .category(category != null ? category : "관광지")
                        .description("Geoapify를 통해 검색된 관광지")
                        .rating(properties.has("rating") && !properties.get("rating").isNull() 
                            ? properties.get("rating").asDouble() 
                            : 4.0)
                        .entranceFee(new BigDecimal("10000"))
                        .currency("KRW")
                        .latitude(geometry != null && geometry.has("coordinates") && geometry.get("coordinates").isArray() ? 
                                 geometry.get("coordinates").get(1).asDouble() : null)
                        .longitude(geometry != null && geometry.has("coordinates") && geometry.get("coordinates").isArray() ? 
                                  geometry.get("coordinates").get(0).asDouble() : null)
                        .build());
            }
        } else {
            log.warn("Geoapify API 응답에 features가 없거나 배열이 아님");
        }
        return places;
    }

    private List<Place> getDefaultPlaces(String city, String category) {
        List<Place> places = new ArrayList<>();
        String[] placeNames = {
            "성산일출봉", "한라산", "천지연폭포", "성산일출봉", "협재해수욕장",
            "우도", "섭지코지", "카멜리아힐", "에코랜드", "제주올레"
        };
        
        for (int i = 0; i < 10; i++) {
            String placeName = city.equals("제주") || city.equals("제주도") 
                ? placeNames[i % placeNames.length] 
                : city + " 관광지 " + (i + 1);
            
        places.add(Place.builder()
                    .id("place-" + (i + 1))
                    .name(placeName)
                .address(city + " 시내")
                .city(city)
                .category(category != null ? category : "관광지")
                .description("인기 관광지입니다.")
                    .rating(4.0 + (i % 5) * 0.1)
                .entranceFee(new BigDecimal("10000"))
                .currency("KRW")
                .build());
        }
        return places;
    }

    private List<Place> getDefaultPlacesNearby(Double latitude, Double longitude) {
        List<Place> places = new ArrayList<>();
        String[] placeNames = {
            "주변 관광지 1", "주변 관광지 2", "주변 관광지 3", "주변 관광지 4", "주변 관광지 5",
            "주변 관광지 6", "주변 관광지 7", "주변 관광지 8", "주변 관광지 9", "주변 관광지 10"
        };
        
        for (int i = 0; i < 10; i++) {
        places.add(Place.builder()
                    .id("place-nearby-" + (i + 1))
                    .name(placeNames[i])
                .address("근처")
                .category("관광지")
                .description("주변 관광지입니다.")
                    .rating(4.0 + (i % 5) * 0.1)
                .entranceFee(new BigDecimal("10000"))
                .currency("KRW")
                .latitude(latitude)
                .longitude(longitude)
                .build());
        }
        return places;
    }
}

