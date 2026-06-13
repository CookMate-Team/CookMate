package com.cookmate.main.dto;

import jakarta.validation.constraints.NotBlank;

public record FavoriteRecipeAddRequest(
    @NotBlank(message = "Recipe title cannot be blank")
    String recipeTitle,
    String imageUrl
) {}
