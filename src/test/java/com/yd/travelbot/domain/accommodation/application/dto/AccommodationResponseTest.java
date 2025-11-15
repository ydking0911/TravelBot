package com.yd.travelbot.domain.accommodation.application.dto;

import com.yd.travelbot.domain.accommodation.domain.entity.Accommodation;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("AccommodationResponse 테스트")
class AccommodationResponseTest {

    @Test
    @DisplayName("from 메서드 - Accommodation 엔티티로부터 Response 생성")
    void from_메서드_엔티티_변환() {
        // given
        Accommodation accommodation = Accommodation.builder()
                .id("acc-123")
                .name("테스트 호텔")
                .address("서울시 강남구")
                .city("서울")
                .country("한국")
                .price(new BigDecimal("100000"))
                .currency("KRW")
                .rating(4.5)
                .description("편안한 호텔")
                .imageUrl("https://example.com/hotel.jpg")
                .checkIn(LocalDate.now().plusDays(1))
                .checkOut(LocalDate.now().plusDays(3))
                .guests(2)
                .build();

        // when
        AccommodationResponse response = AccommodationResponse.from(accommodation);

        // then
        assertThat(response.getId()).isEqualTo("acc-123");
        assertThat(response.getName()).isEqualTo("테스트 호텔");
        assertThat(response.getAddress()).isEqualTo("서울시 강남구");
        assertThat(response.getCity()).isEqualTo("서울");
        assertThat(response.getCountry()).isEqualTo("한국");
        assertThat(response.getPrice()).isEqualByComparingTo(new BigDecimal("100000"));
        assertThat(response.getCurrency()).isEqualTo("KRW");
        assertThat(response.getRating()).isEqualTo(4.5);
        assertThat(response.getDescription()).isEqualTo("편안한 호텔");
        assertThat(response.getImageUrl()).isEqualTo("https://example.com/hotel.jpg");
        assertThat(response.getCheckIn()).isEqualTo(LocalDate.now().plusDays(1));
        assertThat(response.getCheckOut()).isEqualTo(LocalDate.now().plusDays(3));
        assertThat(response.getGuests()).isEqualTo(2);
    }

    @Test
    @DisplayName("빌더 패턴 - 모든 필드 포함하여 객체 생성")
    void 빌더_패턴_모든_필드() {
        // when
        AccommodationResponse response = AccommodationResponse.builder()
                .id("acc-456")
                .name("빌더 테스트 호텔")
                .address("부산시 해운대구")
                .city("부산")
                .country("한국")
                .price(new BigDecimal("200000"))
                .currency("KRW")
                .rating(4.8)
                .description("럭셔리 호텔")
                .imageUrl("https://example.com/luxury.jpg")
                .checkIn(LocalDate.now().plusDays(5))
                .checkOut(LocalDate.now().plusDays(7))
                .guests(3)
                .build();

        // then
        assertThat(response.getId()).isEqualTo("acc-456");
        assertThat(response.getName()).isEqualTo("빌더 테스트 호텔");
        assertThat(response.getPrice()).isEqualByComparingTo(new BigDecimal("200000"));
        assertThat(response.getRating()).isEqualTo(4.8);
        assertThat(response.getGuests()).isEqualTo(3);
    }

    @Test
    @DisplayName("빌더 패턴 - 일부 필드만 포함하여 객체 생성")
    void 빌더_패턴_일부_필드() {
        // when
        AccommodationResponse response = AccommodationResponse.builder()
                .id("acc-789")
                .name("최소 정보 호텔")
                .city("제주")
                .build();

        // then
        assertThat(response.getId()).isEqualTo("acc-789");
        assertThat(response.getName()).isEqualTo("최소 정보 호텔");
        assertThat(response.getCity()).isEqualTo("제주");
        assertThat(response.getAddress()).isNull();
        assertThat(response.getPrice()).isNull();
        assertThat(response.getRating()).isNull();
    }

    @Test
    @DisplayName("기본 생성자 - 빈 객체 생성 가능")
    void 기본_생성자_빈_객체() {
        // when
        AccommodationResponse response = new AccommodationResponse();

        // then
        assertThat(response).isNotNull();
        assertThat(response.getId()).isNull();
        assertThat(response.getName()).isNull();
    }
}

