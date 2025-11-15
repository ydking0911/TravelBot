package com.yd.travelbot.domain.place.application.usecase;

import com.yd.travelbot.domain.place.application.dto.PlaceResponse;
import com.yd.travelbot.domain.place.application.dto.PlaceSearchRequest;
import com.yd.travelbot.domain.place.domain.entity.Place;
import com.yd.travelbot.domain.place.domain.repository.PlaceRepository;
import com.yd.travelbot.domain.place.domain.service.PlaceDomainService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SearchPlaceUseCase 테스트")
class SearchPlaceUseCaseTest {

    @Mock
    private PlaceRepository placeRepository;

    @Mock
    private PlaceDomainService domainService;

    @InjectMocks
    private SearchPlaceUseCase useCase;

    private List<Place> places;

    @BeforeEach
    void setUp() {
        places = new ArrayList<>();
        places.add(Place.builder()
                .id("1")
                .name("관광지 A")
                .city("서울")
                .category("박물관")
                .rating(4.5)
                .build());
        places.add(Place.builder()
                .id("2")
                .name("관광지 B")
                .city("서울")
                .category("공원")
                .rating(4.0)
                .build());
    }

    @Test
    @DisplayName("정상 검색 - 도시, 카테고리로 관광지 검색")
    void 정상_검색_성공() {
        // given
        PlaceSearchRequest request = PlaceSearchRequest.builder()
                .city("서울")
                .category("박물관")
                .build();

        when(placeRepository.search("서울", "박물관"))
                .thenReturn(places);
        // category가 null이 아니면 filterByCategory가 호출됨
        when(domainService.filterByCategory(places, "박물관"))
                .thenReturn(places);

        // when
        List<PlaceResponse> result = useCase.execute(request);

        // then
        assertThat(result).hasSize(2);
        assertThat(result).extracting(PlaceResponse::getName)
                .containsExactlyInAnyOrder("관광지 A", "관광지 B");
        verify(placeRepository).search("서울", "박물관");
        verify(domainService).filterByCategory(places, "박물관");
    }

    @Test
    @DisplayName("좌표 기반 검색 - latitude, longitude로 주변 검색")
    void 좌표_기반_검색_성공() {
        // given
        Double latitude = 37.5665;
        Double longitude = 126.9780;
        Double radius = 5.0;
        PlaceSearchRequest request = PlaceSearchRequest.builder()
                .latitude(latitude)
                .longitude(longitude)
                .radius(radius)
                .build();

        when(placeRepository.searchNearby(latitude, longitude, radius))
                .thenReturn(places);

        // when
        List<PlaceResponse> result = useCase.execute(request);

        // then
        assertThat(result).hasSize(2);
        verify(placeRepository).searchNearby(latitude, longitude, radius);
        verify(placeRepository, never()).search(anyString(), anyString());
    }

    @Test
    @DisplayName("반경 설정 - radius가 null일 때 기본값 5.0 사용")
    void 반경_설정_기본값_사용() {
        // given
        Double latitude = 37.5665;
        Double longitude = 126.9780;
        PlaceSearchRequest request = PlaceSearchRequest.builder()
                .latitude(latitude)
                .longitude(longitude)
                .radius(null)
                .build();

        when(placeRepository.searchNearby(latitude, longitude, 5.0))
                .thenReturn(places);

        // when
        List<PlaceResponse> result = useCase.execute(request);

        // then
        assertThat(result).hasSize(2);
        verify(placeRepository).searchNearby(latitude, longitude, 5.0);
    }

    @Test
    @DisplayName("카테고리 필터링 - category로 필터링")
    void 카테고리_필터링_성공() {
        // given
        PlaceSearchRequest request = PlaceSearchRequest.builder()
                .city("서울")
                .category("박물관")
                .build();

        when(placeRepository.search("서울", "박물관"))
                .thenReturn(places);
        when(domainService.filterByCategory(places, "박물관"))
                .thenReturn(List.of(places.get(0)));

        // when
        List<PlaceResponse> result = useCase.execute(request);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("관광지 A");
        verify(domainService).filterByCategory(places, "박물관");
    }

    @Test
    @DisplayName("평점 필터링 - minRating으로 필터링")
    void 평점_필터링_성공() {
        // given
        PlaceSearchRequest request = PlaceSearchRequest.builder()
                .city("서울")
                .category("박물관")
                .minRating(4.5)
                .build();

        when(placeRepository.search("서울", "박물관"))
                .thenReturn(places);
        // category가 null이 아니면 filterByCategory가 먼저 호출됨
        when(domainService.filterByCategory(places, "박물관"))
                .thenReturn(places);
        when(domainService.filterByRating(places, 4.5))
                .thenReturn(List.of(places.get(0)));

        // when
        List<PlaceResponse> result = useCase.execute(request);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("관광지 A");
        verify(domainService).filterByCategory(places, "박물관");
        verify(domainService).filterByRating(places, 4.5);
    }

    @Test
    @DisplayName("카테고리와 평점 필터링 동시 적용")
    void 카테고리와_평점_필터링_동시_적용() {
        // given
        String category = "박물관";
        Double minRating = 4.5;
        PlaceSearchRequest request = PlaceSearchRequest.builder()
                .city("서울")
                .category(category)
                .minRating(minRating)
                .build();

        when(placeRepository.search("서울", category))
                .thenReturn(places);
        when(domainService.filterByCategory(places, category))
                .thenReturn(places);
        when(domainService.filterByRating(places, minRating))
                .thenReturn(List.of(places.get(0)));

        // when
        List<PlaceResponse> result = useCase.execute(request);

        // then
        assertThat(result).hasSize(1);
        verify(domainService).filterByCategory(places, category);
        verify(domainService).filterByRating(places, minRating);
    }

    @Test
    @DisplayName("빈 결과 - 검색 결과가 없을 때 빈 리스트 반환")
    void 빈_결과_반환() {
        // given
        PlaceSearchRequest request = PlaceSearchRequest.builder()
                .city("서울")
                .category("박물관")
                .build();

        when(placeRepository.search("서울", "박물관"))
                .thenReturn(new ArrayList<>());

        // when
        List<PlaceResponse> result = useCase.execute(request);

        // then
        assertThat(result).isEmpty();
    }
}

