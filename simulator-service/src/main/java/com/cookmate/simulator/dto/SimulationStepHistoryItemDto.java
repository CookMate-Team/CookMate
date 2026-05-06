package com.cookmate.simulator.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

public record SimulationStepHistoryItemDto(
        @Schema(example = "1")
        int stepNumber,
        Long recipeId,
        @Schema(example = "Chop onion")
        String recipeName,
        @Schema(example = "3 minutes")
        String preparationTime,
        @Schema(example = "EXECUTED")
        String status,
        LocalDateTime executedAt
) {
}
