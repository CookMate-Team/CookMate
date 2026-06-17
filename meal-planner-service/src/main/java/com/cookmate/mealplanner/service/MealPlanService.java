package com.cookmate.mealplanner.service;

import com.cookmate.mealplanner.client.MainServiceClient;
import com.cookmate.mealplanner.dto.CategoryResponse;
import com.cookmate.mealplanner.dto.DayPlan;
import com.cookmate.mealplanner.dto.MealItem;
import com.cookmate.mealplanner.dto.MealSearchResponse;
import com.cookmate.mealplanner.dto.WeeklyPlanResponse;
import com.cookmate.mealplanner.exception.MainServiceCommunicationException;
import com.cookmate.mealplanner.exception.MealPlanGenerationException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MealPlanService {

    private enum Slot { BREAKFAST, MAIN, STARTER, SIDE, DESSERT }

    private static final List<String> DAYS = List.of(
            "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"
    );

    private static final Set<String> FIXED_CATEGORIES = Set.of("Breakfast", "Dessert", "Starter", "Side");

    private static final Map<Slot, String> SLOT_TO_CATEGORY = Map.of(
            Slot.BREAKFAST, "Breakfast",
            Slot.DESSERT,   "Dessert",
            Slot.STARTER,   "Starter",
            Slot.SIDE,      "Side"
    );

    private static final Map<Integer, List<Slot>> SLOTS_BY_MEALS_PER_DAY = Map.of(
            1, List.of(Slot.MAIN),
            2, List.of(Slot.BREAKFAST, Slot.MAIN),
            3, List.of(Slot.BREAKFAST, Slot.MAIN, Slot.STARTER),
            4, List.of(Slot.BREAKFAST, Slot.MAIN, Slot.SIDE, Slot.DESSERT),
            5, List.of(Slot.BREAKFAST, Slot.MAIN, Slot.STARTER, Slot.SIDE, Slot.DESSERT)
    );

    private static final Logger logger = LoggerFactory.getLogger(MealPlanService.class);

    private final MainServiceClient mainServiceClient;
    private final Random random = new Random();

    public WeeklyPlanResponse generateWeeklyPlan(int mealsPerDay) {
        logger.info("Generating weekly plan, mealsPerDay={}", mealsPerDay);

        if (mealsPerDay < 1 || mealsPerDay > 5) {
            throw new IllegalArgumentException("mealsPerDay must be between 1 and 5");
        }

        List<CategoryResponse.Category> categories;
        try {
            categories = mainServiceClient.getCategories().categories();
        } catch (Exception e) {
            logger.error("Failed to fetch categories from main-service", e);
            throw new MainServiceCommunicationException("Failed to fetch categories from main-service", e);
        }

        if (categories == null || categories.isEmpty()) {
            logger.error("No categories available from main-service");
            throw new MealPlanGenerationException("No categories available from main-service");
        }

        logger.debug("Fetched {} categories from main-service", categories.size());

        List<String> mainCategories = categories.stream()
                .map(CategoryResponse.Category::name)
                .filter(name -> !FIXED_CATEGORIES.contains(name))
                .collect(Collectors.toCollection(ArrayList::new));

        if (mainCategories.isEmpty()) {
            logger.error("No main course categories available from main-service");
            throw new MealPlanGenerationException("No main course categories available from main-service");
        }

        List<Slot> slots = SLOTS_BY_MEALS_PER_DAY.get(mealsPerDay);

        List<DayPlan> dayPlans = new ArrayList<>();
        for (String day : DAYS) {
            Collections.shuffle(mainCategories, random);
            List<MealItem> meals = new ArrayList<>();
            for (Slot slot : slots) {
                String category = slot == Slot.MAIN ? mainCategories.get(0) : SLOT_TO_CATEGORY.get(slot);
                MealItem meal = pickOneMeal(category);
                if (meal != null) {
                    meals.add(meal);
                }
            }
            dayPlans.add(new DayPlan(day, meals));
        }

        logger.info("Weekly plan generated successfully");
        return new WeeklyPlanResponse(dayPlans);
    }

    private MealItem pickOneMeal(String category) {
        MealSearchResponse response;
        try {
            response = mainServiceClient.getMealsByCategory(category);
        } catch (Exception e) {
            logger.error("Failed to fetch meals for category={} from main-service", category, e);
            throw new MainServiceCommunicationException(
                    "Failed to fetch meals for category: " + category, e);
        }

        if (response == null || response.meals() == null || response.meals().isEmpty()) {
            logger.warn("No meals found for category={}", category);
            return null;
        }

        List<MealSearchResponse.FilteredMeal> all = new ArrayList<>(response.meals());
        MealSearchResponse.FilteredMeal picked = all.get(random.nextInt(all.size()));
        return new MealItem(picked.idMeal(), picked.strMeal(), picked.strMealThumb());
    }
}
