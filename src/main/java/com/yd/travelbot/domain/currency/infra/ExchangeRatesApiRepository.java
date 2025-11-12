package com.yd.travelbot.domain.currency.infra;

import com.fasterxml.jackson.databind.JsonNode;
import com.yd.travelbot.domain.currency.domain.entity.Currency;
import com.yd.travelbot.domain.currency.domain.repository.CurrencyRepository;
import com.yd.travelbot.global.config.ExchangeRatesConfig;
import com.yd.travelbot.global.util.JsonUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Slf4j
@Repository
@RequiredArgsConstructor
public class ExchangeRatesApiRepository implements CurrencyRepository {

    private final ExchangeRatesConfig exchangeRatesConfig;
    private final OkHttpClient httpClient = new OkHttpClient();
    private static final String KOREA_EXIM_API_BASE = "https://oapi.koreaexim.go.kr/site/program/financial/exchangeJSON";

    @Override
    public Currency getExchangeRate(String fromCurrency, String toCurrency) {
        // 같은 통화면 1:1 반환
        if (fromCurrency.equals(toCurrency)) {
            return Currency.builder()
                    .fromCurrency(fromCurrency)
                    .toCurrency(toCurrency)
                    .rate(BigDecimal.ONE)
                    .lastUpdated(LocalDateTime.now())
                    .build();
        }

        try {
            // 한국수출입은행 API는 KRW 기준으로 환율 제공
            // 주말/공휴일에는 데이터가 없을 수 있으므로, 최신 데이터부터 최대 5일 전까지 재시도
            java.time.LocalDate today = java.time.LocalDate.now();
            java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd");
            
            for (int daysBack = 0; daysBack <= 5; daysBack++) {
                java.time.LocalDate targetDate = today.minusDays(daysBack);
                String searchdate = targetDate.format(formatter);
                
                HttpUrl.Builder urlBuilder = HttpUrl.parse(KOREA_EXIM_API_BASE)
                    .newBuilder()
                        .addQueryParameter("authkey", exchangeRatesConfig.getApiKey())
                        .addQueryParameter("searchdate", searchdate)
                        .addQueryParameter("data", "AP01"); // AP01: 환율
            
            String url = urlBuilder.build().toString();
                if (daysBack == 0) {
                    log.info("한국수출입은행 환율 API 요청: {} -> {} (날짜: {})", fromCurrency, toCurrency, searchdate);
                } else {
                    log.debug("한국수출입은행 환율 API 재시도: {} -> {} (날짜: {}, {}일 전)", fromCurrency, toCurrency, searchdate, daysBack);
                }

            Request request = new Request.Builder()
                    .url(url)
                    .get()
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    String errorBody = response.body() != null ? response.body().string() : "";
                        if (daysBack == 0) {
                            log.error("한국수출입은행 API 호출 실패 (status: {}): {}", response.code(), errorBody);
                        }
                        if (daysBack < 5) continue; // 다음 날짜로 재시도
                        log.warn("모든 재시도 실패, 기본 환율로 대체합니다.");
                    return getDefaultRate(fromCurrency, toCurrency);
                }

                String responseBody = response.body().string();
                    if (daysBack == 0) {
                        log.debug("한국수출입은행 API 응답: {}", responseBody.length() > 200 ? responseBody.substring(0, 200) + "..." : responseBody);
                    }
                    
                    // 응답이 JSON 배열 형식
                    JsonNode jsonArray = JsonUtil.fromJson(responseBody, JsonNode.class);
                
                    if (!jsonArray.isArray()) {
                        log.error("한국수출입은행 API 응답이 배열 형식이 아닙니다.");
                        if (daysBack < 5) continue;
                    return getDefaultRate(fromCurrency, toCurrency);
                }
                
                    // 빈 배열이면 다음 날짜로 재시도
                    if (jsonArray.size() == 0) {
                        if (daysBack < 5) {
                            log.debug("{} 날짜에 환율 데이터가 없습니다. 이전 영업일로 재시도합니다.", searchdate);
                            continue;
                        } else {
                            log.warn("최근 5일간 환율 데이터를 찾을 수 없습니다. 기본 환율로 대체합니다.");
                            return getDefaultRate(fromCurrency, toCurrency);
                        }
                    }
                    
                    Currency currency = parseKoreaEximResponse(jsonArray, fromCurrency, toCurrency);
                if (currency != null) {
                        if (daysBack > 0) {
                            log.info("✅ 환율 조회 성공 ({}일 전 데이터 사용): 1 {} = {} {}", 
                                    daysBack, fromCurrency, currency.getRate(), toCurrency);
                        } else {
                            log.info("✅ 환율 조회 성공: 1 {} = {} {}", 
                                    fromCurrency, currency.getRate(), toCurrency);
                        }
                    return currency;
                    } else {
                        // 통화를 찾지 못했지만 데이터는 있음 (다른 통화는 있지만 요청한 통화는 없음)
                        if (daysBack < 5) {
                            log.debug("{} 날짜 데이터에서 {} 또는 {} 통화를 찾을 수 없습니다. 이전 영업일로 재시도합니다.", 
                                    searchdate, fromCurrency, toCurrency);
                            continue;
                } else {
                    log.warn("응답 파싱 실패, 기본 환율로 대체합니다.");
                    return getDefaultRate(fromCurrency, toCurrency);
                        }
                    }
                }
            }
            
            // 모든 재시도 실패
            log.warn("모든 재시도 실패, 기본 환율로 대체합니다.");
            return getDefaultRate(fromCurrency, toCurrency);
        } catch (Exception e) {
            log.error("환율 조회 실패: {}", e.getMessage(), e);
            log.warn("기본 환율로 대체합니다.");
            return getDefaultRate(fromCurrency, toCurrency);
        }
    }

    /**
     * 한국수출입은행 API 응답 파싱
     * API는 KRW 기준으로 환율을 제공 (1 외화 = deal_bas_r KRW)
     */
    private Currency parseKoreaEximResponse(JsonNode jsonArray, String fromCurrency, String toCurrency) {
        BigDecimal fromRateKrw = null; // 1 fromCurrency = fromRateKrw KRW
        BigDecimal toRateKrw = null;   // 1 toCurrency = toRateKrw KRW
        
        // 통화 코드 매핑 (한국수출입은행 API는 일부 통화 코드가 다를 수 있음)
        String fromCode = normalizeCurrencyCode(fromCurrency);
        String toCode = normalizeCurrencyCode(toCurrency);
            
        // 배열에서 각 통화 정보 찾기
        if (jsonArray.isArray()) {
            for (int i = 0; i < jsonArray.size(); i++) {
                JsonNode item = jsonArray.get(i);
                if (!item.has("cur_unit")) continue;
                
                String curUnit = item.get("cur_unit").asText();
                // 통화 코드 매칭 (예: JPY(100) -> JPY, USD -> USD)
                String normalizedCurUnit = curUnit.replaceAll("\\(.*\\)", "").trim();
                
                // 단위 추출 (예: JPY(100) -> 100, USD -> 1)
                int unit = 1;
                if (curUnit.contains("(") && curUnit.contains(")")) {
                    try {
                        String unitStr = curUnit.substring(curUnit.indexOf("(") + 1, curUnit.indexOf(")"));
                        unit = Integer.parseInt(unitStr);
                } catch (Exception e) {
                        log.warn("단위 파싱 실패: {}, 기본값 1 사용", curUnit);
                    }
                }
                
                if (normalizedCurUnit.equals(fromCode) && fromRateKrw == null) {
                    if (item.has("deal_bas_r")) {
                        String dealBasR = item.get("deal_bas_r").asText().replace(",", "");
                        BigDecimal rate = new BigDecimal(dealBasR);
                        // deal_bas_r은 unit개 통화에 대한 KRW 환율이므로, 1개 통화로 변환
                        fromRateKrw = rate.divide(BigDecimal.valueOf(unit), 10, java.math.RoundingMode.HALF_UP);
                        log.debug("{} 환율 찾음: 1 {} = {} KRW (원본: {} {} = {} KRW)", 
                                fromCurrency, fromCurrency, fromRateKrw, unit, fromCurrency, rate);
                    }
                }
                
                if (normalizedCurUnit.equals(toCode) && toRateKrw == null) {
                    if (item.has("deal_bas_r")) {
                        String dealBasR = item.get("deal_bas_r").asText().replace(",", "");
                        BigDecimal rate = new BigDecimal(dealBasR);
                        // deal_bas_r은 unit개 통화에 대한 KRW 환율이므로, 1개 통화로 변환
                        toRateKrw = rate.divide(BigDecimal.valueOf(unit), 10, java.math.RoundingMode.HALF_UP);
                        log.debug("{} 환율 찾음: 1 {} = {} KRW (원본: {} {} = {} KRW)", 
                                toCurrency, toCurrency, toRateKrw, unit, toCurrency, rate);
                    }
                }
                
                // 두 통화 모두 찾았으면 종료
                if (fromRateKrw != null && toRateKrw != null) {
                    break;
                }
            }
        }
        
        // KRW 처리
        if ("KRW".equals(fromCurrency)) {
            fromRateKrw = BigDecimal.ONE;
        }
        if ("KRW".equals(toCurrency)) {
            toRateKrw = BigDecimal.ONE;
        }
        
        // 환율 계산
        BigDecimal rate = null;
        if (fromRateKrw != null && toRateKrw != null) {
            if ("KRW".equals(fromCurrency)) {
                // KRW -> 다른 통화: 1 / toRateKrw
                rate = BigDecimal.ONE.divide(toRateKrw, 10, java.math.RoundingMode.HALF_UP);
            } else if ("KRW".equals(toCurrency)) {
                // 다른 통화 -> KRW: fromRateKrw
                rate = fromRateKrw;
            } else {
                // 다른 통화 -> 다른 통화: fromRateKrw / toRateKrw
                // 1 fromCurrency = fromRateKrw KRW
                // 1 toCurrency = toRateKrw KRW
                // 따라서 1 fromCurrency = fromRateKrw / toRateKrw toCurrency
                rate = fromRateKrw.divide(toRateKrw, 10, java.math.RoundingMode.HALF_UP);
            }
        } else {
            if (fromRateKrw == null) {
                log.warn("{} 통화를 찾을 수 없습니다.", fromCurrency);
                    }
            if (toRateKrw == null) {
                log.warn("{} 통화를 찾을 수 없습니다.", toCurrency);
            }
            return null;
        }
        
        log.info("파싱된 환율: {} -> {}, rate: {} (fromRateKrw: {}, toRateKrw: {})", 
                fromCurrency, toCurrency, rate, fromRateKrw, toRateKrw);
            
            return Currency.builder()
                    .fromCurrency(fromCurrency)
                    .toCurrency(toCurrency)
                    .rate(rate)
                .lastUpdated(LocalDateTime.now())
                    .build();
        }
    
    /**
     * 통화 코드 정규화
     * 한국수출입은행 API는 일부 통화에 (100) 같은 단위 표시가 있음
     */
    private String normalizeCurrencyCode(String currency) {
        if (currency == null) return null;
        // CNY는 CNH로 표시될 수 있음
        if ("CNY".equals(currency)) {
            return "CNH";
        }
        return currency.toUpperCase();
    }

    private Currency getDefaultRate(String fromCurrency, String toCurrency) {
        // 기본 환율 (최신 환율 기준으로 업데이트 필요)
        // USD 기준 환율을 사용하여 간접 변환
        BigDecimal defaultRate = BigDecimal.ONE;
        
        // USD 기준 환율 (1 USD = ?)
        BigDecimal usdToKrw = BigDecimal.valueOf(1465); // 1 USD = 1,465 KRW
        BigDecimal usdToEur = BigDecimal.valueOf(0.92); // 1 USD = 0.92 EUR
        BigDecimal usdToCny = BigDecimal.valueOf(7.25); // 1 USD = 7.25 CNY
        BigDecimal usdToJpy = BigDecimal.valueOf(150); // 1 USD = 150 JPY
        
        BigDecimal fromToUsd = null;
        BigDecimal toFromUsd = null;
        
        // fromCurrency를 USD로 변환하는 환율
        if ("USD".equals(fromCurrency)) {
            fromToUsd = BigDecimal.ONE;
        } else if ("KRW".equals(fromCurrency)) {
            fromToUsd = BigDecimal.ONE.divide(usdToKrw, 10, java.math.RoundingMode.HALF_UP);
        } else if ("EUR".equals(fromCurrency)) {
            fromToUsd = BigDecimal.ONE.divide(usdToEur, 10, java.math.RoundingMode.HALF_UP);
        } else if ("CNY".equals(fromCurrency)) {
            fromToUsd = BigDecimal.ONE.divide(usdToCny, 10, java.math.RoundingMode.HALF_UP);
        } else if ("JPY".equals(fromCurrency)) {
            fromToUsd = BigDecimal.ONE.divide(usdToJpy, 10, java.math.RoundingMode.HALF_UP);
        }
        
        // toCurrency를 USD로 변환하는 환율
        if ("USD".equals(toCurrency)) {
            toFromUsd = BigDecimal.ONE;
        } else if ("KRW".equals(toCurrency)) {
            toFromUsd = usdToKrw;
        } else if ("EUR".equals(toCurrency)) {
            toFromUsd = usdToEur;
        } else if ("CNY".equals(toCurrency)) {
            toFromUsd = usdToCny;
        } else if ("JPY".equals(toCurrency)) {
            toFromUsd = usdToJpy;
        }
        
        // 간접 변환: fromCurrency -> USD -> toCurrency
        if (fromToUsd != null && toFromUsd != null) {
            defaultRate = fromToUsd.multiply(toFromUsd);
        } else {
            // 알 수 없는 통화 쌍인 경우 경고
            log.warn("알 수 없는 통화 쌍: {} -> {}, 환율 1.0 사용", fromCurrency, toCurrency);
        }

        log.warn("기본 환율 사용: {} -> {}, 환율: {}", fromCurrency, toCurrency, defaultRate);
        return Currency.builder()
                .fromCurrency(fromCurrency)
                .toCurrency(toCurrency)
                .rate(defaultRate)
                .lastUpdated(LocalDateTime.now())
                .build();
    }
}

