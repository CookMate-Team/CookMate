package com.cookmate.main.service;

import com.cookmate.main.dto.RecipeCreateRequest;
import com.cookmate.main.dto.RecipeDTO;
import com.cookmate.main.dto.RecipeListResponse;
import com.cookmate.main.dto.Meal;
import com.cookmate.main.dto.MealSearchResponse;
import com.cookmate.main.model.Recipe;
import com.cookmate.main.repository.RecipeRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Optional;

/**
 * Service for managing recipes and integrating with TheMealDB API.
 * Handles CRUD operations and external meal synchronization.
 */
@Service
public class RecipeService {

    private final RecipeRepository recipeRepository;
    private final MealDbClient mealDbClient;

    /**
     * Construct RecipeService with repository and external API client.
     *
     * @param recipeRepository JPA repository for Recipe entity
     * @param mealDbClient client for TheMealDB API calls
     */
    public RecipeService(RecipeRepository recipeRepository, MealDbClient mealDbClient) {
        this.recipeRepository = recipeRepository;
        this.mealDbClient = mealDbClient;
    }

    /**
     * Find all recipes in the database.
     *
     * @return list of all recipes
     */
    public List<Recipe> findAll() {
        return recipeRepository.findAll();
    }

    /**
     * Find a recipe by its ID.
     *
     * @param id recipe ID
     * @return Optional containing recipe if found
     */
    public Optional<Recipe> findById(Long id) {
        return recipeRepository.findById(id);
    }

    /**
     * Find recipes by name (case-insensitive partial match).
     *
     * @param name recipe name or partial name
     * @return list of matching recipes
     */
    public List<Recipe> findByName(String name) {
        return recipeRepository.findByNameContainingIgnoreCase(name);
    }

    /**
     * Find recipes with pagination.
     *
     * @param pageNumber page number (0-based)
     * @param pageSize number of items per page
     * @return paginated recipe list response
     */
    public RecipeListResponse findPaginated(int pageNumber, int pageSize) {
        Pageable pageable = PageRequest.of(pageNumber, pageSize);
        Page<Recipe> page = recipeRepository.findAll(pageable);

        List<RecipeDTO> recipeDTOs = page.getContent()
            .stream()
            .map(this::toDTO)
            .toList();

        return new RecipeListResponse(
            recipeDTOs,
            (int) page.getTotalElements(),
            pageNumber,
            pageSize,
            page.getTotalPages()
        );
    }

    /**
     * Save a new recipe from a creation request.
     *
     * @param request recipe creation request containing recipe details
     * @return saved recipe
     */
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

    /**
     * Save a recipe entity.
     *
     * @param recipe recipe to save
     * @return saved recipe
     */
    public Recipe save(Recipe recipe) {
        return recipeRepository.save(recipe);
    }

    /**
     * Update an existing recipe.
     *
     * @param id recipe ID
     * @param updatedRecipe recipe with updated values
     * @return Optional containing updated recipe if found
     */
    public Optional<Recipe> update(Long id, Recipe updatedRecipe) {
        return recipeRepository.findById(id).map(existing -> {
            if (updatedRecipe.getName() != null) {
                existing.setName(updatedRecipe.getName());
            }
            if (updatedRecipe.getDescription() != null) {
                existing.setDescription(updatedRecipe.getDescription());
            }
            if (updatedRecipe.getIngredients() != null) {
                existing.setIngredients(updatedRecipe.getIngredients());
            }
            if (updatedRecipe.getInstructions() != null) {
                existing.setInstructions(updatedRecipe.getInstructions());
            }
            if (updatedRecipe.getPreparationTimeMinutes() != null) {
                existing.setPreparationTimeMinutes(updatedRecipe.getPreparationTimeMinutes());
            }
            return recipeRepository.save(existing);
        });
    }

    /**
     * Delete a recipe by ID.
     *
     * @param id recipe ID
     * @return true if deletion was successful, false if recipe not found
     */
    public boolean deleteById(Long id) {
        if (recipeRepository.existsById(id)) {
            recipeRepository.deleteById(id);
            return true;
        }
        return false;
    }

    /**
     * Search for meals from TheMealDB API by first letter.
     * Performs non-blocking reactive search.
     *
     * @param letter first letter to search for (a-z)
     * @return Mono containing search response from TheMealDB
     */
    public Mono<MealSearchResponse> searchMealsByLetter(String letter) {
        return mealDbClient.searchByLetter(letter);
    }

    /**
     * Lookup full meal details from TheMealDB API by meal ID.
     *
     * @param mealId meal ID from TheMealDB
     * @return Mono containing meal details
     */
    public Mono<MealSearchResponse> lookupMeal(String mealId) {
        return mealDbClient.lookupById(mealId);
    }

    /**
     * Sync meal from TheMealDB into local database.
     * Creates new recipe based on TheMealDB meal data.
     *
     * @param meal meal data from TheMealDB
     * @return saved local recipe
     */
    public Recipe syncMealFromTheMealDB(Meal meal) {
        String ingredients = buildIngredientsString(meal);
        
        Recipe recipe = new Recipe(
            meal.strMeal(),
            "Category: " + meal.strCategory() + " | Area: " + meal.strArea(),
            ingredients,
            meal.strInstructions(),
            null
        );
        return recipeRepository.save(recipe);
    }

    /**
     * Build ingredients string from meal data.
     * Combines ingredient names with their measurements.
     *
     * @param meal meal from TheMealDB
     * @return formatted ingredients string
     */
    private String buildIngredientsString(Meal meal) {
        StringBuilder ingredients = new StringBuilder();
        
        addIngredient(ingredients, meal.strIngredient1(), meal.strMeasure1());
        addIngredient(ingredients, meal.strIngredient2(), meal.strMeasure2());
        addIngredient(ingredients, meal.strIngredient3(), meal.strMeasure3());
        addIngredient(ingredients, meal.strIngredient4(), meal.strMeasure4());
        addIngredient(ingredients, meal.strIngredient5(), meal.strMeasure5());
        addIngredient(ingredients, meal.strIngredient6(), meal.strMeasure6());
        addIngredient(ingredients, meal.strIngredient7(), meal.strMeasure7());
        addIngredient(ingredients, meal.strIngredient8(), meal.strMeasure8());
        addIngredient(ingredients, meal.strIngredient9(), meal.strMeasure9());
        addIngredient(ingredients, meal.strIngredient10(), meal.strMeasure10());
        addIngredient(ingredients, meal.strIngredient11(), meal.strMeasure11());
        addIngredient(ingredients, meal.strIngredient12(), meal.strMeasure12());
        addIngredient(ingredients, meal.strIngredient13(), meal.strMeasure13());
        addIngredient(ingredients, meal.strIngredient14(), meal.strMeasure14());
        addIngredient(ingredients, meal.strIngredient15(), meal.strMeasure15());
        addIngredient(ingredients, meal.strIngredient16(), meal.strMeasure16());
        addIngredient(ingredients, meal.strIngredient17(), meal.strMeasure17());
        addIngredient(ingredients, meal.strIngredient18(), meal.strMeasure18());
        addIngredient(ingredients, meal.strIngredient19(), meal.strMeasure19());
        addIngredient(ingredients, meal.strIngredient20(), meal.strMeasure20());
        
        return ingredients.toString().trim();
    }

    /**
     * Add single ingredient to ingredients string.
     *
     * @param sb StringBuilder to append to
     * @param ingredient ingredient name
     * @param measure measurement amount
     */
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

    /**
     * Convert Recipe entity to RecipeDTO.
     *
     * @param recipe recipe entity
     * @return recipe DTO
     */
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
}

