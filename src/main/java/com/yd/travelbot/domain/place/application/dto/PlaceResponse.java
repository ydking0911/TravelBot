package com.yd.travelbot.domain.place.application.dto;

import com.yd.travelbot.domain.place.domain.entity.Place;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlaceResponse {
    private String id;
    private String name;
    private String address;
    private String city;
    private String country;
    private String category;
    private String description;
    private String imageUrl;
    private Double rating;
    private Double latitude;
    private Double longitude;
    private BigDecimal entranceFee;
    private String currency;

    public static PlaceResponse from(Place place) {
        return PlaceResponse.builder()
                .id(place.getId())
                .name(place.getName())
                .address(place.getAddress())
                .city(place.getCity())
                .country(place.getCountry())
                .category(place.getCategory())
                .description(place.getDescription())
                .imageUrl(place.getImageUrl())
                .rating(place.getRating())
                .latitude(place.getLatitude())
                .longitude(place.getLongitude())
                .entranceFee(place.getEntranceFee())
                .currency(place.getCurrency())
                .build();
    }
}

