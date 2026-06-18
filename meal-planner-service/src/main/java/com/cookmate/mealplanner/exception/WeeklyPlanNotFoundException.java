package com.cookmate.mealplanner.exception;

public class WeeklyPlanNotFoundException extends RuntimeException {

    public WeeklyPlanNotFoundException(String message) {
        super(message);
    }
}
