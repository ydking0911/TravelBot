package com.yd.travelbot.domain.accommodation.domain.service;

import com.yd.travelbot.domain.accommodation.domain.entity.Accommodation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("AccommodationDomainService 테스트")
class AccommodationDomainServiceTest {

    private AccommodationDomainService domainService;
    private List<Accommodation> accommodations;

    @BeforeEach
    void setUp() {
        domainService = new AccommodationDomainService();
        
        accommodations = new ArrayList<>();
        accommodations.add(Accommodation.builder()
                .id("1")
                .name("호텔 A")
                .price(new BigDecimal("100000"))
                .rating(4.5)
                .build());
        accommodations.add(Accommodation.builder()
                .id("2")
                .name("호텔 B")
                .price(new BigDecimal("150000"))
                .rating(4.0)
                .build());
        accommodations.add(Accommodation.builder()
                .id("3")
                .name("호텔 C")
                .price(new BigDecimal("200000"))
                .rating(4.8)
                .build());
        accommodations.add(Accommodation.builder()
                .id("4")
                .name("호텔 D")
                .price(new BigDecimal("80000"))
                .rating(3.5)
                .build());
    }

    @Test
    @DisplayName("가격 범위 필터링 - minPrice와 maxPrice 모두 지정")
    void 가격_범위_필터링_성공() {
        // given
        Double minPrice = 100000.0;
        Double maxPrice = 150000.0;

        // when
        List<Accommodation> result = domainService.filterByPriceRange(accommodations, minPrice, maxPrice);

        // then
        assertThat(result).hasSize(2);
        assertThat(result).extracting(Accommodation::getName)
                .containsExactlyInAnyOrder("호텔 A", "호텔 B");
        assertThat(result).allMatch(acc -> {
            double price = acc.getPrice().doubleValue();
            return price >= minPrice && price <= maxPrice;
        });
    }

    @Test
    @DisplayName("가격 범위 필터링 - minPrice만 지정")
    void 가격_범위_필터링_minPrice만_지정() {
        // given
        Double minPrice = 150000.0;
        Double maxPrice = null;

        // when
        List<Accommodation> result = domainService.filterByPriceRange(accommodations, minPrice, maxPrice);

        // then
        assertThat(result).hasSize(2);
        assertThat(result).extracting(Accommodation::getName)
                .containsExactlyInAnyOrder("호텔 B", "호텔 C");
        assertThat(result).allMatch(acc -> acc.getPrice().doubleValue() >= minPrice);
    }

    @Test
    @DisplayName("가격 범위 필터링 - maxPrice만 지정")
    void 가격_범위_필터링_maxPrice만_지정() {
        // given
        Double minPrice = null;
        Double maxPrice = 100000.0;

        // when
        List<Accommodation> result = domainService.filterByPriceRange(accommodations, minPrice, maxPrice);

        // then
        assertThat(result).hasSize(2);
        assertThat(result).extracting(Accommodation::getName)
                .containsExactlyInAnyOrder("호텔 A", "호텔 D");
        assertThat(result).allMatch(acc -> acc.getPrice().doubleValue() <= maxPrice);
    }

    @Test
    @DisplayName("가격 범위 필터링 - minPrice와 maxPrice 모두 null")
    void 가격_범위_필터링_모두_null() {
        // given
        Double minPrice = null;
        Double maxPrice = null;

        // when
        List<Accommodation> result = domainService.filterByPriceRange(accommodations, minPrice, maxPrice);

        // then
        assertThat(result).hasSize(4);
        assertThat(result).containsExactlyElementsOf(accommodations);
    }

    @Test
    @DisplayName("평점 필터링 - minRating으로 필터링")
    void 평점_필터링_성공() {
        // given
        Double minRating = 4.5;

        // when
        List<Accommodation> result = domainService.filterByRating(accommodations, minRating);

        // then
        assertThat(result).hasSize(2);
        assertThat(result).extracting(Accommodation::getName)
                .containsExactlyInAnyOrder("호텔 A", "호텔 C");
        assertThat(result).allMatch(acc -> acc.getRating() != null && acc.getRating() >= minRating);
    }

    @Test
    @DisplayName("평점 필터링 - rating이 null인 경우 제외")
    void 평점_필터링_rating_null_제외() {
        // given
        accommodations.add(Accommodation.builder()
                .id("5")
                .name("호텔 E")
                .price(new BigDecimal("120000"))
                .rating(null)
                .build());
        Double minRating = 4.0;

        // when
        List<Accommodation> result = domainService.filterByRating(accommodations, minRating);

        // then
        assertThat(result).hasSize(3);
        assertThat(result).extracting(Accommodation::getName)
                .doesNotContain("호텔 E");
    }

    @Test
    @DisplayName("날짜 범위 검증 - 정상 케이스")
    void 날짜_범위_검증_정상() {
        // given
        LocalDate checkIn = LocalDate.now().plusDays(1);
        LocalDate checkOut = LocalDate.now().plusDays(3);

        // when
        boolean result = domainService.isValidDateRange(checkIn, checkOut);

        // then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("날짜 범위 검증 - checkIn이 checkOut보다 늦은 경우")
    void 날짜_범위_검증_checkIn이_늦은_경우() {
        // given
        LocalDate checkIn = LocalDate.now().plusDays(3);
        LocalDate checkOut = LocalDate.now().plusDays(1);

        // when
        boolean result = domainService.isValidDateRange(checkIn, checkOut);

        // then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("날짜 범위 검증 - checkIn이 오늘보다 이전인 경우")
    void 날짜_범위_검증_checkIn이_과거인_경우() {
        // given
        LocalDate checkIn = LocalDate.now().minusDays(1);
        LocalDate checkOut = LocalDate.now().plusDays(1);

        // when
        boolean result = domainService.isValidDateRange(checkIn, checkOut);

        // then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("날짜 범위 검증 - checkIn이 null인 경우")
    void 날짜_범위_검증_checkIn_null() {
        // given
        LocalDate checkIn = null;
        LocalDate checkOut = LocalDate.now().plusDays(1);

        // when
        boolean result = domainService.isValidDateRange(checkIn, checkOut);

        // then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("날짜 범위 검증 - checkOut이 null인 경우")
    void 날짜_범위_검증_checkOut_null() {
        // given
        LocalDate checkIn = LocalDate.now().plusDays(1);
        LocalDate checkOut = null;

        // when
        boolean result = domainService.isValidDateRange(checkIn, checkOut);

        // then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("날짜 범위 검증 - checkIn과 checkOut이 같은 경우")
    void 날짜_범위_검증_같은_날짜() {
        // given
        LocalDate checkIn = LocalDate.now().plusDays(1);
        LocalDate checkOut = LocalDate.now().plusDays(1);

        // when
        boolean result = domainService.isValidDateRange(checkIn, checkOut);

        // then
        assertThat(result).isFalse();
    }
}

