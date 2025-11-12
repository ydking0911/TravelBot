package com.yd.travelbot.domain.accommodation.domain.repository;

import com.yd.travelbot.domain.accommodation.domain.entity.Accommodation;

import java.time.LocalDate;
import java.util.List;

public interface AccommodationRepository {
    List<Accommodation> search(String city, LocalDate checkIn, LocalDate checkOut, Integer guests);
    Accommodation findById(String id);
}

