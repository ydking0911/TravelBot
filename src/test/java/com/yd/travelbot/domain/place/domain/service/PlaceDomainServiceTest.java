package com.yd.travelbot.domain.place.domain.service;

import com.yd.travelbot.domain.place.domain.entity.Place;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PlaceDomainService 테스트")
class PlaceDomainServiceTest {

    private PlaceDomainService domainService;
    private List<Place> places;

    @BeforeEach
    void setUp() {
        domainService = new PlaceDomainService();
        
        places = new ArrayList<>();
        places.add(Place.builder()
                .id("1")
                .name("관광지 A")
                .category("박물관")
                .rating(4.5)
                .build());
        places.add(Place.builder()
                .id("2")
                .name("관광지 B")
                .category("공원")
                .rating(4.0)
                .build());
        places.add(Place.builder()
                .id("3")
                .name("관광지 C")
                .category("박물관")
                .rating(4.8)
                .build());
        places.add(Place.builder()
                .id("4")
                .name("관광지 D")
                .category("해변")
                .rating(3.5)
                .build());
    }

    @Test
    @DisplayName("카테고리 필터링 - 정상 케이스")
    void 카테고리_필터링_성공() {
        // given
        String category = "박물관";

        // when
        List<Place> result = domainService.filterByCategory(places, category);

        // then
        assertThat(result).hasSize(2);
        assertThat(result).extracting(Place::getName)
                .containsExactlyInAnyOrder("관광지 A", "관광지 C");
        assertThat(result).allMatch(place -> 
                place.getCategory() != null && 
                place.getCategory().toLowerCase().contains(category.toLowerCase()));
    }

    @Test
    @DisplayName("카테고리 필터링 - 대소문자 무시")
    void 카테고리_필터링_대소문자_무시() {
        // given
        String category = "공원";

        // when
        List<Place> result = domainService.filterByCategory(places, category);

        // then
        assertThat(result).hasSize(1);
        assertThat(result).extracting(Place::getName)
                .containsExactly("관광지 B");
    }

    @Test
    @DisplayName("카테고리 필터링 - category가 null인 경우")
    void 카테고리_필터링_category_null() {
        // given
        String category = null;

        // when
        List<Place> result = domainService.filterByCategory(places, category);

        // then
        assertThat(result).hasSize(4);
        assertThat(result).containsExactlyElementsOf(places);
    }

    @Test
    @DisplayName("카테고리 필터링 - category가 빈 문자열인 경우")
    void 카테고리_필터링_category_빈_문자열() {
        // given
        String category = "";

        // when
        List<Place> result = domainService.filterByCategory(places, category);

        // then
        assertThat(result).hasSize(4);
        assertThat(result).containsExactlyElementsOf(places);
    }

    @Test
    @DisplayName("카테고리 필터링 - 일치하는 카테고리가 없는 경우")
    void 카테고리_필터링_일치하는_카테고리_없음() {
        // given
        String category = "산";

        // when
        List<Place> result = domainService.filterByCategory(places, category);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("평점 필터링 - minRating으로 필터링")
    void 평점_필터링_성공() {
        // given
        Double minRating = 4.5;

        // when
        List<Place> result = domainService.filterByRating(places, minRating);

        // then
        assertThat(result).hasSize(2);
        assertThat(result).extracting(Place::getName)
                .containsExactlyInAnyOrder("관광지 A", "관광지 C");
        assertThat(result).allMatch(place -> place.getRating() != null && place.getRating() >= minRating);
    }

    @Test
    @DisplayName("평점 필터링 - rating이 null인 경우 제외")
    void 평점_필터링_rating_null_제외() {
        // given
        places.add(Place.builder()
                .id("5")
                .name("관광지 E")
                .category("성")
                .rating(null)
                .build());
        Double minRating = 4.0;

        // when
        List<Place> result = domainService.filterByRating(places, minRating);

        // then
        assertThat(result).hasSize(3);
        assertThat(result).extracting(Place::getName)
                .doesNotContain("관광지 E");
    }
}

