package com.cookmate.cookingsession.dto;

import java.time.LocalDateTime;

public record ActiveCookingSessionDto(
        String sessionId,
        String recipeId,
        String status,
        Integer currentStep,
        LocalDateTime lastExecutedAt
) {}
