package com.yd.travelbot.domain.place.domain.entity;

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
public class Place extends BaseEntity {
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
}

