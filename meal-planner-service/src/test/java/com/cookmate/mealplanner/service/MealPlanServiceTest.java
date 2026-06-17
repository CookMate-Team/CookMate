package com.cookmate.mealplanner.service;

import com.cookmate.mealplanner.client.MainServiceClient;
import com.cookmate.mealplanner.dto.CategoryResponse;
import com.cookmate.mealplanner.dto.MealSearchResponse;
import com.cookmate.mealplanner.dto.WeeklyPlanResponse;
import com.cookmate.mealplanner.exception.MainServiceCommunicationException;
import com.cookmate.mealplanner.exception.MealPlanGenerationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("MealPlanService — unit tests")
class MealPlanServiceTest {

    @Mock
    private MainServiceClient mainServiceClient;

    @InjectMocks
    private MealPlanService mealPlanService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    // --- helpers ---

    private CategoryResponse mockCategories() {
        return new CategoryResponse(List.of(
                new CategoryResponse.Category("1", "Beef"),
                new CategoryResponse.Category("2", "Breakfast"),
                new CategoryResponse.Category("3", "Chicken"),
                new CategoryResponse.Category("4", "Dessert"),
                new CategoryResponse.Category("5", "Lamb"),
                new CategoryResponse.Category("6", "Pasta"),
                new CategoryResponse.Category("7", "Seafood"),
                new CategoryResponse.Category("8", "Side"),
                new CategoryResponse.Category("9", "Starter"),
                new CategoryResponse.Category("10", "Pork")
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

    private MealSearchResponse emptyMeals() {
        return new MealSearchResponse(List.of());
    }

    // --- generateWeeklyPlan — shape ---

    @Test
    @DisplayName("generateWeeklyPlan — zwraca dokładnie 7 dni")
    void generateWeeklyPlan_returnsExactlySevenDays() {
        when(mainServiceClient.getCategories()).thenReturn(mockCategories());
        when(mainServiceClient.getMealsByCategory(anyString())).thenReturn(mockMeals(10));

        WeeklyPlanResponse result = mealPlanService.generateWeeklyPlan(2);

        assertThat(result.days()).hasSize(7);
    }

    @Test
    @DisplayName("generateWeeklyPlan — dni są we właściwej kolejności (poniedziałek–niedziela)")
    void generateWeeklyPlan_daysAreInCorrectOrder() {
        when(mainServiceClient.getCategories()).thenReturn(mockCategories());
        when(mainServiceClient.getMealsByCategory(anyString())).thenReturn(mockMeals(10));

        WeeklyPlanResponse result = mealPlanService.generateWeeklyPlan(1);

        assertThat(result.days())
                .extracting("day")
                .containsExactly("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday");
    }

    @Test
    @DisplayName("generateWeeklyPlan — każdy dzień ma dokładnie n posiłków gdy wszystkie kategorie zwracają dane")
    void generateWeeklyPlan_eachDayHasExactlyNMeals() {
        int mealsPerDay = 3;
        when(mainServiceClient.getCategories()).thenReturn(mockCategories());
        when(mainServiceClient.getMealsByCategory(anyString())).thenReturn(mockMeals(10));

        WeeklyPlanResponse result = mealPlanService.generateWeeklyPlan(mealsPerDay);

        assertThat(result.days()).allSatisfy(day ->
                assertThat(day.meals()).hasSize(mealsPerDay)
        );
    }

    @Test
    @DisplayName("generateWeeklyPlan — gdy kategoria nie ma posiłków, slot jest pomijany")
    void generateWeeklyPlan_emptyCategory_slotIsSkipped() {
        when(mainServiceClient.getCategories()).thenReturn(mockCategories());
        when(mainServiceClient.getMealsByCategory(anyString())).thenReturn(emptyMeals());

        WeeklyPlanResponse result = mealPlanService.generateWeeklyPlan(3);

        assertThat(result.days()).allSatisfy(day ->
                assertThat(day.meals()).isEmpty()
        );
    }

    @Test
    @DisplayName("generateWeeklyPlan — pobiera kategorie z MainServiceClient")
    void generateWeeklyPlan_fetchesCategoriesFromMainServiceClient() {
        when(mainServiceClient.getCategories()).thenReturn(mockCategories());
        when(mainServiceClient.getMealsByCategory(anyString())).thenReturn(mockMeals(5));

        mealPlanService.generateWeeklyPlan(1);

        verify(mainServiceClient, times(1)).getCategories();
    }

    @Test
    @DisplayName("generateWeeklyPlan — wywołuje getMealsByCategory tyle razy ile wynosi liczba slotów × 7 dni")
    void generateWeeklyPlan_callsMealsByCategoryForEachSlotAndDay() {
        when(mainServiceClient.getCategories()).thenReturn(mockCategories());
        when(mainServiceClient.getMealsByCategory(anyString())).thenReturn(mockMeals(5));

        // mealsPerDay=2 → 2 slots (BREAKFAST + MAIN) × 7 days = 14 calls
        mealPlanService.generateWeeklyPlan(2);

        verify(mainServiceClient, times(14)).getMealsByCategory(anyString());
    }

    @Test
    @DisplayName("generateWeeklyPlan — rzuca wyjątek gdy MainServiceClient niedostępny")
    void generateWeeklyPlan_throwsMainServiceCommunicationException_whenClientFails() {
        when(mainServiceClient.getCategories()).thenThrow(new RuntimeException("Connection refused"));

        assertThatThrownBy(() -> mealPlanService.generateWeeklyPlan(1))
                .isInstanceOf(MainServiceCommunicationException.class)
                .hasMessageContaining("Failed to fetch categories from main-service");
    }

    @Test
    @DisplayName("generateWeeklyPlan — rzuca wyjątek gdy brak kategorii")
    void generateWeeklyPlan_throwsMealPlanGenerationException_whenNoCategoriesReturned() {
        when(mainServiceClient.getCategories()).thenReturn(new CategoryResponse(List.of()));

        assertThatThrownBy(() -> mealPlanService.generateWeeklyPlan(1))
                .isInstanceOf(MealPlanGenerationException.class)
                .hasMessageContaining("No categories available");
    }

    // --- slot mapping ---

    @Test
    @DisplayName("mealsPerDay=1 — MAIN slot nie używa kategorii Breakfast, Dessert, Starter ani Side")
    void generateWeeklyPlan_mealsPerDayOne_mainSlotNeverUsesFixedCategories() {
        when(mainServiceClient.getCategories()).thenReturn(mockCategories());
        when(mainServiceClient.getMealsByCategory(anyString())).thenReturn(mockMeals(5));

        mealPlanService.generateWeeklyPlan(1);

        verify(mainServiceClient, never()).getMealsByCategory("Breakfast");
        verify(mainServiceClient, never()).getMealsByCategory("Dessert");
        verify(mainServiceClient, never()).getMealsByCategory("Starter");
        verify(mainServiceClient, never()).getMealsByCategory("Side");
        // exactly 7 calls total (one MAIN per day)
        verify(mainServiceClient, times(7)).getMealsByCategory(anyString());
    }

    @Test
    @DisplayName("mealsPerDay=2 — BREAKFAST slot wywołuje 'Breakfast' 7 razy, MAIN nie używa stałych kategorii")
    void generateWeeklyPlan_mealsPerDayTwo_breakfastSlotUsesBreakfastCategory() {
        when(mainServiceClient.getCategories()).thenReturn(mockCategories());
        when(mainServiceClient.getMealsByCategory(anyString())).thenReturn(mockMeals(5));

        mealPlanService.generateWeeklyPlan(2);

        verify(mainServiceClient, times(7)).getMealsByCategory("Breakfast");
        verify(mainServiceClient, never()).getMealsByCategory("Dessert");
        verify(mainServiceClient, never()).getMealsByCategory("Starter");
        verify(mainServiceClient, never()).getMealsByCategory("Side");
        // 2 slots × 7 days = 14 total
        verify(mainServiceClient, times(14)).getMealsByCategory(anyString());
    }

    @Test
    @DisplayName("mealsPerDay=5 — wszystkie 5 slotów mapują się na właściwe kategorie")
    void generateWeeklyPlan_mealsPerDayFive_allSlotsUseCorrectCategories() {
        when(mainServiceClient.getCategories()).thenReturn(mockCategories());
        when(mainServiceClient.getMealsByCategory(anyString())).thenReturn(mockMeals(5));

        mealPlanService.generateWeeklyPlan(5);

        verify(mainServiceClient, times(7)).getMealsByCategory("Breakfast");
        verify(mainServiceClient, times(7)).getMealsByCategory("Starter");
        verify(mainServiceClient, times(7)).getMealsByCategory("Side");
        verify(mainServiceClient, times(7)).getMealsByCategory("Dessert");
        // 5 slots × 7 days = 35 total (4 fixed + 7 MAIN)
        verify(mainServiceClient, times(35)).getMealsByCategory(anyString());
    }

    // --- validation ---

    @Test
    @DisplayName("generateWeeklyPlan — rzuca wyjątek gdy mealsPerDay=0")
    void generateWeeklyPlan_throwsWhenMealsPerDayIsZero() {
        assertThatThrownBy(() -> mealPlanService.generateWeeklyPlan(0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("mealsPerDay must be between 1 and 5");
    }

    @Test
    @DisplayName("generateWeeklyPlan — rzuca wyjątek gdy mealsPerDay=6")
    void generateWeeklyPlan_throwsWhenMealsPerDayExceedsFive() {
        assertThatThrownBy(() -> mealPlanService.generateWeeklyPlan(6))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("mealsPerDay must be between 1 and 5");
    }

    @Test
    @DisplayName("generateWeeklyPlan — działa dla wartości granicznych (1 i 5)")
    void generateWeeklyPlan_worksForBoundaryValues() {
        when(mainServiceClient.getCategories()).thenReturn(mockCategories());
        when(mainServiceClient.getMealsByCategory(anyString())).thenReturn(mockMeals(10));

        WeeklyPlanResponse resultMin = mealPlanService.generateWeeklyPlan(1);
        WeeklyPlanResponse resultMax = mealPlanService.generateWeeklyPlan(5);

        assertThat(resultMin.days()).hasSize(7);
        assertThat(resultMin.days()).allSatisfy(day -> assertThat(day.meals()).hasSize(1));
        assertThat(resultMax.days()).hasSize(7);
        assertThat(resultMax.days()).allSatisfy(day -> assertThat(day.meals()).hasSize(5));
    }
}
