package com.cookmate.main.service;

import com.cookmate.main.dto.*;
import com.cookmate.main.model.Recipe;
import com.cookmate.main.repository.RecipeRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Serwis zarządzający przepisami lokalnymi oraz integracją z Discovery API (TheMealDB).
 */
@Service
public class RecipeService {

    private final RecipeRepository recipeRepository;
    private final MealDbClient mealDbClient;

    public RecipeService(RecipeRepository recipeRepository, MealDbClient mealDbClient) {
        this.recipeRepository = recipeRepository;
        this.mealDbClient = mealDbClient;
    }

    // LOKALNY CRUD (Dla RecipeController) ---

    public List<Recipe> findAll() {
        return recipeRepository.findAll();
    }

    public Optional<Recipe> findById(Long id) {
        return recipeRepository.findById(id);
    }

    public List<Recipe> findByName(String name) {
        return recipeRepository.findByNameContainingIgnoreCase(name);
    }

    public RecipeListResponse findPaginated(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Recipe> recipePage = recipeRepository.findAll(pageable);

        List<RecipeDTO> dtos = recipePage.getContent().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());

        return new RecipeListResponse(
                dtos,
                (int) recipePage.getTotalElements(),
                page,
                size,
                recipePage.getTotalPages()
        );
    }

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

    public Optional<Recipe> update(Long id, Recipe updated) {
        return recipeRepository.findById(id).map(existing -> {
            existing.setName(updated.getName());
            existing.setDescription(updated.getDescription());
            existing.setIngredients(updated.getIngredients());
            existing.setInstructions(updated.getInstructions());
            existing.setPreparationTimeMinutes(updated.getPreparationTimeMinutes());
            return recipeRepository.save(existing);
        });
    }

    public boolean deleteById(Long id) {
        if (recipeRepository.existsById(id)) {
            recipeRepository.deleteById(id);
            return true;
        }
        return false;
    }

    public Mono<MealSearchResponse> searchMealsByName(String name) {
        return mealDbClient.searchByName(name);
    }

    public Mono<MealSearchResponse> lookupMeal(String mealId) {
        return mealDbClient.lookupById(mealId);
    }

    public Mono<MealSearchResponse> filterByIngredient(String ingredient) {
        return mealDbClient.filterByIngredient(ingredient);
    }

    public Mono<CategoryResponse> getAllCategories() {
        return mealDbClient.listFullCategories();
    }

    public Mono<CommonListResponse> getDictionaryList(String type) {
        return mealDbClient.listAllBy(type);
    }

    public Recipe syncMealFromTheMealDB(Meal meal) {
        Recipe recipe = new Recipe();
        recipe.setName(meal.strMeal());
        recipe.setDescription("Kategoria: " + meal.strCategory() + ", Kuchnia: " + meal.strArea());
        recipe.setIngredients(buildIngredientsString(meal));
        recipe.setInstructions(meal.strInstructions());
        recipe.setPreparationTimeMinutes(30);

        return recipeRepository.save(recipe);
    }

    // --- METODY POMOCNICZE ---

    private RecipeDTO toDTO(Recipe recipe) {
        return new RecipeDTO(
                recipe.getId(),
                recipe.getName(),
                recipe.getDescription(),
                recipe.getIngredients(),
                recipe.getInstructions(),
                recipe.getPreparationTimeMinutes(),
                recipe.getCreatedAt()
        );
    }

    private String buildIngredientsString(Meal meal) {
        StringBuilder sb = new StringBuilder();
        addIngredient(sb, meal.strIngredient1(), meal.strMeasure1());
        addIngredient(sb, meal.strIngredient2(), meal.strMeasure2());
        addIngredient(sb, meal.strIngredient3(), meal.strMeasure3());
        return sb.toString();
    }

    private void addIngredient(StringBuilder sb, String ingredient, String measure) {
        if (ingredient != null && !ingredient.isBlank()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(ingredient);
            if (measure != null && !measure.isBlank()) {
                sb.append(" (").append(measure).append(")");
            }
        }
    }
}