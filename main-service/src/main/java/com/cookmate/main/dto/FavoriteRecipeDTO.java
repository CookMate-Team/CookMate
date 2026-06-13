package com.cookmate.main.dto;

import java.time.LocalDateTime;

public record FavoriteRecipeDTO(
    Long id,
    String recipeId,
    String recipeTitle,
    String imageUrl,
    LocalDateTime addedAt
) {}
