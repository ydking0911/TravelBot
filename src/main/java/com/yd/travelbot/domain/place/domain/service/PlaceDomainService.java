package com.yd.travelbot.domain.place.domain.service;

import com.yd.travelbot.domain.place.domain.entity.Place;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlaceDomainService {

    public List<Place> filterByCategory(List<Place> places, String category) {
        if (category == null || category.isEmpty()) {
            return places;
        }
        return places.stream()
                .filter(place -> place.getCategory() != null && 
                               place.getCategory().toLowerCase().contains(category.toLowerCase()))
                .collect(Collectors.toList());
    }

    public List<Place> filterByRating(List<Place> places, Double minRating) {
        return places.stream()
                .filter(place -> place.getRating() != null && place.getRating() >= minRating)
                .collect(Collectors.toList());
    }
}

