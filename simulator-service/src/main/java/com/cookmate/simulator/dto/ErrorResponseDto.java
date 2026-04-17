package com.cookmate.simulator.dto;

public record ErrorResponseDto(
        String code,
        String message
) {
}
