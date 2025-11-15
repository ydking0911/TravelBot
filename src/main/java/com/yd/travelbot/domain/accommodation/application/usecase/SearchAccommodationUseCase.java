package com.yd.travelbot.domain.accommodation.application.usecase;

import com.yd.travelbot.domain.accommodation.application.dto.AccommodationResponse;
import com.yd.travelbot.domain.accommodation.application.dto.AccommodationSearchRequest;
import com.yd.travelbot.domain.accommodation.domain.entity.Accommodation;
import com.yd.travelbot.domain.accommodation.domain.repository.AccommodationRepository;
import com.yd.travelbot.domain.accommodation.domain.service.AccommodationDomainService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SearchAccommodationUseCase {

    private final AccommodationRepository accommodationRepository;
    private final AccommodationDomainService domainService;

    public List<AccommodationResponse> execute(AccommodationSearchRequest request) {
        if (!domainService.isValidDateRange(request.getCheckIn(), request.getCheckOut())) {
            throw new IllegalArgumentException("유효하지 않은 날짜 범위입니다.");
        }

        List<Accommodation> accommodations = accommodationRepository.search(
                request.getCity(),
                request.getCheckIn(),
                request.getCheckOut(),
                request.getGuests() != null ? request.getGuests() : 1
        );

        if (request.getMinPrice() != null || request.getMaxPrice() != null) {
            accommodations = domainService.filterByPriceRange(
                    accommodations,
                    request.getMinPrice(),
                    request.getMaxPrice()
            );
        }

        if (request.getMinRating() != null) {
            accommodations = domainService.filterByRating(accommodations, request.getMinRating());
        }

        return accommodations.stream()
                .map(AccommodationResponse::from)
                .collect(Collectors.toList());
    }
}

