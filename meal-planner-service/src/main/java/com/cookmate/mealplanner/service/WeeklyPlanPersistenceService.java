package com.cookmate.mealplanner.service;

import com.cookmate.mealplanner.dto.DayPlan;
import com.cookmate.mealplanner.dto.MealItem;
import com.cookmate.mealplanner.dto.SavedWeeklyPlanResponse;
import com.cookmate.mealplanner.dto.WeeklyPlanResponse;
import com.cookmate.mealplanner.exception.WeeklyPlanNotFoundException;
import com.cookmate.mealplanner.model.WeeklyPlan;
import com.cookmate.mealplanner.model.WeeklyPlanMeal;
import com.cookmate.mealplanner.repository.WeeklyPlanRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WeeklyPlanPersistenceService {

    private static final Logger logger = LoggerFactory.getLogger(WeeklyPlanPersistenceService.class);
    private static final List<String> ORDERED_DAYS = List.of(
            "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"
    );

    private final WeeklyPlanRepository weeklyPlanRepository;

    @Transactional
    public SavedWeeklyPlanResponse save(String userId, WeeklyPlanResponse request) {
        logger.info("Saving weekly plan for userId={}", userId);

        WeeklyPlan plan = new WeeklyPlan();
        plan.setUserId(userId);
        plan.setMealsPerDay(request.days().stream()
                .mapToInt(d -> d.meals().size())
                .max()
                .orElse(0));

        List<WeeklyPlanMeal> meals = new ArrayList<>();
        for (DayPlan day : request.days()) {
            for (MealItem meal : day.meals()) {
                WeeklyPlanMeal entity = new WeeklyPlanMeal();
                entity.setWeeklyPlan(plan);
                entity.setDayName(day.day());
                entity.setMealId(meal.id());
                entity.setMealName(meal.name());
                entity.setThumbnailUrl(meal.thumbnailUrl());
                meals.add(entity);
            }
        }
        plan.setMeals(meals);

        WeeklyPlan saved = weeklyPlanRepository.save(plan);
        logger.info("Weekly plan saved with id={} for userId={}", saved.getId(), userId);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<SavedWeeklyPlanResponse> getHistory(String userId) {
        logger.info("Fetching weekly plan history for userId={}", userId);
        List<WeeklyPlan> plans = weeklyPlanRepository.findByUserIdOrderByCreatedAtDesc(userId);
        logger.info("Found {} weekly plans for userId={}", plans.size(), userId);
        return plans.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<String> getMealIds(UUID weeklyPlanId, String userId) {
        logger.info("Fetching meal ids from weeklyPlanId={} for userId={}", weeklyPlanId, userId);
        WeeklyPlan plan = weeklyPlanRepository.findByIdAndUserId(weeklyPlanId, userId)
                .orElseThrow(() -> new WeeklyPlanNotFoundException(
                        "Weekly plan not found: " + weeklyPlanId));
        return plan.getMeals().stream()
                .map(WeeklyPlanMeal::getMealId)
                .toList();
    }

    private SavedWeeklyPlanResponse toResponse(WeeklyPlan plan) {
        Map<String, List<MealItem>> mealsByDay = new LinkedHashMap<>();
        ORDERED_DAYS.forEach(day -> mealsByDay.put(day, new ArrayList<>()));

        for (WeeklyPlanMeal meal : plan.getMeals()) {
            mealsByDay.computeIfAbsent(meal.getDayName(), k -> new ArrayList<>())
                    .add(new MealItem(meal.getMealId(), meal.getMealName(), meal.getThumbnailUrl()));
        }

        List<DayPlan> days = mealsByDay.entrySet().stream()
                .map(e -> new DayPlan(e.getKey(), e.getValue()))
                .toList();

        return new SavedWeeklyPlanResponse(plan.getId(), plan.getCreatedAt(), plan.getMealsPerDay(), days);
    }
}
