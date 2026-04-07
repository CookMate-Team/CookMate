package com.cookmate.simulator.controller;

import com.cookmate.simulator.client.MainServiceClient;
import com.cookmate.simulator.dto.RecipeDto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Random;

@RestController
@RequestMapping("/api/simulator")
public class SimulatorController {

    private final MainServiceClient mainServiceClient;
    private final Random random = new Random();

    public SimulatorController(MainServiceClient mainServiceClient) {
        this.mainServiceClient = mainServiceClient;
    }

    @GetMapping("/recipes")
    public ResponseEntity<List<RecipeDto>> listRecipes() {
        return ResponseEntity.ok(mainServiceClient.getAllRecipes());
    }

    @GetMapping("/recipes/{id}")
    public ResponseEntity<RecipeDto> getRecipe(@PathVariable Long id) {
        return ResponseEntity.ok(mainServiceClient.getRecipeById(id));
    }

    @GetMapping("/meal-plan")
    public ResponseEntity<Map<String, Object>> generateMealPlan(@RequestParam(defaultValue = "3") int days) {
        List<RecipeDto> recipes = mainServiceClient.getAllRecipes();
        if (recipes.isEmpty()) {
            return ResponseEntity.ok(Map.of(
                "message", "No recipes available. Please add recipes to main-service first.",
                "days", days
            ));
        }

        var mealPlan = new java.util.LinkedHashMap<String, Object>();
        mealPlan.put("days", days);
        mealPlan.put("totalRecipes", recipes.size());

        var plan = new java.util.ArrayList<Map<String, Object>>();
        for (int i = 1; i <= days; i++) {
            RecipeDto recipe = recipes.get(random.nextInt(recipes.size()));
            plan.add(Map.of(
                "day", i,
                "recipeId", recipe.getId(),
                "recipeName", recipe.getName(),
                "preparationTime", recipe.getPreparationTimeMinutes() != null
                    ? recipe.getPreparationTimeMinutes() + " minutes"
                    : "N/A"
            ));
        }
        mealPlan.put("plan", plan);
        return ResponseEntity.ok(mealPlan);
    }

    @GetMapping("/health-check")
    public ResponseEntity<Map<String, String>> serviceHealthCheck() {
        try {
            List<RecipeDto> recipes = mainServiceClient.getAllRecipes();
            return ResponseEntity.ok(Map.of(
                "status", "OK",
                "mainService", "REACHABLE",
                "recipeCount", String.valueOf(recipes.size())
            ));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of(
                "status", "DEGRADED",
                "mainService", "UNREACHABLE",
                "error", e.getMessage()
            ));
        }
    }
}
