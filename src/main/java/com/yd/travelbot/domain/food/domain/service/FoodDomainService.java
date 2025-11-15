package com.yd.travelbot.domain.food.domain.service;

import com.yd.travelbot.domain.food.domain.entity.Food;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FoodDomainService {

    public List<Food> filterByRating(List<Food> foods, Double minRating) {
        return foods.stream()
                .filter(food -> food.getRating() != null && food.getRating() >= minRating)
                .collect(Collectors.toList());
    }

    public List<Food> filterByPriceRange(List<Food> foods, BigDecimal maxPrice) {
        return foods.stream()
                .filter(food -> food.getPriceRange() != null && 
                               food.getPriceRange().compareTo(maxPrice) <= 0)
                .collect(Collectors.toList());
    }
}

