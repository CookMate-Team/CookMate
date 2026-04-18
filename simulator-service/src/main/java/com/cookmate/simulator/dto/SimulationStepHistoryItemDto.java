package com.cookmate.simulator.dto;

import java.time.LocalDateTime;

public record SimulationStepHistoryItemDto(
        int stepNumber,
        Long recipeId,
        String recipeName,
        String preparationTime,
        String status,
        LocalDateTime executedAt
) {
}
