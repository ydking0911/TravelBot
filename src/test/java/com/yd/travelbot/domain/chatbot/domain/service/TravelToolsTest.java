package com.yd.travelbot.domain.chatbot.domain.service;

import com.yd.travelbot.domain.accommodation.application.dto.AccommodationResponse;
import com.yd.travelbot.domain.accommodation.application.usecase.SearchAccommodationUseCase;
import com.yd.travelbot.domain.currency.application.dto.CurrencyResponse;
import com.yd.travelbot.domain.currency.application.usecase.ConvertCurrencyUseCase;
import com.yd.travelbot.domain.food.application.dto.FoodResponse;
import com.yd.travelbot.domain.food.application.usecase.SearchFoodUseCase;
import com.yd.travelbot.domain.place.application.dto.PlaceResponse;
import com.yd.travelbot.domain.place.application.usecase.SearchPlaceUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TravelTools 테스트")
class TravelToolsTest {

    @Mock
    private SearchAccommodationUseCase searchAccommodationUseCase;

    @Mock
    private SearchFoodUseCase searchFoodUseCase;

    @Mock
    private SearchPlaceUseCase searchPlaceUseCase;

    @Mock
    private ConvertCurrencyUseCase convertCurrencyUseCase;

    @InjectMocks
    private TravelTools travelTools;

    @Test
    @DisplayName("searchAccommodation - 숙소 검색 도구 호출")
    void searchAccommodation_성공() {
        // given
        String city = "서울";
        String checkIn = LocalDate.now().plusDays(1).toString();
        String checkOut = LocalDate.now().plusDays(3).toString();
        Integer guests = 2;

        List<AccommodationResponse> accommodations = new ArrayList<>();
        accommodations.add(AccommodationResponse.builder()
                .name("호텔 A")
                .address("서울시 강남구")
                .price(new BigDecimal("100000"))
                .currency("KRW")
                .rating(4.5)
                .build());

        when(searchAccommodationUseCase.execute(any()))
                .thenReturn(accommodations);

        // when
        String result = travelTools.searchAccommodation(city, checkIn, checkOut, guests);

        // then
        assertThat(result).isNotNull();
        assertThat(result).contains("서울");
        assertThat(result).contains("호텔 A");
        verify(searchAccommodationUseCase).execute(any());
    }

    @Test
    @DisplayName("searchFood - 음식점 검색 도구 호출")
    void searchFood_성공() {
        // given
        String city = "서울";
        String cuisine = "한식";

        List<FoodResponse> foods = new ArrayList<>();
        foods.add(FoodResponse.builder()
                .name("맛집 A")
                .address("서울시 강남구")
                .cuisine("한식")
                .rating(4.5)
                .build());

        when(searchFoodUseCase.execute(any()))
                .thenReturn(foods);

        // when
        String result = travelTools.searchFood(city, cuisine);

        // then
        assertThat(result).isNotNull();
        assertThat(result).contains("서울");
        assertThat(result).contains("맛집 A");
        verify(searchFoodUseCase).execute(any());
    }

    @Test
    @DisplayName("searchPlace - 관광지 검색 도구 호출")
    void searchPlace_성공() {
        // given
        String city = "서울";
        String category = "박물관";

        List<PlaceResponse> places = new ArrayList<>();
        places.add(PlaceResponse.builder()
                .name("국립중앙박물관")
                .address("서울시 용산구")
                .category("박물관")
                .rating(4.5)
                .build());

        when(searchPlaceUseCase.execute(any()))
                .thenReturn(places);

        // when
        String result = travelTools.searchPlace(city, category);

        // then
        assertThat(result).isNotNull();
        assertThat(result).contains("서울");
        assertThat(result).contains("국립중앙박물관");
        verify(searchPlaceUseCase).execute(any());
    }

    @Test
    @DisplayName("convertCurrency - 환율 변환 도구 호출")
    void convertCurrency_성공() {
        // given
        String fromCurrency = "USD";
        String toCurrency = "KRW";
        BigDecimal amount = new BigDecimal("100");

        CurrencyResponse currencyResponse = CurrencyResponse.builder()
                .fromCurrency("USD")
                .toCurrency("KRW")
                .rate(new BigDecimal("1300.50"))
                .convertedAmount(new BigDecimal("130050.00"))
                .build();

        when(convertCurrencyUseCase.execute(any()))
                .thenReturn(currencyResponse);

        // when
        String result = travelTools.convertCurrency(amount, fromCurrency, toCurrency);

        // then
        assertThat(result).isNotNull();
        assertThat(result).contains("USD");
        assertThat(result).contains("KRW");
        // formatAmount로 포맷되므로 "130,050" 또는 "130050" 형태일 수 있음
        assertThat(result).containsAnyOf("130050", "130,050", "130,050.00");
        verify(convertCurrencyUseCase).execute(any());
    }

    @Test
    @DisplayName("예외 처리 - searchAccommodation 예외 발생 시 처리")
    void 예외_처리_searchAccommodation() {
        // given
        String city = "서울";
        String checkIn = LocalDate.now().plusDays(1).toString();
        String checkOut = LocalDate.now().plusDays(3).toString();
        Integer guests = 2;

        when(searchAccommodationUseCase.execute(any()))
                .thenThrow(new RuntimeException("검색 실패"));

        // when
        String result = travelTools.searchAccommodation(city, checkIn, checkOut, guests);

        // then
        assertThat(result).isNotNull();
        assertThat(result).contains("오류가 발생했습니다");
        verify(searchAccommodationUseCase).execute(any());
    }

    @Test
    @DisplayName("빈 결과 처리 - searchAccommodation 빈 결과")
    void 빈_결과_처리_searchAccommodation() {
        // given
        String city = "존재하지않는도시";
        String checkIn = LocalDate.now().plusDays(1).toString();
        String checkOut = LocalDate.now().plusDays(3).toString();
        Integer guests = 2;

        when(searchAccommodationUseCase.execute(any()))
                .thenReturn(new ArrayList<>());

        // when
        String result = travelTools.searchAccommodation(city, checkIn, checkOut, guests);

        // then
        assertThat(result).isNotNull();
        assertThat(result).contains("찾지 못했습니다");
    }
}

