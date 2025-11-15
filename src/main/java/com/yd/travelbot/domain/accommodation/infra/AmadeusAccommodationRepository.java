package com.yd.travelbot.domain.accommodation.infra;

import com.fasterxml.jackson.databind.JsonNode;
import com.yd.travelbot.domain.accommodation.domain.entity.Accommodation;
import com.yd.travelbot.domain.accommodation.domain.repository.AccommodationRepository;
import com.yd.travelbot.global.config.AmadeusConfig;
import com.yd.travelbot.global.config.GeoapifyConfig;
import com.yd.travelbot.global.resolver.GeoapifyCityResolver;
import com.yd.travelbot.global.util.JsonUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Repository
@RequiredArgsConstructor
public class AmadeusAccommodationRepository implements AccommodationRepository {

    private final AmadeusConfig amadeusConfig;
    private final GeoapifyConfig geoapifyConfig;
    private final GeoapifyCityResolver cityResolver;
    private final OkHttpClient httpClient = new OkHttpClient();
    private static final String AMADEUS_API_BASE_V1 = "https://test.api.amadeus.com/v1";
    private static final String AMADEUS_API_BASE_V3 = "https://test.api.amadeus.com/v3";
    private static final String GEOAPIFY_API_BASE = "https://api.geoapify.com/v2/places";

    @Override
    public List<Accommodation> search(String city, LocalDate checkIn, LocalDate checkOut, Integer guests) {
        // 1차: Amadeus Hotel Search API 시도
        try {
            List<Accommodation> accommodations = searchFromAmadeus(city, checkIn, checkOut, guests);
            if (!accommodations.isEmpty()) {
                // 5개 미만이면 Geoapify로 보충
                if (accommodations.size() < 5) {
                    log.warn("Amadeus 결과가 적음 ({}개), Geoapify로 보충 시도", accommodations.size());
                    try {
                        List<Accommodation> more = searchFromGeoapify(city, checkIn, checkOut, guests);
                        // 중복 제거 후 합치기
                        java.util.Set<String> seen = new java.util.HashSet<>();
                        List<Accommodation> merged = new java.util.ArrayList<>();
                        for (Accommodation a : accommodations) {
                            if (seen.add(a.getId())) merged.add(a);
                        }
                        for (Accommodation a : more) {
                            if (seen.add(a.getId())) merged.add(a);
                        }
                        // 최소 5개 보장
                        if (merged.size() < 5) {
                            merged.addAll(getDefaultAccommodations(city, checkIn, checkOut, guests)
                                    .subList(0, Math.min(5 - merged.size(), 5)));
                        }
                        return merged;
                    } catch (Exception ge) {
                        log.warn("Geoapify 보충 중 오류: {}", ge.getMessage());
                        // 기본 데이터로 최소 5개 보장
                        if (accommodations.size() < 5) {
                            accommodations.addAll(getDefaultAccommodations(city, checkIn, checkOut, guests)
                                    .subList(0, Math.min(5 - accommodations.size(), 5)));
                        }
                        return accommodations;
                    }
                }
                return accommodations;
            }
        } catch (Exception e) {
            log.warn("Amadeus API 호출 실패: {}, Geoapify로 대체 시도", e.getMessage());
        }

        // 2차: Geoapify로 fallback
        try {
            return searchFromGeoapify(city, checkIn, checkOut, guests);
        } catch (Exception e) {
            log.error("Geoapify API 호출 실패: {}", e.getMessage());
            return getDefaultAccommodations(city, checkIn, checkOut, guests);
        }
    }

    private List<Accommodation> searchFromAmadeus(String city, LocalDate checkIn, LocalDate checkOut, Integer guests) throws IOException {
        String accessToken = getAccessToken();
        
        // 먼저 도시 코드를 찾기
        String cityCode = getCityCode(city, accessToken);
        if (cityCode == null) {
            throw new IOException("도시 코드를 찾을 수 없습니다: " + city);
        }

        // 호텔 ID 목록 가져오기
        String hotelIdsUrl = String.format("%s/reference-data/locations/hotels/by-city?cityCode=%s",
                AMADEUS_API_BASE_V1, cityCode);
        
        Request hotelIdsRequest = new Request.Builder()
                .url(hotelIdsUrl)
                .addHeader("Authorization", "Bearer " + accessToken)
                .get()
                .build();

        List<String> hotelIds = new ArrayList<>();
        try (Response response = httpClient.newCall(hotelIdsRequest).execute()) {
            if (response.isSuccessful()) {
                String responseBody = response.body().string();
                JsonNode jsonNode = JsonUtil.fromJson(responseBody, JsonNode.class);
                
                // Amadeus API 에러 응답 체크
                if (jsonNode.has("errors")) {
                    JsonNode errors = jsonNode.get("errors");
                    if (errors.isArray() && errors.size() > 0) {
                        JsonNode firstError = errors.get(0);
                        String errorCode = firstError.has("code") ? firstError.get("code").asText() : "UNKNOWN";
                        String errorDetail = firstError.has("detail") ? firstError.get("detail").asText() : "Unknown error";
                        log.warn("Amadeus Hotel List API 에러 (code: {}): {}", errorCode, errorDetail);
                        throw new IOException("호텔 목록 조회 실패: " + errorDetail);
                    }
                }
                
                // Amadeus.md 문서에 따른 응답 구조: { "data": [...], "meta": {...} }
                if (jsonNode.has("data") && jsonNode.get("data").isArray()) {
                    for (JsonNode hotel : jsonNode.get("data")) {
                        if (hotel.has("hotelId")) {
                            hotelIds.add(hotel.get("hotelId").asText());
                        }
                    }
                }
            } else {
                String errorBody = response.body() != null ? response.body().string() : "";
                log.warn("Amadeus Hotel List API 호출 실패 (status: {}): {}", response.code(), errorBody);
            }
        }

        if (hotelIds.isEmpty()) {
            throw new IOException("호텔 ID를 찾을 수 없습니다");
        }

        // 호텔 검색 (최대 5개) - v3 API 사용, INVALID PROPERTY CODE 발생 시 해당 hotelId 제거 후 재시도
        String checkInStr = checkIn.format(DateTimeFormatter.ISO_DATE);
        String checkOutStr = checkOut.format(DateTimeFormatter.ISO_DATE);

        java.util.List<String> candidateIds = new java.util.ArrayList<>(hotelIds);
        int attempts = 0;
        while (!candidateIds.isEmpty() && attempts < 3) {
            attempts++;
            String hotelIdsParam = String.join(",", candidateIds.subList(0, Math.min(candidateIds.size(), 5)));
            String searchUrl = String.format("%s/shopping/hotel-offers?hotelIds=%s&checkInDate=%s&checkOutDate=%s&adults=%d",
                    AMADEUS_API_BASE_V3, hotelIdsParam, checkInStr, checkOutStr, guests);

            Request searchRequest = new Request.Builder()
                    .url(searchUrl)
                    .addHeader("Authorization", "Bearer " + accessToken)
                    .get()
                    .build();

            try (Response response = httpClient.newCall(searchRequest).execute()) {
                String body = response.body() != null ? response.body().string() : "";
                if (!response.isSuccessful()) {
                    log.error("Amadeus Hotel Search API 호출 실패 (status: {}): {}", response.code(), body);
                    // 400 INVALID PROPERTY CODE 처리: 오류 본문에서 잘못된 hotelIds 추출 후 제거하고 재시도
                    if (response.code() == 400 && body.contains("INVALID PROPERTY CODE")) {
                        try {
                            JsonNode err = JsonUtil.fromJson(body, JsonNode.class);
                            if (err.has("errors") && err.get("errors").isArray() && err.get("errors").size() > 0) {
                                JsonNode first = err.get("errors").get(0);
                                String param = first.has("source") && first.get("source").has("parameter")
                                        ? first.get("source").get("parameter").asText()
                                        : "";
                                if (param.startsWith("hotelIds=")) {
                                    String invalidList = param.substring("hotelIds=".length());
                                    for (String invalid : invalidList.split(",")) {
                                        candidateIds.removeIf(id -> id.equalsIgnoreCase(invalid.trim()));
                                    }
                                    // 다음 루프로 재시도
                                    continue;
                                }
                            }
                        } catch (Exception parseEx) {
                            log.warn("Amadeus 오류 파싱 실패: {}", parseEx.getMessage());
                        }
                    }
                    throw new IOException("호텔 검색 실패: " + response.code());
                }

                JsonNode jsonNode = JsonUtil.fromJson(body, JsonNode.class);
                if (jsonNode.has("errors") && jsonNode.get("errors").isArray() && jsonNode.get("errors").size() > 0) {
                    JsonNode firstError = jsonNode.get("errors").get(0);
                    String errorCode = firstError.has("code") ? firstError.get("code").asText() : "UNKNOWN";
                    String errorDetail = firstError.has("detail") ? firstError.get("detail").asText() : "Unknown error";
                    log.error("Amadeus API 에러 (code: {}): {}", errorCode, errorDetail);
                    // INVALID PROPERTY CODE인 경우 제거 후 재시도
                    if ("1257".equals(errorCode) || errorDetail.contains("INVALID PROPERTY CODE")) {
                        String param = firstError.has("source") && firstError.get("source").has("parameter")
                                ? firstError.get("source").get("parameter").asText()
                                : "";
                        if (param.startsWith("hotelIds=")) {
                            String invalidList = param.substring("hotelIds=".length());
                            for (String invalid : invalidList.split(",")) {
                                candidateIds.removeIf(id -> id.equalsIgnoreCase(invalid.trim()));
                            }
                            continue; // 재시도
                        }
                    }
                    throw new IOException("Amadeus API 에러: " + errorDetail);
                }

                // 성공적으로 결과 파싱
                return parseAmadeusHotelOffers(jsonNode, checkIn, checkOut, guests);
            }
        }

        throw new IOException("유효한 호텔 ID로 검색할 수 없습니다");
    }

    private String getCityCode(String city, String accessToken) throws IOException {
        // 한국 도시명을 IATA 코드로 매핑
        String cityCode = mapKoreanCityToIataCode(city);
        if (cityCode != null) {
            return cityCode;
        }
        
        // 도시 검색
        String searchUrl = String.format("%s/reference-data/locations/cities?keyword=%s&max=1",
                AMADEUS_API_BASE_V1, city);
        
        Request request = new Request.Builder()
                .url(searchUrl)
                .addHeader("Authorization", "Bearer " + accessToken)
                .get()
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                log.warn("Amadeus Cities API 호출 실패 (status: {}): {}", response.code(), 
                        response.body() != null ? response.body().string() : "");
                return null;
            }
            String responseBody = response.body().string();
            JsonNode jsonNode = JsonUtil.fromJson(responseBody, JsonNode.class);
            
            // Amadeus API 에러 응답 체크
            if (jsonNode.has("errors")) {
                JsonNode errors = jsonNode.get("errors");
                if (errors.isArray() && errors.size() > 0) {
                    JsonNode firstError = errors.get(0);
                    String errorCode = firstError.has("code") ? firstError.get("code").asText() : "UNKNOWN";
                    log.warn("Amadeus Cities API 에러 (code: {}): {}", errorCode, 
                            firstError.has("detail") ? firstError.get("detail").asText() : "Unknown error");
                }
                return null;
            }
            
            // Amadeus.md 문서에 따른 응답 구조: { "data": [...], "meta": {...} }
            if (jsonNode.has("data") && jsonNode.get("data").isArray() && jsonNode.get("data").size() > 0) {
                JsonNode firstCity = jsonNode.get("data").get(0);
                if (firstCity.has("iataCode")) {
                    return firstCity.get("iataCode").asText();
                }
            }
        } catch (Exception e) {
            log.warn("도시 코드 검색 중 오류 발생: {}", e.getMessage());
        }
        return null;
    }

    private List<Accommodation> parseAmadeusHotelOffers(JsonNode jsonNode, LocalDate checkIn, LocalDate checkOut, Integer guests) {
        List<Accommodation> accommodations = new ArrayList<>();
        if (jsonNode.has("data")) {
            for (JsonNode hotelOffer : jsonNode.get("data")) {
                // Amadeus.md 문서에 따른 응답 구조 파싱
                // { "type": "hotel-offers", "hotel": {...}, "available": true, "offers": [...] }
                JsonNode hotel = hotelOffer.has("hotel") ? hotelOffer.get("hotel") : null;
                JsonNode offers = hotelOffer.has("offers") ? hotelOffer.get("offers") : null;

                // available 체크 (Amadeus.md 문서 참조)
                boolean available = hotelOffer.has("available") && hotelOffer.get("available").asBoolean();

                if (hotel != null && offers != null && offers.isArray() && offers.size() > 0 && available) {
                    // 첫 번째 오퍼 사용 (가장 저렴한 옵션)
                    JsonNode firstOffer = offers.get(0);
                    JsonNode price = firstOffer.has("price") ? firstOffer.get("price") : null;

                    String hotelId = hotel.has("hotelId") ? hotel.get("hotelId").asText() : "unknown";
                    String hotelName = hotel.has("name") ? hotel.get("name").asText() : "호텔";
                    
                    BigDecimal totalPrice = BigDecimal.ZERO;
                    String currency = "KRW";
                    if (price != null) {
                        // Amadeus.md: price 객체는 { "currency": "USD", "base": "200.00", "total": "250.00" } 구조
                        if (price.has("total")) {
                            totalPrice = BigDecimal.valueOf(price.get("total").asDouble());
                        } else if (price.has("base")) {
                            // total이 없으면 base 사용
                            totalPrice = BigDecimal.valueOf(price.get("base").asDouble());
                        }
                        if (price.has("currency")) {
                            currency = price.get("currency").asText();
                        }
                    }

                    // 주소 파싱 (Amadeus.md 문서 구조에 맞게)
                    String address = "";
                    if (hotel.has("address")) {
                        JsonNode addressNode = hotel.get("address");
                        if (addressNode.has("lines") && addressNode.get("lines").isArray() && addressNode.get("lines").size() > 0) {
                            address = addressNode.get("lines").get(0).asText();
                        } else if (addressNode.has("cityName")) {
                            address = addressNode.get("cityName").asText();
                        }
                    }

                    accommodations.add(Accommodation.builder()
                            .id(hotelId)
                            .name(hotelName)
                            .address(address)
                            .city(hotel.has("cityCode") ? hotel.get("cityCode").asText() : "")
                            .country(hotel.has("address") && hotel.get("address").has("countryCode") ? 
                                    hotel.get("address").get("countryCode").asText() : "")
                            .price(totalPrice)
                            .currency(currency)
                            .rating(hotel.has("rating") ? hotel.get("rating").asDouble() : null)
                            .description("Amadeus를 통해 검색된 호텔")
                            .checkIn(checkIn)
                            .checkOut(checkOut)
                            .guests(guests)
                            .build());
                }
            }
        }
        return accommodations;
    }

    private List<Accommodation> searchFromGeoapify(String city, LocalDate checkIn, LocalDate checkOut, Integer guests) throws IOException {
        // 1단계: CityResolver로 도시 좌표 얻기
        Double[] cityCoordinates = cityResolver.resolveCoordinates(city, null);
        if (cityCoordinates == null) {
            log.warn("도시 좌표를 찾을 수 없음: {}", city);
            throw new IOException("도시 좌표를 찾을 수 없습니다: " + city);
        }
        
        double lon = cityCoordinates[0];
        double lat = cityCoordinates[1];
        log.info("도시 {}의 좌표: ({}, {})", city, lat, lon);
        
        // 2단계: Places API로 숙소 검색 (도시 중심 10km 반경)
        okhttp3.HttpUrl.Builder urlBuilder = okhttp3.HttpUrl.parse(GEOAPIFY_API_BASE).newBuilder()
                .addQueryParameter("categories", "accommodation.hotel")
                .addQueryParameter("filter", String.format("circle:%f,%f,10000", lon, lat))
                .addQueryParameter("limit", "10")
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
                throw new IOException("Geoapify API 호출 실패: " + response.code());
            }

            String responseBody = response.body().string();
            JsonNode jsonNode = JsonUtil.fromJson(responseBody, JsonNode.class);
            return parseGeoapifyResponse(jsonNode, city, checkIn, checkOut, guests);
        }
    }

    private List<Accommodation> parseGeoapifyResponse(JsonNode jsonNode, String city, LocalDate checkIn, LocalDate checkOut, Integer guests) {
        List<Accommodation> accommodations = new ArrayList<>();
        if (jsonNode.has("features")) {
            for (JsonNode feature : jsonNode.get("features")) {
                JsonNode properties = feature.get("properties");
                JsonNode geometry = feature.get("geometry");

                accommodations.add(Accommodation.builder()
                        .id(properties.has("place_id") ? properties.get("place_id").asText() : "unknown")
                        .name(properties.has("name") ? properties.get("name").asText() : city + " 호텔")
                        .address(properties.has("formatted") ? properties.get("formatted").asText() : "")
                        .city(city)
                        .country(properties.has("country_code") ? properties.get("country_code").asText().toUpperCase() : "")
                        .price(new BigDecimal("150000"))
                        .currency("KRW")
                        .rating(4.0)
                        .description("Geoapify를 통해 검색된 호텔")
                        .checkIn(checkIn)
                        .checkOut(checkOut)
                        .guests(guests)
                        .build());
            }
        }
        // Geoapify 결과가 5개 미만이면 기본 데이터로 보충
        if (accommodations.size() < 5) {
            log.warn("Geoapify 숙소 응답이 적음 ({}개), 기본 데이터로 보충", accommodations.size());
            List<Accommodation> defaults = getDefaultAccommodations(city, checkIn, checkOut, guests);
            // 중복 제거 후 합치기
            java.util.Set<String> seen = new java.util.HashSet<>();
            List<Accommodation> merged = new java.util.ArrayList<>();
            for (Accommodation a : accommodations) {
                if (seen.add(a.getId())) merged.add(a);
            }
            for (Accommodation a : defaults) {
                if (merged.size() >= 5) break;
                if (seen.add(a.getId())) merged.add(a);
            }
            return merged;
        }
        return accommodations;
    }

    private String mapKoreanCityToIataCode(String city) {
        // 한국 주요 도시명을 IATA 코드로 매핑
        return switch (city) {
            case "서울" -> "SEL";
            case "부산" -> "PUS";
            case "제주", "제주도" -> "CJU";
            case "인천" -> "ICN";
            case "대구" -> "TAE";
            case "대전" -> "YEC";
            case "광주" -> "KWJ";
            case "울산" -> "USN";
            case "수원" -> "SWU";
            case "성남" -> "SEL"; // 서울 근처
            case "고양" -> "SEL"; // 서울 근처
            case "용인" -> "SEL"; // 서울 근처
            case "청주" -> "CJJ";
            case "천안" -> "SEL"; // 서울 근처
            case "전주" -> "HIN";
            case "포항" -> "KPO";
            case "창원" -> "CHW";
            default -> null;
        };
    }

    private String getAccessToken() throws IOException {
        String credentials = Credentials.basic(amadeusConfig.getApiKey(), amadeusConfig.getApiSecret());
        RequestBody body = RequestBody.create("grant_type=client_credentials", MediaType.get("application/x-www-form-urlencoded"));
        
        Request request = new Request.Builder()
                .url(AMADEUS_API_BASE_V1 + "/security/oauth2/token")
                .addHeader("Authorization", credentials)
                .post(body)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "";
                log.error("Amadeus OAuth2 토큰 발급 실패 (status: {}): {}", response.code(), errorBody);
                throw new IOException("Amadeus 토큰 발급 실패: " + response.code());
            }
            String responseBody = response.body().string();
            JsonNode jsonNode = JsonUtil.fromJson(responseBody, JsonNode.class);
            
            // Amadeus.md 문서에 따른 OAuth2 응답 구조: { "type": "amadeusOAuth2Token", "username": "...", "application_name": "...", "client_id": "...", "token_type": "Bearer", "access_token": "...", "expires_in": 1799, "state": "approved", "scope": "" }
            if (jsonNode.has("access_token")) {
            return jsonNode.get("access_token").asText();
            } else {
                log.error("Amadeus OAuth2 응답에 access_token이 없습니다: {}", responseBody);
                throw new IOException("Amadeus 토큰 응답에 access_token이 없습니다");
            }
        }
    }

    private List<Accommodation> getDefaultAccommodations(String city, LocalDate checkIn, LocalDate checkOut, Integer guests) {
        List<Accommodation> accommodations = new ArrayList<>();
        String[] hotelNames = {
            "리조트", "호텔", "펜션", "게스트하우스", "콘도",
            "모텔", "민박", "한옥 스테이", "호스텔", "비즈니스 호텔"
        };
        
        for (int i = 0; i < 10; i++) {
        accommodations.add(Accommodation.builder()
                    .id("default-" + (i + 1))
                    .name(city + " " + hotelNames[i % hotelNames.length])
                .address(city + " 시내")
                .city(city)
                .country("KR")
                    .price(new BigDecimal("100000").add(new BigDecimal(i * 10000)))
                .currency("KRW")
                    .rating(4.0 + (i % 5) * 0.1)
                .description("기본 호텔 정보")
                .checkIn(checkIn)
                .checkOut(checkOut)
                .guests(guests)
                .build());
        }
        return accommodations;
    }

    @Override
    public Accommodation findById(String id) {
        // 구현 필요
        return null;
    }
}
