package com.cookmate.mealplanner.client;

import com.cookmate.mealplanner.dto.CategoryResponse;
import com.cookmate.mealplanner.dto.MealSearchResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(
        name = "main-service",
        url = "${meal-planner-service.main-service-url:http://main-service:8081}"
)
public interface MainServiceClient {

    @GetMapping("/api/v1/discovery/categories")
    CategoryResponse getCategories();

    @GetMapping("/api/v1/discovery/filter/category")
    MealSearchResponse getMealsByCategory(@RequestParam("c") String category);
}
