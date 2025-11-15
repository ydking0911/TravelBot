package com.yd.travelbot.domain.chatbot.domain.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.yd.travelbot.domain.food.application.dto.FoodResponse;
import com.yd.travelbot.domain.food.domain.entity.Food;
import com.yd.travelbot.global.config.GeoapifyConfig;
import com.yd.travelbot.global.resolver.GeoapifyCityResolver;
import com.yd.travelbot.global.util.JsonUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class GeoapifyPlacesAgent {

    private static final String GEOAPIFY_PLACES_API_BASE = "https://api.geoapify.com/v2/places";

    private final GeoapifyConfig geoapifyConfig;
    private final GeoapifyCityResolver cityResolver;
    private final OkHttpClient http = new OkHttpClient();

    public List<FoodResponse> searchFood(String city, String countryCode, String cuisine) {
        Double[] coord = cityResolver.resolveCoordinates(city, countryCode);
        if (coord == null) {
            log.warn("GeoapifyPlacesAgent: 도시 좌표를 찾을 수 없음: {}", city);
            return buildDefaultFoods(city, cuisine, 5);
        }
        double lon = coord[0];
        double lat = coord[1];

        okhttp3.HttpUrl.Builder url = okhttp3.HttpUrl.parse(GEOAPIFY_PLACES_API_BASE).newBuilder()
                .addQueryParameter("categories", cuisine != null && !cuisine.isBlank() ? "catering.restaurant" : "catering")
                .addQueryParameter("filter", String.format("circle:%f,%f,10000", lon, lat))
                .addQueryParameter("limit", "20")
                .addQueryParameter("apiKey", geoapifyConfig.getApiKey());

        String built = url.build().toString();
        log.info("Geoapify Places API 요청 URL (agent/food): {}", built);

        try {
            Request req = new Request.Builder().url(built).get().build();
            try (Response resp = http.newCall(req).execute()) {
                if (!resp.isSuccessful()) {
                    String body = resp.body() != null ? resp.body().string() : "";
                    log.error("Geoapify Places API 실패 status={} body={}", resp.code(), body);
                    return buildDefaultFoods(city, cuisine, 5);
                }
                String body = resp.body() != null ? resp.body().string() : "";
                JsonNode json = JsonUtil.fromJson(body, JsonNode.class);
                List<Food> foods = mapFoods(json, city, cuisine);
                if (foods.size() < 5) {
                    foods.addAll(mapDefaultFoods(city, cuisine, 5 - foods.size()));
                }
                return toResponses(foods);
            }
        } catch (Exception e) {
            log.error("Geoapify Places agent 호출 실패: {}", e.getMessage(), e);
            return buildDefaultFoods(city, cuisine, 5);
        }
    }

    private List<Food> mapFoods(JsonNode jsonNode, String city, String cuisine) {
        List<Food> foods = new ArrayList<>();
        if (jsonNode.has("features") && jsonNode.get("features").isArray()) {
            for (JsonNode feature : jsonNode.get("features")) {
                if (!feature.has("properties")) continue;
                JsonNode p = feature.get("properties");
                String name = p.has("name") && !p.get("name").isNull() ? p.get("name").asText() : "";
                if (name.isBlank()) continue;
                foods.add(Food.builder()
                        .id(p.has("place_id") ? p.get("place_id").asText() : "unknown")
                        .name(name)
                        .address(p.has("formatted") && !p.get("formatted").isNull() ? p.get("formatted").asText() : "")
                        .city(city != null ? city : "")
                        .cuisine(cuisine != null ? cuisine : "현지")
                        .priceRange(new BigDecimal("30000"))
                        .rating(p.has("rating") && !p.get("rating").isNull() ? p.get("rating").asDouble() : 4.0)
                        .description("Geoapify를 통해 검색된 음식점")
                        .build());
            }
        }
        return foods;
    }

    private List<Food> mapDefaultFoods(String city, String cuisine, int count) {
        List<Food> foods = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            foods.add(Food.builder()
                    .id("food-default-" + i)
                    .name((city != null ? city : "현지") + " 맛집 " + (i + 1))
                    .address((city != null ? city : "도시") + " 시내")
                    .city(city != null ? city : "")
                    .cuisine(cuisine != null ? cuisine : "현지")
                    .priceRange(new BigDecimal("30000"))
                    .rating(4.2)
                    .description("기본 음식점 정보")
                    .build());
        }
        return foods;
    }

    private List<FoodResponse> toResponses(List<Food> foods) {
        List<FoodResponse> res = new ArrayList<>();
        for (Food f : foods) {
            res.add(FoodResponse.from(f));
        }
        return res;
    }

    private List<FoodResponse> buildDefaultFoods(String city, String cuisine, int minCount) {
        return toResponses(mapDefaultFoods(city, cuisine, Math.max(minCount, 5)));
    }
}

