package com.cookmate.mealplanner.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record MealSearchResponse(
        @JsonProperty("meals") List<FilteredMeal> meals
) {
    public record FilteredMeal(
            @JsonProperty("idMeal") String idMeal,
            @JsonProperty("strMeal") String strMeal,
            @JsonProperty("strMealThumb") String strMealThumb
    ) {}
}
