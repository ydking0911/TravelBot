package com.yd.travelbot.domain.accommodation.domain.service;

import com.yd.travelbot.domain.accommodation.domain.entity.Accommodation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccommodationDomainService {

    public List<Accommodation> filterByPriceRange(List<Accommodation> accommodations, Double minPrice, Double maxPrice) {
        return accommodations.stream()
                .filter(acc -> {
                    double price = acc.getPrice().doubleValue();
                    return (minPrice == null || price >= minPrice) && (maxPrice == null || price <= maxPrice);
                })
                .collect(Collectors.toList());
    }

    public List<Accommodation> filterByRating(List<Accommodation> accommodations, Double minRating) {
        return accommodations.stream()
                .filter(acc -> acc.getRating() != null && acc.getRating() >= minRating)
                .collect(Collectors.toList());
    }

    public boolean isValidDateRange(LocalDate checkIn, LocalDate checkOut) {
        return checkIn != null && checkOut != null && 
               !checkIn.isBefore(LocalDate.now()) && 
               checkOut.isAfter(checkIn);
    }
}

