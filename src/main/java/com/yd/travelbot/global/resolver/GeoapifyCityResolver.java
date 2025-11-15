package com.yd.travelbot.global.resolver;

import com.fasterxml.jackson.databind.JsonNode;
import com.yd.travelbot.global.config.GeoapifyConfig;
import com.yd.travelbot.global.util.JsonUtil;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class GeoapifyCityResolver {

    private static final String GEOAPIFY_GEOCODE_API_BASE = "https://api.geoapify.com/v1/geocode/search";

    private final GeoapifyConfig geoapifyConfig;
    private final ChatLanguageModel chatModel;
    private final OkHttpClient http = new OkHttpClient();
    
    // 안전 좌표 맵 (필요시 application.yml에서 주입 가능)
    private final Map<String, String> safeCoordsMap = Collections.emptyMap();

    /**
     * Resolve city name to coordinates [lon, lat].
     * @param city raw city name (any language)
     * @param countryCode optional ISO-3166 alpha-2 country code hint (e.g., "kr"), nullable
     * @return Double[]{lon, lat} or null if not found
     */
    public Double[] resolveCoordinates(String city, String countryCode) {
        if (city == null || city.isBlank()) return null;
        String input = city.trim();
        String preprocessed = preprocessCityInput(input);

        // 1) AI 번역/정규화: 약어/별칭/다국어 입력을 표준 영문 도시명으로
        String normalized = translateCityName(preprocessed);
        if (normalized == null || normalized.isBlank()) {
            normalized = preprocessed;
        }

        // 2) 1차 시도: 영문 표준명으로 Geoapify 호출
        Double[] coord = callGeoapify(normalized, "en", countryCode);
        if (coord != null) return coord;

        // 3) 2차 시도: "<city>, <country>" 형태 (countryCode가 있을 때)
        if (countryCode != null && !countryCode.isBlank()) {
            String countryName = countryCodeToName(countryCode);
            coord = callGeoapify(normalized + ", " + countryName, "en", countryCode);
            if (coord != null) return coord;
        }

        // 4) 안전 좌표 (최후의 수단)
        Double[] safe = safeCoords(normalized, safeCoordsMap);
        if (safe != null) {
            log.warn("도시 좌표 안전값 사용: {} -> {}", normalized, java.util.Arrays.toString(safe));
            return safe;
        }
        safe = safeCoords(input, safeCoordsMap);
        if (safe != null) {
            log.warn("도시 좌표 안전값 사용: {} -> {}", input, java.util.Arrays.toString(safe));
            return safe;
        }

        return null;
    }

    /**
     * AI를 사용하여 도시 입력(약어/별칭/다국어 포함)을 표준 영문 도시명으로 정규화합니다.
     */
    private String translateCityName(String cityName) {
        try {
            String system = """
                You are a city name normalizer.
                Task: Convert any given city input (any language, abbreviations, nicknames, airport-code-like aliases)
                into its standard English city name. Do not include country or state unless it's part of the canonical city name.
                - Remove dots/commas and whitespace noise (e.g., "S.F." -> "SF").
                - Be case-insensitive.
                - Map abbreviations or aliases to the full city name.
                - If the input is already the standard English city name, return it unchanged.
                - Respond ONLY with the city name text (no quotes, no extra words).

                Examples:
                - "서울" -> Seoul
                - "로마" -> Rome
                - "파리" -> Paris
                - "베를린" -> Berlin
                - "뉴욕" -> New York
                - "LA" -> Los Angeles
                - "la" -> Los Angeles
                - "L.A." -> Los Angeles
                - "NYC" -> New York City
                - "N.Y.C." -> New York City
                - "SF" -> San Francisco
                - "S.F." -> San Francisco
                - "NOLA" -> New Orleans
                - "SFO" -> San Francisco
                - "CDMX" -> Mexico City
                """;
            String user = "Normalize this city input to standard English city name: " + cityName;
            
            dev.langchain4j.model.output.Response<dev.langchain4j.data.message.AiMessage> response =
                chatModel.generate(SystemMessage.from(system), UserMessage.from(user));
            
            String translated = response.content().text().trim();
            // 따옴표 제거 + 여분 구두점 제거
            translated = translated.replaceAll("^[\"']|[\"']$", "").replaceAll("[,]+$", "").trim();
            
            if (translated.isBlank() || translated.equals(cityName)) {
                return null;
            }
            return translated;
        } catch (Exception e) {
            log.warn("도시명 AI 번역 실패: {} - {}", cityName, e.getMessage());
            return null;
        }
    }

    // 입력 전처리: 점/쉼표 제거, 다중 공백 정리, 트림
    private String preprocessCityInput(String raw) {
        if (raw == null) return null;
        String s = raw.trim();
        // 점/쉼표 제거: "S.F." -> "SF", "N.Y.C." -> "NYC"
        s = s.replace(".", "").replace(",", "");
        // 다중 공백 -> 단일 공백
        s = s.replaceAll("\\s+", " ").trim();
        return s;
    }

    private Double[] callGeoapify(String text, String lang, String countryCode) {
        try {
            okhttp3.HttpUrl.Builder url = okhttp3.HttpUrl.parse(GEOAPIFY_GEOCODE_API_BASE).newBuilder()
                    .addQueryParameter("text", text)
                    .addQueryParameter("type", "city")
                    .addQueryParameter("limit", "1")
                    .addQueryParameter("format", "json")
                    .addQueryParameter("lang", lang)
                    .addQueryParameter("apiKey", geoapifyConfig.getApiKey());
            if (countryCode != null && !countryCode.isBlank()) {
                url.addQueryParameter("filter", "countrycode:" + countryCode.toLowerCase());
            }
            String built = url.build().toString();
            log.debug("Geoapify CityResolver 요청: {}", built);

            Request req = new Request.Builder().url(built).get().build();
            try (okhttp3.Response resp = http.newCall(req).execute()) {
                if (!resp.isSuccessful()) {
                    log.warn("Geoapify CityResolver 실패 status={}", resp.code());
                    return null;
                }
                String body = resp.body() != null ? resp.body().string() : "";
                JsonNode node = JsonUtil.fromJson(body, JsonNode.class);
                if (node.has("results") && node.get("results").isArray() && node.get("results").size() > 0) {
                    JsonNode first = node.get("results").get(0);
                    if (first.has("lat") && first.has("lon")) {
                        double lat = first.get("lat").asDouble();
                        double lon = first.get("lon").asDouble();
                        return new Double[]{lon, lat};
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Geoapify CityResolver 예외: {}", e.getMessage());
        }
        return null;
    }

    private Double[] safeCoords(String city, Map<String, String> map) {
        if (map == null || map.isEmpty()) return null;
        String val = map.get(city);
        if (val == null) {
            for (Map.Entry<String, String> e : map.entrySet()) {
                if (e.getKey().equalsIgnoreCase(city)) {
                    val = e.getValue();
                    break;
                }
            }
        }
        if (val == null) return null;
        String[] parts = val.split(",");
        if (parts.length != 2) return null;
        try {
            double lon = Double.parseDouble(parts[0].trim());
            double lat = Double.parseDouble(parts[1].trim());
            return new Double[]{lon, lat};
        } catch (NumberFormatException ignore) {
            return null;
        }
    }

    private String countryCodeToName(String code) {
        // 최소 구현: KR -> South Korea, US -> United States, JP -> Japan, CN -> China
        String c = code.toUpperCase();
        return switch (c) {
            case "KR" -> "South Korea";
            case "US" -> "United States";
            case "JP" -> "Japan";
            case "CN" -> "China";
            default -> c;
        };
    }
}

