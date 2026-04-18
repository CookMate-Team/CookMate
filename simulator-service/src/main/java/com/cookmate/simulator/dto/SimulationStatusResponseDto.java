package com.cookmate.simulator.dto;

import java.util.List;

public record SimulationStatusResponseDto(
        String sessionId,
        String status,
        int currentStep,
        int totalSteps,
        int totalRecipes,
        String message,
        List<SimulationStepHistoryItemDto> history
) {
}
