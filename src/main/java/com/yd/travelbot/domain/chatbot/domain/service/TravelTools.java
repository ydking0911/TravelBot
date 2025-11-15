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
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * LangChain4j Tools를 사용하여 멀티홉 추론을 지원하는 여행 관련 도구들
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TravelTools {

    private final SearchAccommodationUseCase searchAccommodationUseCase;
    private final SearchFoodUseCase searchFoodUseCase;
    private final SearchPlaceUseCase searchPlaceUseCase;
    private final ConvertCurrencyUseCase convertCurrencyUseCase;

    @Tool("특정 도시의 숙소를 검색합니다. 도시명은 한국어, 영어, 또는 다른 언어로 입력할 수 있습니다.")
    public String searchAccommodation(
            @P("검색할 도시명 (예: 서울, 부산, 제주도, Tokyo, Paris, New York)") String city,
            @P("체크인 날짜 (YYYY-MM-DD 형식, 선택사항)") String checkIn,
            @P("체크아웃 날짜 (YYYY-MM-DD 형식, 선택사항)") String checkOut,
            @P("게스트 수 (선택사항, 기본값: 1)") Integer guests
    ) {
        try {
            LocalDate checkInDate = checkIn != null && !checkIn.isEmpty() 
                ? LocalDate.parse(checkIn) 
                : LocalDate.now().plusDays(1);
            LocalDate checkOutDate = checkOut != null && !checkOut.isEmpty() 
                ? LocalDate.parse(checkOut) 
                : checkInDate.plusDays(1);
            int guestCount = guests != null ? guests : 1;

            AccommodationSearchRequest request = AccommodationSearchRequest.builder()
                    .city(city)
                    .checkIn(checkInDate)
                    .checkOut(checkOutDate)
                    .guests(guestCount)
                    .build();

            List<AccommodationResponse> accommodations = searchAccommodationUseCase.execute(request);
            
            if (accommodations.isEmpty()) {
                return String.format("%s에서 숙소를 찾지 못했습니다.", city);
            }

            StringBuilder result = new StringBuilder();
            result.append(String.format("%s 숙소 검색 결과 (%d개):\n", city, accommodations.size()));
            int count = Math.min(accommodations.size(), 10);
            for (int i = 0; i < count; i++) {
                AccommodationResponse acc = accommodations.get(i);
                result.append(String.format("%d. %s", i + 1, acc.getName()));
                if (acc.getAddress() != null && !acc.getAddress().isEmpty()) {
                    result.append(String.format(" - %s", acc.getAddress()));
                }
                if (acc.getPrice() != null && acc.getCurrency() != null) {
                    result.append(String.format(" (%s %s/박)", acc.getPrice(), acc.getCurrency()));
                }
                if (acc.getRating() != null) {
                    result.append(String.format(" ⭐%.1f", acc.getRating()));
                }
                result.append("\n");
            }
            return result.toString();
        } catch (Exception e) {
            log.error("숙소 검색 실패: {}", e.getMessage(), e);
            return String.format("숙소 검색 중 오류가 발생했습니다: %s", e.getMessage());
        }
    }

    @Tool("특정 도시의 음식점을 검색합니다. 도시명은 한국어, 영어, 또는 다른 언어로 입력할 수 있습니다.")
    public String searchFood(
            @P("검색할 도시명 (예: 서울, 부산, 제주도, Tokyo, Paris, New York)") String city,
            @P("음식 종류 (예: 한식, 중식, 일식, 양식, Korean, Chinese, Japanese, Italian, 선택사항)") String cuisine
    ) {
        try {
            FoodSearchRequest request = FoodSearchRequest.builder()
                    .city(city)
                    .cuisine(cuisine)
                    .build();

            List<FoodResponse> foods = searchFoodUseCase.execute(request);
            
            if (foods.isEmpty()) {
                return String.format("%s에서 음식점을 찾지 못했습니다.", city);
            }

            StringBuilder result = new StringBuilder();
            result.append(String.format("%s 음식점 검색 결과 (%d개):\n", city, foods.size()));
            int count = Math.min(foods.size(), 10);
            for (int i = 0; i < count; i++) {
                FoodResponse food = foods.get(i);
                result.append(String.format("%d. %s", i + 1, food.getName()));
                if (food.getDescription() != null && !food.getDescription().isEmpty()) {
                    result.append(String.format(" - %s", food.getDescription()));
                }
                if (food.getAddress() != null && !food.getAddress().isEmpty()) {
                    result.append(String.format(" (주소: %s)", food.getAddress()));
                }
                if (food.getCuisine() != null && !food.getCuisine().isEmpty()) {
                    result.append(String.format(" [%s]", food.getCuisine()));
                }
                if (food.getRating() != null) {
                    result.append(String.format(" ⭐%.1f", food.getRating()));
                }
                if (food.getImageUrl() != null && !food.getImageUrl().isEmpty()) {
                    result.append(String.format(" ![이미지](%s)", food.getImageUrl()));
                }
                result.append("\n");
            }
            return result.toString();
        } catch (Exception e) {
            log.error("음식점 검색 실패: {}", e.getMessage(), e);
            return String.format("음식점 검색 중 오류가 발생했습니다: %s", e.getMessage());
        }
    }

    @Tool("특정 도시의 관광지를 검색합니다. 도시명은 한국어, 영어, 또는 다른 언어로 입력할 수 있습니다.")
    public String searchPlace(
            @P("검색할 도시명 (예: 서울, 부산, 제주도, Tokyo, Paris, New York)") String city,
            @P("카테고리 (예: 박물관, 미술관, 공원, 해변, museum, park, beach, 선택사항)") String category
    ) {
        try {
            PlaceSearchRequest request = PlaceSearchRequest.builder()
                    .city(city)
                    .category(category)
                    .build();

            List<PlaceResponse> places = searchPlaceUseCase.execute(request);
            
            if (places.isEmpty()) {
                return String.format("%s에서 관광지를 찾지 못했습니다.", city);
            }

            StringBuilder result = new StringBuilder();
            result.append(String.format("%s 관광지 검색 결과 (%d개):\n", city, places.size()));
            int count = Math.min(places.size(), 10);
            for (int i = 0; i < count; i++) {
                PlaceResponse place = places.get(i);
                result.append(String.format("%d. %s", i + 1, place.getName()));
                if (place.getAddress() != null && !place.getAddress().isEmpty()) {
                    result.append(String.format(" - %s", place.getAddress()));
                }
                if (place.getCategory() != null && !place.getCategory().isEmpty()) {
                    result.append(String.format(" (%s)", place.getCategory()));
                }
                if (place.getRating() != null) {
                    result.append(String.format(" ⭐%.1f", place.getRating()));
                }
                result.append("\n");
            }
            return result.toString();
        } catch (Exception e) {
            log.error("관광지 검색 실패: {}", e.getMessage(), e);
            return String.format("관광지 검색 중 오류가 발생했습니다: %s", e.getMessage());
        }
    }

    @Tool("통화를 변환합니다.")
    public String convertCurrency(
            @P("변환할 금액") BigDecimal amount,
            @P("출발 통화 코드 (예: KRW, USD, EUR, JPY)") String fromCurrency,
            @P("목표 통화 코드 (예: KRW, USD, EUR, JPY)") String toCurrency
    ) {
        try {
            CurrencyConvertRequest request = CurrencyConvertRequest.builder()
                    .fromCurrency(fromCurrency)
                    .toCurrency(toCurrency)
                    .amount(amount)
                    .build();

            CurrencyResponse response = convertCurrencyUseCase.execute(request);
            
            String result = String.format(
                "%s %s = %s %s (환율: 1 %s = %s %s)",
                formatAmount(amount, fromCurrency),
                fromCurrency,
                formatAmount(response.getConvertedAmount(), toCurrency),
                toCurrency,
                fromCurrency,
                response.getRate(),
                toCurrency
            );
            
            // 유의사항 추가
            result += "\n\n[유의사항] 본 환율은 한국수출입은행의 일자 기준 고시 환율로, 실시간 시세와 다를 수 있습니다. 일부 통화는 CNH(역외 위안) 또는 JPY(100)처럼 단위 표기가 적용됩니다.";
            
            return result;
        } catch (Exception e) {
            log.error("환율 변환 실패: {}", e.getMessage(), e);
            return String.format("환율 변환 중 오류가 발생했습니다: %s", e.getMessage());
        }
    }

    private String formatAmount(BigDecimal amount, String currency) {
        if ("KRW".equals(currency)) {
            return String.format("%,.0f", amount.doubleValue());
        } else {
            return String.format("%,.2f", amount.doubleValue());
        }
    }
}

