package com.yd.travelbot.global.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.yd.travelbot.global.config.AmadeusConfig;
import com.yd.travelbot.global.config.ExchangeRatesConfig;
import com.yd.travelbot.global.config.GeoapifyConfig;
import dev.langchain4j.model.chat.ChatLanguageModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ApiHealthChecker {

    private final AmadeusConfig amadeusConfig;
    private final GeoapifyConfig geoapifyConfig;
    private final ExchangeRatesConfig exchangeRatesConfig;
    private final ChatLanguageModel chatLanguageModel;
    private final OkHttpClient http = new OkHttpClient();

    @EventListener(ApplicationReadyEvent.class)
    public void checkApisOnStartup() {
        checkAmadeus();
        checkGeoapify();
        checkExchangeRates();
        checkGemini();
    }

    private void checkAmadeus() {
        try {
            String credentials = okhttp3.Credentials.basic(amadeusConfig.getApiKey(), amadeusConfig.getApiSecret());
            Request request = new Request.Builder()
                    .url("https://test.api.amadeus.com/v1/security/oauth2/token")
                    .addHeader("Authorization", credentials)
                    .post(okhttp3.RequestBody.create("grant_type=client_credentials",
                            okhttp3.MediaType.get("application/x-www-form-urlencoded")))
                    .build();
            try (Response resp = http.newCall(request).execute()) {
                if (resp.isSuccessful()) {
                    log.info("[HEALTH] Amadeus OAuth OK");
                } else {
                    log.warn("[HEALTH] Amadeus OAuth FAIL status={} body={}", resp.code(),
                            resp.body() != null ? resp.body().string() : "");
                }
            }
        } catch (Exception e) {
            log.warn("[HEALTH] Amadeus check error: {}", e.getMessage());
        }
    }

    private void checkGeoapify() {
        try {
            String url = okhttp3.HttpUrl.parse("https://api.geoapify.com/v1/geocode/search").newBuilder()
                    .addQueryParameter("text", "서울")
                    .addQueryParameter("type", "city")
                    .addQueryParameter("limit", "1")
                    .addQueryParameter("format", "json")
                    .addQueryParameter("lang", "ko")
                    .addQueryParameter("filter", "countrycode:kr")
                    .addQueryParameter("apiKey", geoapifyConfig.getApiKey())
                    .build().toString();
            Request request = new Request.Builder().url(url).get().build();
            try (Response resp = http.newCall(request).execute()) {
                if (resp.isSuccessful()) {
                    String body = resp.body() != null ? resp.body().string() : "";
                    JsonNode node = JsonUtil.fromJson(body, JsonNode.class);
                    boolean ok = node.has("results") && node.get("results").isArray() && node.get("results").size() > 0;
                    if (ok) {
                        log.info("[HEALTH] Geoapify Geocoding OK (서울)");
                    } else {
                        // 영문 도시명으로 재시도
                        String enUrl = okhttp3.HttpUrl.parse("https://api.geoapify.com/v1/geocode/search").newBuilder()
                                .addQueryParameter("text", "Seoul, South Korea")
                                .addQueryParameter("type", "city")
                                .addQueryParameter("limit", "1")
                                .addQueryParameter("format", "json")
                                .addQueryParameter("lang", "en")
                                .addQueryParameter("filter", "countrycode:kr")
                                .addQueryParameter("apiKey", geoapifyConfig.getApiKey())
                                .build().toString();
                        Request enReq = new Request.Builder().url(enUrl).get().build();
                        try (Response enResp = http.newCall(enReq).execute()) {
                            if (enResp.isSuccessful()) {
                                String enBody = enResp.body() != null ? enResp.body().string() : "";
                                JsonNode enNode = JsonUtil.fromJson(enBody, JsonNode.class);
                                boolean enOk = enNode.has("results") && enNode.get("results").isArray() && enNode.get("results").size() > 0;
                                if (enOk) {
                                    log.info("[HEALTH] Geoapify Geocoding OK (Seoul fallback)");
                                } else {
                                    log.warn("[HEALTH] Geoapify Geocoding EMPTY (서울/Seoul)");
                                }
                            } else {
                                log.warn("[HEALTH] Geoapify Geocoding FAIL (fallback) status={}", enResp.code());
                            }
                        }
                    }
                } else {
                    log.warn("[HEALTH] Geoapify Geocoding FAIL status={}", resp.code());
                }
            }
        } catch (Exception e) {
            log.warn("[HEALTH] Geoapify check error: {}", e.getMessage());
        }
    }

    private void checkExchangeRates() {
        try {
            String url = okhttp3.HttpUrl.parse("https://open.er-api.com/v6/latest/USD").newBuilder().build().toString();
            Request request = new Request.Builder().url(url).get().build();
            try (Response resp = http.newCall(request).execute()) {
                if (resp.isSuccessful()) {
                    log.info("[HEALTH] ExchangeRates OK (USD latest)");
                } else {
                    log.warn("[HEALTH] ExchangeRates FAIL status={}", resp.code());
                }
            }
        } catch (Exception e) {
            log.warn("[HEALTH] ExchangeRates check error: {}", e.getMessage());
        }
    }

    private void checkGemini() {
        try {
            // 매우 짧은 프롬프트로 가용성만 확인
            chatLanguageModel.generate("ping");
            log.info("[HEALTH] Gemini Chat Model OK");
        } catch (Exception e) {
            log.warn("[HEALTH] Gemini Chat Model UNAVAILABLE: {}", e.getMessage());
        }
    }
}

