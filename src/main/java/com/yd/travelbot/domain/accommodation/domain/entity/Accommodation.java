package com.yd.travelbot.domain.accommodation.domain.entity;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.yd.travelbot.global.common.BaseEntity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Accommodation extends BaseEntity {
    private String id;
    private String name;
    private String address;
    private String city;
    private String country;
    private BigDecimal price;
    private String currency;
    private Double rating;
    private String description;
    private String imageUrl;
    private LocalDate checkIn;
    private LocalDate checkOut;
    private Integer guests;
}

