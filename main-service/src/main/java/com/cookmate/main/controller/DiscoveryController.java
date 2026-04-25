package com.cookmate.main.controller;

import com.cookmate.main.dto.MealSearchResponse;
import com.cookmate.main.dto.CategoryResponse;
import com.cookmate.main.dto.CommonListResponse;
import com.cookmate.main.service.MealDbClient;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

/**
 * Kontroler odpowiedzialny za Discovery API (Integracja z TheMealDB).
 * Ujednolica wyszukiwanie, filtrowanie i listy słownikowe.
 */
@RestController
@RequestMapping("/api/v1/discovery")
public class DiscoveryController {

    private final MealDbClient mealDbClient;

    public DiscoveryController(MealDbClient mealDbClient) {
        this.mealDbClient = mealDbClient;
    }

    /**
     * Wyszukiwanie potraw po nazwie.
     */
    @GetMapping("/search")
    public Mono<MealSearchResponse> search(@RequestParam @NotBlank String name) {
        return mealDbClient.searchByName(name);
    }

    /**
     * Pobieranie szczegółów potrawy po ID.
     */
    @GetMapping("/lookup/{id}")
    public Mono<MealSearchResponse> lookup(@PathVariable String id) {
        return mealDbClient.lookupById(id);
    }

    /**
     * Pobieranie pełnej listy kategorii z opisami.
     */
    @GetMapping("/categories")
    public Mono<CategoryResponse> categories() {
        return mealDbClient.listFullCategories();
    }

    /**
     * Filtrowanie po głównym składniku.
     */
    @GetMapping("/filter/ingredient")
    public Mono<MealSearchResponse> filterByIngredient(@RequestParam String i) {
        return mealDbClient.filterByIngredient(i);
    }

    /**
     * Pobieranie słowników (kategorie, obszary, składniki).
     * @param type 'c' dla kategorii, 'a' dla obszarów, 'i' dla składników.
     */
    @GetMapping("/list")
    public Mono<CommonListResponse> listByType(@RequestParam String type) {
        return mealDbClient.listAllBy(type);
    }
}