package com.cookmate.mealplanner.service;

import com.cookmate.mealplanner.client.MainServiceClient;
import com.cookmate.mealplanner.dto.CategoryResponse;
import com.cookmate.mealplanner.dto.DayPlan;
import com.cookmate.mealplanner.dto.MealItem;
import com.cookmate.mealplanner.dto.MealSearchResponse;
import com.cookmate.mealplanner.dto.WeeklyPlanResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class MealPlanService {

    private static final List<String> DAYS = List.of(
            "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"
    );
    private static final Logger logger = LoggerFactory.getLogger(MealPlanService.class);

    private final MainServiceClient mainServiceClient;
    private final Random random = new Random();

    public WeeklyPlanResponse generateWeeklyPlan(int mealsPerDay) {
        logger.info("Generating weekly plan, mealsPerDay={}", mealsPerDay);

        if (mealsPerDay < 1 || mealsPerDay > 5) {
            throw new IllegalArgumentException("mealsPerDay must be between 1 and 5");
        }

        List<CategoryResponse.Category> categories = mainServiceClient.getCategories().categories();

        if (categories == null || categories.isEmpty()) {
            logger.error("No categories available from main-service");
            throw new IllegalStateException("No categories available from main-service");
        }

        logger.debug("Fetched {} categories from main-service", categories.size());

        List<CategoryResponse.Category> shuffled = new ArrayList<>(categories);
        Collections.shuffle(shuffled, random);

        List<DayPlan> dayPlans = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            String category = shuffled.get(i % shuffled.size()).name();
            dayPlans.add(new DayPlan(DAYS.get(i), pickMeals(category, mealsPerDay)));
        }

        logger.info("Weekly plan generated successfully");
        return new WeeklyPlanResponse(dayPlans);
    }

    private List<MealItem> pickMeals(String category, int count) {
        MealSearchResponse response = mainServiceClient.getMealsByCategory(category);

        if (response == null || response.meals() == null || response.meals().isEmpty()) {
            logger.warn("No meals found for category={}", category);
            return List.of();
        }

        List<MealSearchResponse.FilteredMeal> all = new ArrayList<>(response.meals());
        Collections.shuffle(all, random);

        return all.stream()
                .limit(count)
                .map(m -> new MealItem(m.idMeal(), m.strMeal(), m.strMealThumb()))
                .toList();
    }
}
