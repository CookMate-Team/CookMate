package com.cookmate.mealplanner.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record SavedWeeklyPlanResponse(UUID id, LocalDateTime createdAt, int mealsPerDay, List<DayPlan> days) {}
