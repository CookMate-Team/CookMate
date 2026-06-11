package com.cookmate.mealplanner.dto;

import java.util.List;

public record DayPlan(String day, List<MealItem> meals) {}
