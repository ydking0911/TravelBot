package com.yd.travelbot.domain.place.application.usecase;

import com.yd.travelbot.domain.place.application.dto.PlaceResponse;
import com.yd.travelbot.domain.place.application.dto.PlaceSearchRequest;
import com.yd.travelbot.domain.place.domain.entity.Place;
import com.yd.travelbot.domain.place.domain.repository.PlaceRepository;
import com.yd.travelbot.domain.place.domain.service.PlaceDomainService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SearchPlaceUseCase {

    private final PlaceRepository placeRepository;
    private final PlaceDomainService domainService;

    public List<PlaceResponse> execute(PlaceSearchRequest request) {
        List<Place> places;

        if (request.getLatitude() != null && request.getLongitude() != null) {
            places = placeRepository.searchNearby(
                    request.getLatitude(),
                    request.getLongitude(),
                    request.getRadius() != null ? request.getRadius() : 5.0
            );
        } else {
            places = placeRepository.search(request.getCity(), request.getCategory());
        }

        if (request.getCategory() != null) {
            places = domainService.filterByCategory(places, request.getCategory());
        }

        if (request.getMinRating() != null) {
            places = domainService.filterByRating(places, request.getMinRating());
        }

        return places.stream()
                .map(PlaceResponse::from)
                .collect(Collectors.toList());
    }
}

