package com.yd.travelbot.domain.food.application.usecase;

import com.yd.travelbot.domain.food.application.dto.FoodResponse;
import com.yd.travelbot.domain.food.application.dto.FoodSearchRequest;
import com.yd.travelbot.domain.food.domain.entity.Food;
import com.yd.travelbot.domain.food.domain.repository.FoodRepository;
import com.yd.travelbot.domain.food.domain.service.FoodDomainService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SearchFoodUseCase 테스트")
class SearchFoodUseCaseTest {

    @Mock
    private FoodRepository foodRepository;

    @Mock
    private FoodDomainService domainService;

    @InjectMocks
    private SearchFoodUseCase useCase;

    private List<Food> foods;

    @BeforeEach
    void setUp() {
        foods = new ArrayList<>();
        foods.add(Food.builder()
                .id("1")
                .name("맛집 A")
                .city("서울")
                .cuisine("한식")
                .rating(4.5)
                .priceRange(new BigDecimal("30000"))
                .build());
        foods.add(Food.builder()
                .id("2")
                .name("맛집 B")
                .city("서울")
                .cuisine("중식")
                .rating(4.0)
                .priceRange(new BigDecimal("50000"))
                .build());
    }

    @Test
    @DisplayName("정상 검색 - 도시, 요리 종류로 음식점 검색")
    void 정상_검색_성공() {
        // given
        FoodSearchRequest request = FoodSearchRequest.builder()
                .city("서울")
                .cuisine("한식")
                .build();

        when(foodRepository.search("서울", "한식"))
                .thenReturn(foods);

        // when
        List<FoodResponse> result = useCase.execute(request);

        // then
        assertThat(result).hasSize(2);
        assertThat(result).extracting(FoodResponse::getName)
                .containsExactlyInAnyOrder("맛집 A", "맛집 B");
        verify(foodRepository).search("서울", "한식");
    }

    @Test
    @DisplayName("좌표 기반 검색 - latitude, longitude로 주변 검색")
    void 좌표_기반_검색_성공() {
        // given
        Double latitude = 37.5665;
        Double longitude = 126.9780;
        Double radius = 5.0;
        FoodSearchRequest request = FoodSearchRequest.builder()
                .latitude(latitude)
                .longitude(longitude)
                .radius(radius)
                .build();

        when(foodRepository.searchNearby(latitude, longitude, radius))
                .thenReturn(foods);

        // when
        List<FoodResponse> result = useCase.execute(request);

        // then
        assertThat(result).hasSize(2);
        verify(foodRepository).searchNearby(latitude, longitude, radius);
        verify(foodRepository, never()).search(anyString(), anyString());
    }

    @Test
    @DisplayName("반경 설정 - radius가 null일 때 기본값 5.0 사용")
    void 반경_설정_기본값_사용() {
        // given
        Double latitude = 37.5665;
        Double longitude = 126.9780;
        FoodSearchRequest request = FoodSearchRequest.builder()
                .latitude(latitude)
                .longitude(longitude)
                .radius(null)
                .build();

        when(foodRepository.searchNearby(latitude, longitude, 5.0))
                .thenReturn(foods);

        // when
        List<FoodResponse> result = useCase.execute(request);

        // then
        assertThat(result).hasSize(2);
        verify(foodRepository).searchNearby(latitude, longitude, 5.0);
    }

    @Test
    @DisplayName("평점 필터링 - minRating으로 필터링")
    void 평점_필터링_성공() {
        // given
        FoodSearchRequest request = FoodSearchRequest.builder()
                .city("서울")
                .cuisine("한식")
                .minRating(4.5)
                .build();

        when(foodRepository.search("서울", "한식"))
                .thenReturn(foods);
        when(domainService.filterByRating(foods, 4.5))
                .thenReturn(List.of(foods.get(0)));

        // when
        List<FoodResponse> result = useCase.execute(request);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("맛집 A");
        verify(domainService).filterByRating(foods, 4.5);
    }

    @Test
    @DisplayName("가격 필터링 - maxPrice로 필터링")
    void 가격_필터링_성공() {
        // given
        BigDecimal maxPrice = new BigDecimal("40000");
        FoodSearchRequest request = FoodSearchRequest.builder()
                .city("서울")
                .cuisine("한식")
                .maxPrice(maxPrice)
                .build();

        when(foodRepository.search("서울", "한식"))
                .thenReturn(foods);
        when(domainService.filterByPriceRange(foods, maxPrice))
                .thenReturn(List.of(foods.get(0)));

        // when
        List<FoodResponse> result = useCase.execute(request);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("맛집 A");
        verify(domainService).filterByPriceRange(foods, maxPrice);
    }

    @Test
    @DisplayName("평점과 가격 필터링 동시 적용")
    void 평점과_가격_필터링_동시_적용() {
        // given
        Double minRating = 4.0;
        BigDecimal maxPrice = new BigDecimal("40000");
        FoodSearchRequest request = FoodSearchRequest.builder()
                .city("서울")
                .cuisine("한식")
                .minRating(minRating)
                .maxPrice(maxPrice)
                .build();

        when(foodRepository.search("서울", "한식"))
                .thenReturn(foods);
        when(domainService.filterByRating(foods, minRating))
                .thenReturn(foods);
        when(domainService.filterByPriceRange(foods, maxPrice))
                .thenReturn(List.of(foods.get(0)));

        // when
        List<FoodResponse> result = useCase.execute(request);

        // then
        assertThat(result).hasSize(1);
        verify(domainService).filterByRating(foods, minRating);
        verify(domainService).filterByPriceRange(foods, maxPrice);
    }

    @Test
    @DisplayName("빈 결과 - 검색 결과가 없을 때 빈 리스트 반환")
    void 빈_결과_반환() {
        // given
        FoodSearchRequest request = FoodSearchRequest.builder()
                .city("서울")
                .cuisine("한식")
                .build();

        when(foodRepository.search("서울", "한식"))
                .thenReturn(new ArrayList<>());

        // when
        List<FoodResponse> result = useCase.execute(request);

        // then
        assertThat(result).isEmpty();
    }
}

