package com.yd.travelbot.domain.food.application.dto;

import com.yd.travelbot.domain.food.domain.entity.Food;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("FoodResponse 테스트")
class FoodResponseTest {

    @Test
    @DisplayName("from 메서드 - Food 엔티티로부터 Response 생성")
    void from_메서드_엔티티_변환() {
        // given
        Food food = Food.builder()
                .id("food-123")
                .name("맛있는 식당")
                .address("서울시 마포구")
                .city("서울")
                .country("한국")
                .cuisine("한식")
                .priceRange(new BigDecimal("30000"))
                .rating(4.7)
                .description("전통 한식 전문점")
                .imageUrl("https://example.com/restaurant.jpg")
                .latitude(37.5665)
                .longitude(126.9780)
                .build();

        // when
        FoodResponse response = FoodResponse.from(food);

        // then
        assertThat(response.getId()).isEqualTo("food-123");
        assertThat(response.getName()).isEqualTo("맛있는 식당");
        assertThat(response.getAddress()).isEqualTo("서울시 마포구");
        assertThat(response.getCity()).isEqualTo("서울");
        assertThat(response.getCountry()).isEqualTo("한국");
        assertThat(response.getCuisine()).isEqualTo("한식");
        assertThat(response.getPriceRange()).isEqualByComparingTo(new BigDecimal("30000"));
        assertThat(response.getRating()).isEqualTo(4.7);
        assertThat(response.getDescription()).isEqualTo("전통 한식 전문점");
        assertThat(response.getImageUrl()).isEqualTo("https://example.com/restaurant.jpg");
        assertThat(response.getLatitude()).isEqualTo(37.5665);
        assertThat(response.getLongitude()).isEqualTo(126.9780);
    }

    @Test
    @DisplayName("빌더 패턴 - 모든 필드 포함하여 객체 생성")
    void 빌더_패턴_모든_필드() {
        // when
        FoodResponse response = FoodResponse.builder()
                .id("food-456")
                .name("이탈리안 레스토랑")
                .address("부산시 해운대구")
                .city("부산")
                .country("한국")
                .cuisine("이탈리안")
                .priceRange(new BigDecimal("50000"))
                .rating(4.9)
                .description("정통 이탈리안 요리")
                .imageUrl("https://example.com/italian.jpg")
                .latitude(35.1796)
                .longitude(129.0756)
                .build();

        // then
        assertThat(response.getId()).isEqualTo("food-456");
        assertThat(response.getName()).isEqualTo("이탈리안 레스토랑");
        assertThat(response.getCuisine()).isEqualTo("이탈리안");
        assertThat(response.getPriceRange()).isEqualByComparingTo(new BigDecimal("50000"));
        assertThat(response.getRating()).isEqualTo(4.9);
    }

    @Test
    @DisplayName("기본 생성자 - 빈 객체 생성 가능")
    void 기본_생성자_빈_객체() {
        // when
        FoodResponse response = new FoodResponse();

        // then
        assertThat(response).isNotNull();
        assertThat(response.getId()).isNull();
        assertThat(response.getName()).isNull();
    }
}

