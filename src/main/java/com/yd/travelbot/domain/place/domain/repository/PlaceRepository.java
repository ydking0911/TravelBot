package com.yd.travelbot.domain.place.domain.repository;

import com.yd.travelbot.domain.place.domain.entity.Place;

import java.util.List;

public interface PlaceRepository {
    List<Place> search(String city, String category);
    List<Place> searchNearby(Double latitude, Double longitude, Double radius);
}

