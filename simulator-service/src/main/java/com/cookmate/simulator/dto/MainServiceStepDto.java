package com.cookmate.simulator.dto;

import java.util.Map;

public record MainServiceStepDto(
        Long id,
        Integer stepNumber,
        String description,
        Map<String, Object> parameters,
        Integer durationMinutes,
        String recipeId
) {
}

