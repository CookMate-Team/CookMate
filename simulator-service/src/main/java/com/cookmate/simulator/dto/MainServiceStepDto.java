package com.cookmate.simulator.dto;

public record MainServiceStepDto(
        Long id,
        Integer stepNumber,
        String description,
        String parameters,
        Integer durationMinutes,
        String recipeId
) {
}
