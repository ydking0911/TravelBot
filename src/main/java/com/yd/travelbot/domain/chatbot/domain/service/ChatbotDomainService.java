package com.yd.travelbot.domain.chatbot.domain.service;

import com.yd.travelbot.domain.accommodation.application.dto.AccommodationSearchRequest;
import com.yd.travelbot.domain.accommodation.application.dto.AccommodationResponse;
import com.yd.travelbot.domain.accommodation.application.usecase.SearchAccommodationUseCase;
import com.yd.travelbot.domain.currency.application.dto.CurrencyConvertRequest;
import com.yd.travelbot.domain.currency.application.dto.CurrencyResponse;
import com.yd.travelbot.domain.currency.application.usecase.ConvertCurrencyUseCase;
import com.yd.travelbot.domain.food.application.dto.FoodSearchRequest;
import com.yd.travelbot.domain.food.application.dto.FoodResponse;
import com.yd.travelbot.domain.food.application.usecase.SearchFoodUseCase;
import com.yd.travelbot.domain.place.application.dto.PlaceSearchRequest;
import com.yd.travelbot.domain.place.application.dto.PlaceResponse;
import com.yd.travelbot.domain.place.application.usecase.SearchPlaceUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatbotDomainService {

    private final IntentAnalyzer intentAnalyzer;
    private final MessageParser messageParser;
    private final SearchAccommodationUseCase searchAccommodationUseCase;
    private final SearchFoodUseCase searchFoodUseCase;
    private final SearchPlaceUseCase searchPlaceUseCase;
    private final ConvertCurrencyUseCase convertCurrencyUseCase;
    private final ChatService chatService;

    public String processMessage(String userInput) {
        String lowerInput = userInput.toLowerCase();
        IntentAnalyzer.Intent intent = intentAnalyzer.analyze(lowerInput);

        return switch (intent) {
            case ACCOMMODATION -> handleAccommodationSearch(userInput);
            case FOOD -> handleFoodSearch(userInput);
            case PLACE -> handlePlaceSearch(userInput);
            case CURRENCY -> handleCurrencyConversion(userInput);
            case GENERAL -> throw new UnsupportedOperationException("일반 대화는 UseCase에서 처리해야 합니다");
        };
    }

    private String handleAccommodationSearch(String userInput) {
        try {
            String city = messageParser.extractCity(userInput);
            if (city == null) {
                return "어느 도시의 숙소를 찾고 계신가요? (예: 서울, 부산, 제주도)";
            }

            LocalDate checkIn = messageParser.extractDate(userInput);
            LocalDate checkOut = messageParser.extractDate(userInput);
            
            if (checkIn == null) {
                checkIn = LocalDate.now().plusDays(1);
            }
            if (checkOut == null) {
                checkOut = checkIn.plusDays(1);
            }

            Integer guests = messageParser.extractNumber(userInput);
            if (guests == null) {
                guests = 1;
            }

            AccommodationSearchRequest request = AccommodationSearchRequest.builder()
                    .city(city)
                    .checkIn(checkIn)
                    .checkOut(checkOut)
                    .guests(guests)
                    .build();

            List<AccommodationResponse> accommodations = searchAccommodationUseCase.execute(request);

            // LLM을 통해 자연스러운 대화 형식으로 포맷팅
            return chatService.formatAccommodationResults(userInput, accommodations);
        } catch (Exception e) {
            log.error("숙소 검색 중 오류 발생: {}", e.getMessage(), e);
            return "숙소 검색 중 오류가 발생했습니다: " + e.getMessage();
        }
    }

    private String handleFoodSearch(String userInput) {
        try {
            String city = messageParser.extractCity(userInput);
            if (city == null) {
                return "어느 도시의 음식점을 찾고 계신가요? (예: 서울, 부산, 제주도)";
            }

            String cuisine = messageParser.extractCuisine(userInput);

            FoodSearchRequest request = FoodSearchRequest.builder()
                    .city(city)
                    .cuisine(cuisine)
                    .build();

            List<FoodResponse> foods = searchFoodUseCase.execute(request);

            // LLM을 통해 자연스러운 대화 형식으로 포맷팅
            return chatService.formatFoodResults(userInput, foods);
        } catch (Exception e) {
            log.error("음식점 검색 중 오류 발생: {}", e.getMessage(), e);
            return "음식점 검색 중 오류가 발생했습니다: " + e.getMessage();
        }
    }

    private String handlePlaceSearch(String userInput) {
        try {
            String city = messageParser.extractCity(userInput);
            if (city == null) {
                return "어느 도시의 관광지를 찾고 계신가요? (예: 서울, 부산, 제주도)";
            }

            String category = messageParser.extractCategory(userInput);

            PlaceSearchRequest request = PlaceSearchRequest.builder()
                    .city(city)
                    .category(category)
                    .build();

            List<PlaceResponse> places = searchPlaceUseCase.execute(request);

            // LLM을 통해 자연스러운 대화 형식으로 포맷팅
            return chatService.formatPlaceResults(userInput, places);
        } catch (Exception e) {
            log.error("관광지 검색 중 오류 발생: {}", e.getMessage(), e);
            return "관광지 검색 중 오류가 발생했습니다: " + e.getMessage();
        }
    }

    private String handleCurrencyConversion(String userInput) {
        try {
            BigDecimal amount = messageParser.extractAmount(userInput);
            String fromCurrency = messageParser.extractCurrency(userInput, "from");
            String toCurrency = messageParser.extractCurrency(userInput, "to");

            if (amount == null || fromCurrency == null || toCurrency == null) {
                return "환율 변환을 위해 금액과 통화를 명시해주세요. (예: 100만원을 USD로 변환)";
            }

            CurrencyConvertRequest request = CurrencyConvertRequest.builder()
                    .fromCurrency(fromCurrency)
                    .toCurrency(toCurrency)
                    .amount(amount)
                    .build();

            CurrencyResponse response = convertCurrencyUseCase.execute(request);

            // 금액 포맷팅
            String formattedAmount = formatCurrencyAmount(amount, fromCurrency);
            String formattedConvertedAmount = formatCurrencyAmount(response.getConvertedAmount(), toCurrency);
            String formattedRate = formatRate(response.getRate(), fromCurrency, toCurrency);
            
            // 날짜 포맷팅
            String dateStr = response.getLastUpdated().format(DateTimeFormatter.ofPattern("yyyy년 MM월 dd일 HH:mm"));
            
            StringBuilder result = new StringBuilder();
            result.append(String.format("💰 환율 변환 결과\n\n"));
            result.append(String.format("%s %s = %s %s\n\n", formattedAmount, fromCurrency, formattedConvertedAmount, toCurrency));
            result.append(String.format("📊 현재 환율: %s\n", formattedRate));
            result.append(String.format("🕐 기준 시각: %s (한국수출입은행 고시 기준)\n", dateStr));
            result.append("\n");
            result.append("ℹ️ 유의: 본 환율은 일자 기준 고시 환율로, 실시간 시세와 다를 수 있습니다. ");
            result.append("일부 통화는 CNH(역외 위안) 또는 JPY(100)처럼 단위 표기가 적용됩니다.");
            
            return result.toString();
        } catch (Exception e) {
            log.error("환율 변환 중 오류 발생: {}", e.getMessage(), e);
            return "환율 변환 중 오류가 발생했습니다: " + e.getMessage();
        }
    }

    private String formatCurrencyAmount(BigDecimal amount, String currency) {
        if ("KRW".equals(currency)) {
            // 원화는 천 단위 구분자 사용
            return String.format("%,.0f", amount.doubleValue());
        } else {
            // 다른 통화는 소수점 2자리까지
            return String.format("%,.2f", amount.doubleValue());
        }
    }

    private String formatRate(BigDecimal rate, String fromCurrency, String toCurrency) {
        if ("KRW".equals(fromCurrency)) {
            // 원화에서 다른 통화로: 1 KRW = 0.00077 USD 형식
            return String.format("1 %s = %s %s", fromCurrency, String.format("%.6f", rate.doubleValue()), toCurrency);
        } else if ("KRW".equals(toCurrency)) {
            // 다른 통화에서 원화로: 1 USD = 1,300 KRW 형식
            return String.format("1 %s = %s %s", fromCurrency, String.format("%,.2f", rate.doubleValue()), toCurrency);
        } else {
            // 기타 통화 간 변환
            return String.format("1 %s = %s %s", fromCurrency, String.format("%.4f", rate.doubleValue()), toCurrency);
        }
    }
}

