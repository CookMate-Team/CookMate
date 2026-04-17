package com.cookmate.simulator.dto;

import java.util.List;

public record MealPlanResponseDto(
        int days,
        int totalRecipes,
        String message,
        List<MealPlanItemDto> plan
) {
}
