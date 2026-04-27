package com.cookmate.main.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**
 * DTO representing the complete LLM response containing generated cooking steps.
 * Wraps a list of LLMStepDTO objects returned from the LLM service.
 */
public record LLMResponseDTO(
    /**
     * List of generated cooking steps from LLM.
     */
    @NotEmpty(message = "Steps list cannot be empty")
    @JsonProperty("steps")
    List<LLMStepDTO> steps
) {}
