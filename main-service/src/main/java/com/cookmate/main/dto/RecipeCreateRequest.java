package com.cookmate.main.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.util.List;

/**
 * Request DTO for creating a new recipe.
 * Contains all required fields for recipe creation.
 */
public record RecipeCreateRequest(
    /**
     * Recipe name (required, non-blank).
     */
    @NotBlank(message = "Recipe name cannot be blank")
    String name,

    /**
     * Recipe description (optional).
     */
    String description,

    /**
     * List of ingredients (required, non-blank).
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
    Integer preparationTimeMinutes,

    /**
     * Default number of portions (optional, defaults to 4 in service).
     */
    @Min(value = 1, message = "Portions must be at least 1")
    Integer defaultPortions,

    /**
     * URL to the recipe's image.
     */
    String imageUrl,

    /**
     * List of cooking steps (optional).
     */
    List<StepDTO> steps
) {}
