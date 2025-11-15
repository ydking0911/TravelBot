package com.yd.travelbot.domain.place.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlaceSearchRequest {
    private String city;
    private String category;
    private Double latitude;
    private Double longitude;
    private Double radius;
    private Double minRating;
}

