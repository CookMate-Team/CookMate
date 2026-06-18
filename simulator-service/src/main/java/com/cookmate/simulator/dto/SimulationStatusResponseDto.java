package com.cookmate.simulator.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record SimulationStatusResponseDto(
        @Schema(example = "550e8400-e29b-41d4-a716-446655440000")
        String sessionId,
        @Schema(example = "RUNNING")
        String status,
        @Schema(example = "2")
        int currentStep,
        @Schema(example = "8")
        int totalSteps,
        @Schema(example = "1")
        int totalRecipes,
        String message,
        List<SimulationStepHistoryItemDto> history,
        @Schema(example = "4")
        Integer targetPortions
) {
}
