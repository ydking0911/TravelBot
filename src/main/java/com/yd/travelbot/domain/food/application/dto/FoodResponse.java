package com.yd.travelbot.domain.food.application.dto;

import com.yd.travelbot.domain.food.domain.entity.Food;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FoodResponse {
    private String id;
    private String name;
    private String address;
    private String city;
    private String country;
    private String cuisine;
    private BigDecimal priceRange;
    private Double rating;
    private String description;
    private String imageUrl;
    private Double latitude;
    private Double longitude;

    public static FoodResponse from(Food food) {
        return FoodResponse.builder()
                .id(food.getId())
                .name(food.getName())
                .address(food.getAddress())
                .city(food.getCity())
                .country(food.getCountry())
                .cuisine(food.getCuisine())
                .priceRange(food.getPriceRange())
                .rating(food.getRating())
                .description(food.getDescription())
                .imageUrl(food.getImageUrl())
                .latitude(food.getLatitude())
                .longitude(food.getLongitude())
                .build();
    }
}

