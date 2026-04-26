package com.cookmate.main.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for generating steps from a TheMealDB meal.
 * Contains the meal ID to fetch and generate cooking steps for.
 */
public record StepGenerationRequest(
        /**
         * TheMealDB meal ID (required, non-blank).
         */
        @NotBlank(message = "Meal ID cannot be blank")
        String mealId
) {}
