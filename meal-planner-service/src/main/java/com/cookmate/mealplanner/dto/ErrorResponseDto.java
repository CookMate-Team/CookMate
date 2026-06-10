package com.cookmate.mealplanner.dto;

public record ErrorResponseDto(
        String code,
        String message
) {
}
