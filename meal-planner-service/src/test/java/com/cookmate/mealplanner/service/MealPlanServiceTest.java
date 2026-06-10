package com.cookmate.mealplanner.service;

import com.cookmate.mealplanner.client.MainServiceClient;
import com.cookmate.mealplanner.dto.CategoryResponse;
import com.cookmate.mealplanner.dto.MealSearchResponse;
import com.cookmate.mealplanner.dto.WeeklyPlanResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MealPlanServiceTest {

    @Mock
    private MainServiceClient mainServiceClient;

    @InjectMocks
    private MealPlanService mealPlanService;

    private CategoryResponse mockCategories() {
        return new CategoryResponse(List.of(
                new CategoryResponse.Category("1", "Beef"),
                new CategoryResponse.Category("2", "Chicken"),
                new CategoryResponse.Category("3", "Dessert"),
                new CategoryResponse.Category("4", "Lamb"),
                new CategoryResponse.Category("5", "Miscellaneous"),
                new CategoryResponse.Category("6", "Pasta"),
                new CategoryResponse.Category("7", "Pork"),
                new CategoryResponse.Category("8", "Seafood")
        ));
    }

    private MealSearchResponse mockMeals(int count) {
        List<MealSearchResponse.FilteredMeal> meals = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            meals.add(new MealSearchResponse.FilteredMeal(
                    String.valueOf(i), "Meal " + i, "https://thumb" + i + ".jpg"
            ));
        }
        return new MealSearchResponse(meals);
    }

    @Test
    void shouldReturnExactlySevenDays() {
        when(mainServiceClient.getCategories()).thenReturn(mockCategories());
        when(mainServiceClient.getMealsByCategory(anyString())).thenReturn(mockMeals(10));

        WeeklyPlanResponse result = mealPlanService.generateWeeklyPlan(2);

        assertThat(result.days()).hasSize(7);
    }

    @Test
    void shouldReturnCorrectDayNames() {
        when(mainServiceClient.getCategories()).thenReturn(mockCategories());
        when(mainServiceClient.getMealsByCategory(anyString())).thenReturn(mockMeals(10));

        WeeklyPlanResponse result = mealPlanService.generateWeeklyPlan(1);

        assertThat(result.days())
                .extracting("day")
                .containsExactly("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday");
    }

    @Test
    void eachDayShouldHaveExactlyNMeals() {
        int mealsPerDay = 3;
        when(mainServiceClient.getCategories()).thenReturn(mockCategories());
        when(mainServiceClient.getMealsByCategory(anyString())).thenReturn(mockMeals(10));

        WeeklyPlanResponse result = mealPlanService.generateWeeklyPlan(mealsPerDay);

        assertThat(result.days()).allSatisfy(day ->
                assertThat(day.meals()).hasSize(mealsPerDay)
        );
    }

    @Test
    void shouldThrowWhenMealsPerDayIsZero() {
        assertThatThrownBy(() -> mealPlanService.generateWeeklyPlan(0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("mealsPerDay must be between 1 and 5");
    }

    @Test
    void shouldThrowWhenMealsPerDayExceedsFive() {
        assertThatThrownBy(() -> mealPlanService.generateWeeklyPlan(6))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("mealsPerDay must be between 1 and 5");
    }
}
