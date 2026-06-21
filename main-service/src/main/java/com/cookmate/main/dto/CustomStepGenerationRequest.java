package com.cookmate.main.dto;

import jakarta.validation.constraints.NotBlank;

public record CustomStepGenerationRequest(
    @NotBlank(message = "Instrukcje nie mogą być puste")
    String instructions,

    @NotBlank(message = "Składniki nie mogą być puste")
    String ingredients
) {}
