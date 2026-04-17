package com.cookmate.simulator.dto;

public record MealPlanItemDto(
        int day,
        Long recipeId,
        String recipeName,
        String preparationTime
) {
}
