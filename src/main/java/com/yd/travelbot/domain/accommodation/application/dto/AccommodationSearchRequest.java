package com.yd.travelbot.domain.accommodation.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccommodationSearchRequest {
    private String city;
    private LocalDate checkIn;
    private LocalDate checkOut;
    private Integer guests;
    private Double minPrice;
    private Double maxPrice;
    private Double minRating;
}

