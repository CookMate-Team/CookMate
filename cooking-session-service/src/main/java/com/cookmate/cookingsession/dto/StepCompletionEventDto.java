package com.cookmate.cookingsession.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDateTime;

public record StepCompletionEventDto(
        @NotBlank(message = "sessionId is required")
        String sessionId,

        @NotNull(message = "stepNumber is required")
        @Positive(message = "stepNumber must be positive")
        Integer stepNumber,

        @NotBlank(message = "status is required")
        String status,

        @NotNull(message = "executedAt is required")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime executedAt,

        @NotBlank(message = "recipeId is required")
        String recipeId
) {}
