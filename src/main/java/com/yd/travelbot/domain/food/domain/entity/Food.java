package com.yd.travelbot.domain.food.domain.entity;

import java.math.BigDecimal;

import com.yd.travelbot.global.common.BaseEntity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Food extends BaseEntity {
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
}

