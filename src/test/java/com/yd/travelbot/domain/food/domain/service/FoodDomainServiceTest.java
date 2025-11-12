package com.yd.travelbot.domain.food.domain.service;

import com.yd.travelbot.domain.food.domain.entity.Food;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("FoodDomainService 테스트")
class FoodDomainServiceTest {

    private FoodDomainService domainService;
    private List<Food> foods;

    @BeforeEach
    void setUp() {
        domainService = new FoodDomainService();
        
        foods = new ArrayList<>();
        foods.add(Food.builder()
                .id("1")
                .name("맛집 A")
                .rating(4.5)
                .priceRange(new BigDecimal("30000"))
                .build());
        foods.add(Food.builder()
                .id("2")
                .name("맛집 B")
                .rating(4.0)
                .priceRange(new BigDecimal("50000"))
                .build());
        foods.add(Food.builder()
                .id("3")
                .name("맛집 C")
                .rating(4.8)
                .priceRange(new BigDecimal("20000"))
                .build());
        foods.add(Food.builder()
                .id("4")
                .name("맛집 D")
                .rating(3.5)
                .priceRange(new BigDecimal("40000"))
                .build());
    }

    @Test
    @DisplayName("평점 필터링 - minRating으로 필터링")
    void 평점_필터링_성공() {
        // given
        Double minRating = 4.5;

        // when
        List<Food> result = domainService.filterByRating(foods, minRating);

        // then
        assertThat(result).hasSize(2);
        assertThat(result).extracting(Food::getName)
                .containsExactlyInAnyOrder("맛집 A", "맛집 C");
        assertThat(result).allMatch(food -> food.getRating() != null && food.getRating() >= minRating);
    }

    @Test
    @DisplayName("평점 필터링 - rating이 null인 경우 제외")
    void 평점_필터링_rating_null_제외() {
        // given
        foods.add(Food.builder()
                .id("5")
                .name("맛집 E")
                .rating(null)
                .priceRange(new BigDecimal("25000"))
                .build());
        Double minRating = 4.0;

        // when
        List<Food> result = domainService.filterByRating(foods, minRating);

        // then
        assertThat(result).hasSize(3);
        assertThat(result).extracting(Food::getName)
                .doesNotContain("맛집 E");
    }

    @Test
    @DisplayName("가격 범위 필터링 - maxPrice로 필터링")
    void 가격_범위_필터링_성공() {
        // given
        BigDecimal maxPrice = new BigDecimal("30000");

        // when
        List<Food> result = domainService.filterByPriceRange(foods, maxPrice);

        // then
        assertThat(result).hasSize(2);
        assertThat(result).extracting(Food::getName)
                .containsExactlyInAnyOrder("맛집 A", "맛집 C");
        assertThat(result).allMatch(food -> 
                food.getPriceRange() != null && 
                food.getPriceRange().compareTo(maxPrice) <= 0);
    }

    @Test
    @DisplayName("가격 범위 필터링 - priceRange가 null인 경우 제외")
    void 가격_범위_필터링_priceRange_null_제외() {
        // given
        foods.add(Food.builder()
                .id("5")
                .name("맛집 E")
                .rating(4.0)
                .priceRange(null)
                .build());
        BigDecimal maxPrice = new BigDecimal("50000");

        // when
        List<Food> result = domainService.filterByPriceRange(foods, maxPrice);

        // then
        assertThat(result).hasSize(4);
        assertThat(result).extracting(Food::getName)
                .doesNotContain("맛집 E");
    }

    @Test
    @DisplayName("가격 범위 필터링 - 모든 음식점이 maxPrice보다 비싼 경우")
    void 가격_범위_필터링_모두_비싼_경우() {
        // given
        BigDecimal maxPrice = new BigDecimal("10000");

        // when
        List<Food> result = domainService.filterByPriceRange(foods, maxPrice);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("가격 범위 필터링 - 모든 음식점이 maxPrice 이하인 경우")
    void 가격_범위_필터링_모두_저렴한_경우() {
        // given
        BigDecimal maxPrice = new BigDecimal("100000");

        // when
        List<Food> result = domainService.filterByPriceRange(foods, maxPrice);

        // then
        assertThat(result).hasSize(4);
        assertThat(result).containsExactlyElementsOf(foods);
    }
}

