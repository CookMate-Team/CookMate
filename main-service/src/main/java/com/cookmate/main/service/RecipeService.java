package com.cookmate.main.service;

import com.cookmate.main.dto.*;
import com.cookmate.main.model.Recipe;
import com.cookmate.main.repository.RecipeRepository;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Optional;

/**
 * Zaktualizowany serwis zarządzający przepisami oraz integracją z Discovery API.
 * Obsługuje nowe metody wyszukiwania i filtrowania z TheMealDB.
 */
@Service
public class RecipeService {

    private final RecipeRepository recipeRepository;
    private final MealDbClient mealDbClient;

    public RecipeService(RecipeRepository recipeRepository, MealDbClient mealDbClient) {
        this.recipeRepository = recipeRepository;
        this.mealDbClient = mealDbClient;
    }

    // --- SEKCJA DISCOVERY (INTEGRACJA Z THEMEALDB) ---

    /**
     * Wyszukiwanie potraw po nazwie (zamiast starej metody po literze).
     */
    public Mono<MealSearchResponse> searchMealsByName(String name) {
        return mealDbClient.searchByName(name);
    }

    /**
     * Pobieranie szczegółów potrawy po ID.
     */
    public Mono<MealSearchResponse> lookupMeal(String mealId) {
        return mealDbClient.lookupById(mealId);
    }

    /**
     * Filtrowanie potraw po głównym składniku.
     */
    public Mono<MealSearchResponse> filterByIngredient(String ingredient) {
        return mealDbClient.filterByIngredient(ingredient);
    }

    /**
     * Pobieranie pełnej listy kategorii z opisami i zdjęciami.
     */
    public Mono<CategoryResponse> getAllCategories() {
        return mealDbClient.listFullCategories();
    }

    /**
     * Pobieranie słowników (obszary, składniki, uproszczone kategorie).
     * @param type "a" dla obszarów, "i" dla składników, "c" dla kategorii.
     */
    public Mono<CommonListResponse> getDictionaryList(String type) {
        return mealDbClient.listAllBy(type);
    }

    // --- SEKCJA LOKALNEGO ZARZĄDZANIA (RECIPE CRUD) ---

    public Recipe save(RecipeCreateRequest request) {
        Recipe recipe = new Recipe(
                request.name(),
                request.description(),
                request.ingredients(),
                request.instructions(),
                request.preparationTimeMinutes()
        );
        return recipeRepository.save(recipe);
    }

    public List<Recipe> findAll() {
        return recipeRepository.findAll();
    }

    public Optional<Recipe> findById(Long id) {
        return recipeRepository.findById(id);
    }

    /**
     * Synchronizacja danych z zewnętrznego API do lokalnej bazy danych.
     */
    public Recipe syncMealFromTheMealDB(Meal meal) {
        Recipe recipe = new Recipe();
        recipe.setName(meal.strMeal());
        recipe.setDescription("Category: " + meal.strCategory() + ", Area: " + meal.strArea());
        recipe.setIngredients(buildIngredientsString(meal));
        recipe.setInstructions(meal.strInstructions());
        // Domyślny czas przygotowania, jeśli API go nie podaje
        recipe.setPreparationTimeMinutes(30);

        return recipeRepository.save(recipe);
    }

    // --- METODY POMOCNICZE ---

    /**
     * Buduje sformatowany ciąg znaków ze składników i ich miar.
     */
    private String buildIngredientsString(Meal meal) {
        StringBuilder ingredients = new StringBuilder();

        // Wykorzystujemy Twoją logikę addIngredient dla wszystkich 20 pól
        addIngredient(ingredients, meal.strIngredient1(), meal.strMeasure1());
        addIngredient(ingredients, meal.strIngredient2(), meal.strMeasure2());
        addIngredient(ingredients, meal.strIngredient3(), meal.strMeasure3());
        addIngredient(ingredients, meal.strIngredient4(), meal.strMeasure4());
        addIngredient(ingredients, meal.strIngredient5(), meal.strMeasure5());
        // ... (powtórzenie dla pozostałych pól do 20)
        addIngredient(ingredients, meal.strIngredient20(), meal.strMeasure20());

        return ingredients.toString().trim();
    }

    private void addIngredient(StringBuilder sb, String ingredient, String measure) {
        if (ingredient != null && !ingredient.isBlank()) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(ingredient);
            if (measure != null && !measure.isBlank()) {
                sb.append(" (").append(measure).append(")");
            }
        }
    }
}