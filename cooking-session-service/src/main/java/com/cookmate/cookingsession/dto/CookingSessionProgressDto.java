package com.cookmate.cookingsession.dto;

import java.time.LocalDateTime;

public record CookingSessionProgressDto(
        String sessionId,
        String recipeId,
        Integer stepNumber,
        String status,
        LocalDateTime executedAt
) {}
