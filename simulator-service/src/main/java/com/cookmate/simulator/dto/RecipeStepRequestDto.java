package com.cookmate.simulator.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record
RecipeStepRequestDto(
        @Schema(description = "Step number to execute", example = "1")
        @NotNull(message = "stepNumber is required")
        @Positive(message = "stepNumber must be positive")
        Integer stepNumber,
        @Schema(description = "Step description", example = "Heat the pan and add oil.")
        @NotBlank(message = "description is required")
        String description,
        @Schema(description = "Execution duration in seconds", example = "5")
        @NotNull(message = "durationSeconds is required")
        @Positive(message = "durationSeconds must be positive")
        Integer durationSeconds,
        String temperature,
        String weight,
        String additionalNotes
) {
}
