package com.cookmate.simulator.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record StartSimulationRequestDto(
        @Schema(description = "Recipe identifier from main-service", example = "52772")
        @NotBlank(message = "recipeId is required")
        String recipeId
) {
}
