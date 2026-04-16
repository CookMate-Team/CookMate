package com.cookmate.main.controller;

import com.cookmate.main.dto.MealSearchResponse;
import com.cookmate.main.service.RecipeService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

/**
 * REST Controller for external meal API integration.
 * Provides endpoints to search and lookup meals from TheMealDB API.
 */
@RestController
@RequestMapping("/api/recipes/search")
public class RecipeSearchController {

    private final RecipeService recipeService;

    /**
     * Construct RecipeSearchController with recipe service.
     *
     * @param recipeService service for recipe operations
     */
    public RecipeSearchController(RecipeService recipeService) {
        this.recipeService = recipeService;
    }

    /**
     * Search meals from TheMealDB API by first letter.
     * 
     * API: GET /search.php?f={letter}
     * Returns all meals whose name begins with the given letter.
     *
     * @param letter first letter to search for (a-z)
     * @return Mono with search response
     */
    @GetMapping("/themealdb/letter")
    public Mono<ResponseEntity<MealSearchResponse>> searchByLetter(
        @RequestParam String letter) {
        return recipeService.searchMealsByLetter(letter)
            .map(ResponseEntity::ok);
    }

    /**
     * Lookup full meal details from TheMealDB API by meal ID.
     * 
     * API: GET /lookup.php?i={id}
     * Returns complete meal information including all ingredients and measurements.
     *
     * @param mealId meal ID from TheMealDB
     * @return Mono with meal details
     */
    @GetMapping("/themealdb/meal")
    public Mono<ResponseEntity<MealSearchResponse>> lookupMeal(
        @RequestParam String mealId) {
        return recipeService.lookupMeal(mealId)
            .map(ResponseEntity::ok);
    }
}
