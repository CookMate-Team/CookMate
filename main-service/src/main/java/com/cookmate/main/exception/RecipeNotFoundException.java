package com.cookmate.main.exception;

public class RecipeNotFoundException extends ResourceNotFoundException {

    public RecipeNotFoundException(Long recipeId) {
        super(ErrorCode.RECIPE_NOT_FOUND, "Recipe with id " + recipeId + " not found");
    }
}
