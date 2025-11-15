package com.yd.travelbot.domain.accommodation.application.usecase;

import com.yd.travelbot.domain.accommodation.application.dto.AccommodationResponse;
import com.yd.travelbot.domain.accommodation.application.dto.AccommodationSearchRequest;
import com.yd.travelbot.domain.accommodation.domain.entity.Accommodation;
import com.yd.travelbot.domain.accommodation.domain.repository.AccommodationRepository;
import com.yd.travelbot.domain.accommodation.domain.service.AccommodationDomainService;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SearchAccommodationUseCase 테스트")
class SearchAccommodationUseCaseTest {

    @Mock
    private AccommodationRepository accommodationRepository;

    @Mock
    private AccommodationDomainService domainService;

    @InjectMocks
    private SearchAccommodationUseCase useCase;

    private List<Accommodation> accommodations;

    @BeforeEach
    void setUp() {
        accommodations = new ArrayList<>();
        accommodations.add(Accommodation.builder()
                .id("1")
                .name("호텔 A")
                .city("서울")
                .price(new BigDecimal("100000"))
                .rating(4.5)
                .build());
        accommodations.add(Accommodation.builder()
                .id("2")
                .name("호텔 B")
                .city("서울")
                .price(new BigDecimal("150000"))
                .rating(4.0)
                .build());
    }

    @Test
    @DisplayName("정상 검색 - 도시, 날짜, 인원으로 숙소 검색")
    void 정상_검색_성공() {
        // given
        LocalDate checkIn = LocalDate.now().plusDays(1);
        LocalDate checkOut = LocalDate.now().plusDays(3);
        AccommodationSearchRequest request = AccommodationSearchRequest.builder()
                .city("서울")
                .checkIn(checkIn)
                .checkOut(checkOut)
                .guests(2)
                .build();

        when(domainService.isValidDateRange(checkIn, checkOut)).thenReturn(true);
        when(accommodationRepository.search("서울", checkIn, checkOut, 2))
                .thenReturn(accommodations);

        // when
        List<AccommodationResponse> result = useCase.execute(request);

        // then
        assertThat(result).hasSize(2);
        assertThat(result).extracting(AccommodationResponse::getName)
                .containsExactlyInAnyOrder("호텔 A", "호텔 B");
        verify(domainService).isValidDateRange(checkIn, checkOut);
        verify(accommodationRepository).search("서울", checkIn, checkOut, 2);
    }

    @Test
    @DisplayName("날짜 검증 - 유효하지 않은 날짜 범위 시 예외 발생")
    void 날짜_검증_실패() {
        // given
        LocalDate checkIn = LocalDate.now().plusDays(3);
        LocalDate checkOut = LocalDate.now().plusDays(1);
        AccommodationSearchRequest request = AccommodationSearchRequest.builder()
                .city("서울")
                .checkIn(checkIn)
                .checkOut(checkOut)
                .guests(2)
                .build();

        when(domainService.isValidDateRange(checkIn, checkOut)).thenReturn(false);

        // when & then
        assertThatThrownBy(() -> useCase.execute(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("유효하지 않은 날짜 범위");
        verify(accommodationRepository, never()).search(anyString(), any(), any(), anyInt());
    }

    @Test
    @DisplayName("가격 필터링 - minPrice와 maxPrice로 필터링")
    void 가격_필터링_성공() {
        // given
        LocalDate checkIn = LocalDate.now().plusDays(1);
        LocalDate checkOut = LocalDate.now().plusDays(3);
        AccommodationSearchRequest request = AccommodationSearchRequest.builder()
                .city("서울")
                .checkIn(checkIn)
                .checkOut(checkOut)
                .guests(2)
                .minPrice(120000.0)
                .maxPrice(160000.0)
                .build();

        when(domainService.isValidDateRange(checkIn, checkOut)).thenReturn(true);
        when(accommodationRepository.search("서울", checkIn, checkOut, 2))
                .thenReturn(accommodations);
        when(domainService.filterByPriceRange(accommodations, 120000.0, 160000.0))
                .thenReturn(List.of(accommodations.get(1)));

        // when
        List<AccommodationResponse> result = useCase.execute(request);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("호텔 B");
        verify(domainService).filterByPriceRange(accommodations, 120000.0, 160000.0);
    }

    @Test
    @DisplayName("평점 필터링 - minRating으로 필터링")
    void 평점_필터링_성공() {
        // given
        LocalDate checkIn = LocalDate.now().plusDays(1);
        LocalDate checkOut = LocalDate.now().plusDays(3);
        AccommodationSearchRequest request = AccommodationSearchRequest.builder()
                .city("서울")
                .checkIn(checkIn)
                .checkOut(checkOut)
                .guests(2)
                .minRating(4.5)
                .build();

        when(domainService.isValidDateRange(checkIn, checkOut)).thenReturn(true);
        when(accommodationRepository.search("서울", checkIn, checkOut, 2))
                .thenReturn(accommodations);
        when(domainService.filterByRating(accommodations, 4.5))
                .thenReturn(List.of(accommodations.get(0)));

        // when
        List<AccommodationResponse> result = useCase.execute(request);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("호텔 A");
        verify(domainService).filterByRating(accommodations, 4.5);
    }

    @Test
    @DisplayName("기본 인원 - guests가 null일 때 기본값 1 사용")
    void 기본_인원_사용() {
        // given
        LocalDate checkIn = LocalDate.now().plusDays(1);
        LocalDate checkOut = LocalDate.now().plusDays(3);
        AccommodationSearchRequest request = AccommodationSearchRequest.builder()
                .city("서울")
                .checkIn(checkIn)
                .checkOut(checkOut)
                .guests(null)
                .build();

        when(domainService.isValidDateRange(checkIn, checkOut)).thenReturn(true);
        when(accommodationRepository.search("서울", checkIn, checkOut, 1))
                .thenReturn(accommodations);

        // when
        List<AccommodationResponse> result = useCase.execute(request);

        // then
        assertThat(result).hasSize(2);
        verify(accommodationRepository).search("서울", checkIn, checkOut, 1);
    }

    @Test
    @DisplayName("빈 결과 - 검색 결과가 없을 때 빈 리스트 반환")
    void 빈_결과_반환() {
        // given
        LocalDate checkIn = LocalDate.now().plusDays(1);
        LocalDate checkOut = LocalDate.now().plusDays(3);
        AccommodationSearchRequest request = AccommodationSearchRequest.builder()
                .city("서울")
                .checkIn(checkIn)
                .checkOut(checkOut)
                .guests(2)
                .build();

        when(domainService.isValidDateRange(checkIn, checkOut)).thenReturn(true);
        when(accommodationRepository.search("서울", checkIn, checkOut, 2))
                .thenReturn(new ArrayList<>());

        // when
        List<AccommodationResponse> result = useCase.execute(request);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("가격과 평점 필터링 동시 적용")
    void 가격과_평점_필터링_동시_적용() {
        // given
        LocalDate checkIn = LocalDate.now().plusDays(1);
        LocalDate checkOut = LocalDate.now().plusDays(3);
        AccommodationSearchRequest request = AccommodationSearchRequest.builder()
                .city("서울")
                .checkIn(checkIn)
                .checkOut(checkOut)
                .guests(2)
                .minPrice(100000.0)
                .maxPrice(150000.0)
                .minRating(4.0)
                .build();

        when(domainService.isValidDateRange(checkIn, checkOut)).thenReturn(true);
        when(accommodationRepository.search("서울", checkIn, checkOut, 2))
                .thenReturn(accommodations);
        when(domainService.filterByPriceRange(accommodations, 100000.0, 150000.0))
                .thenReturn(accommodations);
        when(domainService.filterByRating(accommodations, 4.0))
                .thenReturn(accommodations);

        // when
        List<AccommodationResponse> result = useCase.execute(request);

        // then
        assertThat(result).hasSize(2);
        verify(domainService).filterByPriceRange(accommodations, 100000.0, 150000.0);
        verify(domainService).filterByRating(accommodations, 4.0);
    }
}

