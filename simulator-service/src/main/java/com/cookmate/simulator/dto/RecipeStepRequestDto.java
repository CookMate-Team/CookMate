package com.cookmate.simulator.dto;

public record
RecipeStepRequestDto(
        int stepNumber,
        String description,
        int durationSeconds,
        String temperature,
        String weight,
        String additionalNotes
) {
}
