package com.cookmate.simulator.dto;

public record RecipeStepResponseDto(
        String sessionId,
        int stepNumber,
        boolean completed,
        String status,
        String message
) {
}
