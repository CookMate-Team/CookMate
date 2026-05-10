package com.cookmate.main.exception;

public class MealNotFoundException extends ResourceNotFoundException {

    public MealNotFoundException(String mealId) {
        super(ErrorCode.MEAL_NOT_FOUND, "Meal with id " + mealId + " not found");
    }
}
