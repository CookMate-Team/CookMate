package com.cookmate.main.controller;

import com.cookmate.main.dto.MealSearchResponse;
import com.cookmate.main.dto.CategoryResponse;
import com.cookmate.main.dto.CommonListResponse;
import com.cookmate.main.dto.Meal;
import com.cookmate.main.service.MealDbClient;
import com.cookmate.main.service.StepService;
import jakarta.validation.constraints.NotBlank;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import java.util.List;
import java.util.Map;

/**
 * Kontroler odpowiedzialny za Discovery API (Integracja z TheMealDB).
 * Ujednolica wyszukiwanie, filtrowanie i listy słownikowe.
 */
@RestController
@RequestMapping("/api/v1/discovery")
@Tag(name = "Discovery", description = "Endpoints for discovering, searching and filtering recipes from TheMealDB")
public class DiscoveryController {

    private final MealDbClient mealDbClient;
    private final StepService stepService;

    public DiscoveryController(MealDbClient mealDbClient, StepService stepService) {
        this.mealDbClient = mealDbClient;
        this.stepService = stepService;
    }

    private MealSearchResponse enrichWithTimes(MealSearchResponse response) {
        if (response == null || response.meals() == null || response.meals().isEmpty()) {
            return response;
        }
        List<String> ids = response.meals().stream().map(Meal::getIdMeal).toList();
        Map<String, Integer> times = stepService.getPreparationTimes(ids);
        
        response.meals().forEach(meal -> {
            meal.setPreparationTimeMinutes(times.get(meal.getIdMeal()));
        });
        
        return response;
    }

    /**
     * Wyszukiwanie potraw po nazwie.
     */
    @Operation(summary = "Search recipes by name", description = "Search TheMealDB recipes by name, enriching with preparation times")
    @GetMapping("/search")
    public Mono<MealSearchResponse> search(@RequestParam @NotBlank String name) {
        return mealDbClient.searchByName(name).map(this::enrichWithTimes);
    }

    /**
     * Pobieranie szczegółów potrawy po ID.
     */
    @Operation(summary = "Lookup recipe by ID", description = "Get details of a specific recipe from TheMealDB")
    @GetMapping("/lookup/{id}")
    public Mono<MealSearchResponse> lookup(@PathVariable String id) {
        return mealDbClient.lookupById(id).map(this::enrichWithTimes);
    }

    /**
     * Pobieranie pełnej listy kategorii z opisami.
     */
    @Operation(summary = "List categories", description = "Get full list of recipe categories with descriptions from TheMealDB")
    @GetMapping("/categories")
    public Mono<CategoryResponse> categories() {
        return mealDbClient.listFullCategories();
    }

    /**
     * Filtrowanie po głównym składniku.
     */
    @Operation(summary = "Filter by ingredient", description = "Filter recipes by main ingredient")
    @GetMapping("/filter/ingredient")
    public Mono<MealSearchResponse> filterByIngredient(@RequestParam String i) {
        return mealDbClient.filterByIngredient(i).map(this::enrichWithTimes);
    }

    /**
     * Filtrowanie po kategorii.
     */
    @Operation(summary = "Filter by category", description = "Filter recipes by category")
    @GetMapping("/filter/category")
    public Mono<MealSearchResponse> filterByCategory(@RequestParam String c) {
        return mealDbClient.filterByCategory(c).map(this::enrichWithTimes);
    }

    /**
     * Pobieranie słowników (kategorie, obszary, składniki).
     * @param type 'c' dla kategorii, 'a' dla obszarów, 'i' dla składników.
     */
    @Operation(summary = "List reference data", description = "List categories ('c'), areas ('a'), or ingredients ('i')")
    @GetMapping("/list")
    public Mono<CommonListResponse> listByType(@RequestParam String type) {
        return mealDbClient.listAllBy(type);
    }
}