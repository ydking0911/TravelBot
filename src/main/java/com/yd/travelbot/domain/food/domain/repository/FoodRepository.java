package com.yd.travelbot.domain.food.domain.repository;

import com.yd.travelbot.domain.food.domain.entity.Food;

import java.util.List;

public interface FoodRepository {
    List<Food> search(String city, String cuisine);
    List<Food> searchNearby(Double latitude, Double longitude, Double radius);
}

