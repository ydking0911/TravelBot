package com.yd.travelbot.domain.food.application.usecase;

import com.yd.travelbot.domain.food.application.dto.FoodResponse;
import com.yd.travelbot.domain.food.application.dto.FoodSearchRequest;
import com.yd.travelbot.domain.food.domain.entity.Food;
import com.yd.travelbot.domain.food.domain.repository.FoodRepository;
import com.yd.travelbot.domain.food.domain.service.FoodDomainService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SearchFoodUseCase {

    private final FoodRepository foodRepository;
    private final FoodDomainService domainService;

    public List<FoodResponse> execute(FoodSearchRequest request) {
        List<Food> foods;

        if (request.getLatitude() != null && request.getLongitude() != null) {
            foods = foodRepository.searchNearby(
                    request.getLatitude(),
                    request.getLongitude(),
                    request.getRadius() != null ? request.getRadius() : 5.0
            );
        } else {
            foods = foodRepository.search(request.getCity(), request.getCuisine());
        }

        if (request.getMinRating() != null) {
            foods = domainService.filterByRating(foods, request.getMinRating());
        }

        if (request.getMaxPrice() != null) {
            foods = domainService.filterByPriceRange(foods, request.getMaxPrice());
        }

        return foods.stream()
                .map(FoodResponse::from)
                .collect(Collectors.toList());
    }
}

