package com.cookmate.main.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

/**
 * DTO representing a complete recipe with all details.
 * Used for returning recipe information in API responses.
 */
public record RecipeDTO(
    /**
     * Unique recipe identifier.
     */
    Long id,

    /**
     * Recipe name (required, non-blank).
     */
    @NotBlank(message = "Recipe name cannot be blank")
    String name,

    /**
     * Recipe description.
     */
    String description,

    /**
     * List of ingredients (required, non-blank).
     */
    @NotBlank(message = "Ingredients cannot be blank")
    String ingredients,

    /**
     * Cooking instructions.
     */
    String instructions,

    /**
     * Preparation time in minutes (non-negative).
     */
    @Min(value = 0, message = "Preparation time must be non-negative")
    Integer preparationTimeMinutes,

    /**
     * Timestamp when the recipe was created.
     */
    LocalDateTime createdAt
) {}
