package com.cookmate.mealplanner.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record MealDetailListResponse(
        @JsonProperty("meals") List<MealDetailResponse> meals
) {}
