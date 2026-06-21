package com.cookmate.main.service;

import com.cookmate.main.dto.*;
import com.cookmate.main.exception.RecipeNotFoundException;
import com.cookmate.main.model.Recipe;
import com.cookmate.main.repository.RecipeRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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

    public Recipe findByIdOrThrow(Long id) {
        return recipeRepository.findById(id)
            .orElseThrow(() -> new RecipeNotFoundException(id));
    }

    public List<Recipe> findByName(String name) {
        return recipeRepository.findByNameContainingIgnoreCase(name);
    }

    public RecipeListResponse findByNamePaginated(String name, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "name"));
        Page<Recipe> recipePage = recipeRepository.findByNameContainingIgnoreCase(name, pageable);

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

    public RecipeListResponse findMyRecipesPaginated(String userId, int page, int size) {
        // Find by userId manually since Pageable isn't yet added for it in repo
        List<Recipe> allMyRecipes = recipeRepository.findByUserId(userId);
        int total = allMyRecipes.size();
        int start = Math.min(page * size, total);
        int end = Math.min((page + 1) * size, total);
        List<Recipe> paged = allMyRecipes.subList(start, end);
        
        List<RecipeDTO> dtos = paged.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());

        return new RecipeListResponse(
                dtos,
                total,
                page,
                size,
                (int) Math.ceil((double) total / size)
        );
    }

    public RecipeListResponse findPaginated(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "name"));
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
        if (request.defaultPortions() != null) {
            recipe.setDefaultPortions(request.defaultPortions());
        }
        recipe.setImageUrl(request.imageUrl());
        return recipeRepository.save(recipe);
    }

    public Recipe saveCustom(RecipeCreateRequest request, String userId) {
        Recipe recipe = new Recipe(
                request.name(),
                request.description(),
                request.ingredients(),
                request.instructions(),
                request.preparationTimeMinutes(),
                userId
        );
        if (request.defaultPortions() != null) {
            recipe.setDefaultPortions(request.defaultPortions());
        }
        recipe.setImageUrl(request.imageUrl());
        return recipeRepository.save(recipe);
    }

    public Optional<Recipe> update(Long id, Recipe updated) {
        return recipeRepository.findById(id).map(existing -> {
            existing.setName(updated.getName());
            existing.setDescription(updated.getDescription());
            existing.setIngredients(updated.getIngredients());
            existing.setInstructions(updated.getInstructions());
            existing.setPreparationTimeMinutes(updated.getPreparationTimeMinutes());
            existing.setDefaultPortions(updated.getDefaultPortions());
            existing.setImageUrl(updated.getImageUrl());
            return recipeRepository.save(existing);
        });
    }

    public Recipe updateOrThrow(Long id, Recipe updated) {
        Recipe existing = findByIdOrThrow(id);
        existing.setName(updated.getName());
        existing.setDescription(updated.getDescription());
        existing.setIngredients(updated.getIngredients());
        existing.setInstructions(updated.getInstructions());
        existing.setPreparationTimeMinutes(updated.getPreparationTimeMinutes());
        existing.setDefaultPortions(updated.getDefaultPortions());
        existing.setImageUrl(updated.getImageUrl());
        return recipeRepository.save(existing);
    }

    public Recipe updateCustomOrThrow(Long id, Recipe updated, String userId) {
        Recipe existing = findByIdOrThrow(id);
        if (!userId.equals(existing.getUserId())) {
            throw new org.springframework.security.access.AccessDeniedException("Nie jesteś właścicielem tego przepisu");
        }
        existing.setName(updated.getName());
        existing.setDescription(updated.getDescription());
        existing.setIngredients(updated.getIngredients());
        existing.setInstructions(updated.getInstructions());
        existing.setPreparationTimeMinutes(updated.getPreparationTimeMinutes());
        existing.setDefaultPortions(updated.getDefaultPortions());
        existing.setImageUrl(updated.getImageUrl());
        return recipeRepository.save(existing);
    }

    public boolean deleteById(Long id) {
        if (recipeRepository.existsById(id)) {
            recipeRepository.deleteById(id);
            return true;
        }
        return false;
    }

    public void deleteByIdOrThrow(Long id) {
        Recipe recipe = findByIdOrThrow(id);
        recipeRepository.delete(recipe);
    }

    public void deleteCustomByIdOrThrow(Long id, String userId) {
        Recipe recipe = findByIdOrThrow(id);
        if (!userId.equals(recipe.getUserId())) {
            throw new org.springframework.security.access.AccessDeniedException("Nie jesteś właścicielem tego przepisu");
        }
        recipeRepository.delete(recipe);
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
        recipe.setName(meal.getStrMeal());
        recipe.setDescription("Kategoria: " + meal.getStrCategory() + ", Kuchnia: " + meal.getStrArea());
        recipe.setIngredients(buildIngredientsString(meal));
        recipe.setInstructions(meal.getStrInstructions());
        recipe.setPreparationTimeMinutes(30);
        recipe.setDefaultPortions(4);

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
                recipe.getCreatedAt(),
                recipe.getDefaultPortions(),
                recipe.getUserId(),
                recipe.isCustom(),
                recipe.getImageUrl(),
                null // Steps are loaded separately when needed, or could be fetched via StepService if required
        );
    }

    private String buildIngredientsString(Meal meal) {
        StringBuilder sb = new StringBuilder();
        addIngredient(sb, meal.getStrIngredient1(), meal.getStrMeasure1());
        addIngredient(sb, meal.getStrIngredient2(), meal.getStrMeasure2());
        addIngredient(sb, meal.getStrIngredient3(), meal.getStrMeasure3());
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
