package com.cookmate.main.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for updating an existing recipe.
 * All fields are optional - only provided fields will be updated.
 */
public record RecipeUpdateRequest(
    /**
     * Recipe name (optional, non-blank if provided).
     */
    @NotBlank(message = "Recipe name cannot be blank")
    String name,

    /**
     * Recipe description (optional).
     */
    String description,

    /**
     * List of ingredients (optional, non-blank if provided).
     */
    @NotBlank(message = "Ingredients cannot be blank")
    String ingredients,

    /**
     * Cooking instructions (optional).
     */
    String instructions,

    /**
     * Preparation time in minutes (optional, non-negative if provided).
     */
    @Min(value = 0, message = "Preparation time must be non-negative")
    Integer preparationTimeMinutes
) {}
