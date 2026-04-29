package com.cookmate.main.dto;

import java.util.List;

/**
 * Response DTO for step generation endpoint.
 * Contains the generated cooking steps and recipe metadata.
 */
public record StepGenerationResponse(
        /**
         * Recipe ID (TheMealDB meal ID).
         */
        String recipeId,

        /**
         * Recipe name from theMealDB.
         */
        String recipeName,

        /**
         * List of generated cooking steps.
         */
        List<StepDTO> steps
) {}
