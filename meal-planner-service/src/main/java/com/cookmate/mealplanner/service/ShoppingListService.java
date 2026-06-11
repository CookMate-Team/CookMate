package com.cookmate.mealplanner.service;

import com.cookmate.mealplanner.client.MainServiceClient;
import com.cookmate.mealplanner.dto.MealDetailListResponse;
import com.cookmate.mealplanner.dto.MealDetailResponse;
import com.cookmate.mealplanner.dto.ShoppingListItem;
import com.cookmate.mealplanner.dto.ShoppingListRequest;
import com.cookmate.mealplanner.dto.ShoppingListResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ShoppingListService {

    private final MainServiceClient mainServiceClient;

    public ShoppingListResponse buildShoppingList(ShoppingListRequest request) {
        // ingredient name -> [measure, recipes]
        Map<String, IngredientAccumulator> accumulated = new LinkedHashMap<>();

        for (String mealId : request.mealIds()) {
            MealDetailListResponse response = mainServiceClient.lookupById(mealId);
            if (response == null || response.meals() == null) {
                continue;
            }
            for (MealDetailResponse meal : response.meals()) {
                extractIngredients(meal, accumulated);
            }
        }

        List<ShoppingListItem> items = accumulated.entrySet().stream()
                .map(e -> new ShoppingListItem(e.getKey(), e.getValue().measures, e.getValue().recipes))
                .toList();

        return new ShoppingListResponse(items);
    }

    private void extractIngredients(MealDetailResponse meal, Map<String, IngredientAccumulator> accumulated) {
        String recipeName = meal.getStrMeal();
        for (int i = 1; i <= 20; i++) {
            String ingredient = getIngredient(meal, i);
            if (ingredient == null || ingredient.isBlank()) {
                continue;
            }
            String measure = getMeasure(meal, i);
            String trimmedMeasure = measure != null ? measure.trim() : "";
            accumulated.compute(ingredient, (name, acc) -> {
                if (acc == null) {
                    List<String> measures = new ArrayList<>();
                    measures.add(trimmedMeasure);
                    List<String> recipes = new ArrayList<>();
                    recipes.add(recipeName);
                    return new IngredientAccumulator(measures, recipes);
                }
                acc.measures.add(trimmedMeasure);
                if (!acc.recipes.contains(recipeName)) {
                    acc.recipes.add(recipeName);
                }
                return acc;
            });
        }
    }

    private String getIngredient(MealDetailResponse meal, int index) {
        return switch (index) {
            case 1 -> meal.getStrIngredient1();
            case 2 -> meal.getStrIngredient2();
            case 3 -> meal.getStrIngredient3();
            case 4 -> meal.getStrIngredient4();
            case 5 -> meal.getStrIngredient5();
            case 6 -> meal.getStrIngredient6();
            case 7 -> meal.getStrIngredient7();
            case 8 -> meal.getStrIngredient8();
            case 9 -> meal.getStrIngredient9();
            case 10 -> meal.getStrIngredient10();
            case 11 -> meal.getStrIngredient11();
            case 12 -> meal.getStrIngredient12();
            case 13 -> meal.getStrIngredient13();
            case 14 -> meal.getStrIngredient14();
            case 15 -> meal.getStrIngredient15();
            case 16 -> meal.getStrIngredient16();
            case 17 -> meal.getStrIngredient17();
            case 18 -> meal.getStrIngredient18();
            case 19 -> meal.getStrIngredient19();
            case 20 -> meal.getStrIngredient20();
            default -> null;
        };
    }

    private String getMeasure(MealDetailResponse meal, int index) {
        return switch (index) {
            case 1 -> meal.getStrMeasure1();
            case 2 -> meal.getStrMeasure2();
            case 3 -> meal.getStrMeasure3();
            case 4 -> meal.getStrMeasure4();
            case 5 -> meal.getStrMeasure5();
            case 6 -> meal.getStrMeasure6();
            case 7 -> meal.getStrMeasure7();
            case 8 -> meal.getStrMeasure8();
            case 9 -> meal.getStrMeasure9();
            case 10 -> meal.getStrMeasure10();
            case 11 -> meal.getStrMeasure11();
            case 12 -> meal.getStrMeasure12();
            case 13 -> meal.getStrMeasure13();
            case 14 -> meal.getStrMeasure14();
            case 15 -> meal.getStrMeasure15();
            case 16 -> meal.getStrMeasure16();
            case 17 -> meal.getStrMeasure17();
            case 18 -> meal.getStrMeasure18();
            case 19 -> meal.getStrMeasure19();
            case 20 -> meal.getStrMeasure20();
            default -> null;
        };
    }

    private static class IngredientAccumulator {
        List<String> measures;
        List<String> recipes;

        IngredientAccumulator(List<String> measures, List<String> recipes) {
            this.measures = measures;
            this.recipes = recipes;
        }
    }
}
