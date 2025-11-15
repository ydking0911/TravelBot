package com.yd.travelbot.domain.accommodation.application.dto;

import com.yd.travelbot.domain.accommodation.domain.entity.Accommodation;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccommodationResponse {
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

    public static AccommodationResponse from(Accommodation accommodation) {
        return AccommodationResponse.builder()
                .id(accommodation.getId())
                .name(accommodation.getName())
                .address(accommodation.getAddress())
                .city(accommodation.getCity())
                .country(accommodation.getCountry())
                .price(accommodation.getPrice())
                .currency(accommodation.getCurrency())
                .rating(accommodation.getRating())
                .description(accommodation.getDescription())
                .imageUrl(accommodation.getImageUrl())
                .checkIn(accommodation.getCheckIn())
                .checkOut(accommodation.getCheckOut())
                .guests(accommodation.getGuests())
                .build();
    }
}

