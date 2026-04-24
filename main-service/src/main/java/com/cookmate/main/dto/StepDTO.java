package com.cookmate.main.dto;

import com.cookmate.main.model.ActionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record StepDTO(
        Long id,

        @NotNull(message = "Numer kroku jest wymagany")
        @Positive(message = "Numer kroku musi być liczbą dodatnią")
        Integer stepNumber,

        @NotBlank(message = "Opis kroku nie może być pusty")
        String description,

        @NotNull(message = "Typ akcji jest wymagany")
        ActionType action,

        String parameters,

        @PositiveOrZero(message = "Czas trwania nie może być wartością ujemną")
        Integer duration,

        @NotBlank(message = "ID przepisu jest wymagane")
        String recipeId,

        LocalDateTime createdAt
) {
}