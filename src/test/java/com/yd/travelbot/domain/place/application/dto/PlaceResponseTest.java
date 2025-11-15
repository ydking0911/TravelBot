package com.yd.travelbot.domain.place.application.dto;

import com.yd.travelbot.domain.place.domain.entity.Place;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PlaceResponse 테스트")
class PlaceResponseTest {

    @Test
    @DisplayName("from 메서드 - Place 엔티티로부터 Response 생성")
    void from_메서드_엔티티_변환() {
        // given
        Place place = Place.builder()
                .id("place-123")
                .name("경복궁")
                .address("서울시 종로구")
                .city("서울")
                .country("한국")
                .category("역사")
                .description("조선 왕조의 첫 번째 궁궐")
                .imageUrl("https://example.com/palace.jpg")
                .rating(4.6)
                .latitude(37.5796)
                .longitude(126.9770)
                .entranceFee(new BigDecimal("3000"))
                .currency("KRW")
                .build();

        // when
        PlaceResponse response = PlaceResponse.from(place);

        // then
        assertThat(response.getId()).isEqualTo("place-123");
        assertThat(response.getName()).isEqualTo("경복궁");
        assertThat(response.getAddress()).isEqualTo("서울시 종로구");
        assertThat(response.getCity()).isEqualTo("서울");
        assertThat(response.getCountry()).isEqualTo("한국");
        assertThat(response.getCategory()).isEqualTo("역사");
        assertThat(response.getDescription()).isEqualTo("조선 왕조의 첫 번째 궁궐");
        assertThat(response.getImageUrl()).isEqualTo("https://example.com/palace.jpg");
        assertThat(response.getRating()).isEqualTo(4.6);
        assertThat(response.getLatitude()).isEqualTo(37.5796);
        assertThat(response.getLongitude()).isEqualTo(126.9770);
        assertThat(response.getEntranceFee()).isEqualByComparingTo(new BigDecimal("3000"));
        assertThat(response.getCurrency()).isEqualTo("KRW");
    }

    @Test
    @DisplayName("빌더 패턴 - 모든 필드 포함하여 객체 생성")
    void 빌더_패턴_모든_필드() {
        // when
        PlaceResponse response = PlaceResponse.builder()
                .id("place-456")
                .name("해운대 해수욕장")
                .address("부산시 해운대구")
                .city("부산")
                .country("한국")
                .category("해변")
                .description("아름다운 해수욕장")
                .imageUrl("https://example.com/beach.jpg")
                .rating(4.8)
                .latitude(35.1587)
                .longitude(129.1604)
                .entranceFee(BigDecimal.ZERO)
                .currency("KRW")
                .build();

        // then
        assertThat(response.getId()).isEqualTo("place-456");
        assertThat(response.getName()).isEqualTo("해운대 해수욕장");
        assertThat(response.getCategory()).isEqualTo("해변");
        assertThat(response.getRating()).isEqualTo(4.8);
        assertThat(response.getEntranceFee()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("기본 생성자 - 빈 객체 생성 가능")
    void 기본_생성자_빈_객체() {
        // when
        PlaceResponse response = new PlaceResponse();

        // then
        assertThat(response).isNotNull();
        assertThat(response.getId()).isNull();
        assertThat(response.getName()).isNull();
    }
}

