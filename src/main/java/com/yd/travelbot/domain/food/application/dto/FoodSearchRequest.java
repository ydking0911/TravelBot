package com.yd.travelbot.domain.food.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FoodSearchRequest {
    private String city;
    private String cuisine;
    private Double latitude;
    private Double longitude;
    private Double radius;
    private Double minRating;
    private BigDecimal maxPrice;
}

