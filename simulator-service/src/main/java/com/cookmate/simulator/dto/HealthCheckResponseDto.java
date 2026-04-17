package com.cookmate.simulator.dto;

public record HealthCheckResponseDto(
        String status,
        String mainService,
        String recipeCount,
        String error
) {
}
