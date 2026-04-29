package com.cookmate.main.dto;

import com.cookmate.main.model.ActionType;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.Map;

/**
 * DTO representing a single cooking step returned from LLM.
 * Used for parsing the LLM response and creating Step entities.
 */
public record LLMStepDTO(
    /**
     * Step number in the cooking process (must be positive).
     */
    @NotNull(message = "Step number is required")
    @Positive(message = "Step number must be positive")
    @JsonProperty("step_number")
    Integer stepNumber,

    /**
     * Description of what to do in this step (required, non-blank).
     */
    @NotBlank(message = "Step description cannot be blank")
    String description,

    /**
     * Type of cooking action for this step.
     */
    @NotNull(message = "Action type is required")
    ActionType action,

    /**
     * Main ingredient used in this step (optional).
     */
    @JsonProperty("main_ingredient")
    String mainIngredient,

    /**
     * Duration of this step in minutes (optional).
     */
    @JsonProperty("duration_minutes")
    Integer duration,

    /**
     * Additional parameters (optional).
     */
    Map<String, Object> parameters
) {}
