package com.cookmate.main.controller;

import com.cookmate.main.dto.RecipeCreateRequest;
import com.cookmate.main.dto.RecipeDTO;
import com.cookmate.main.dto.RecipeListResponse;
import com.cookmate.main.dto.RecipeUpdateRequest;
import com.cookmate.main.dto.StepDTO;
import com.cookmate.main.model.Recipe;
import com.cookmate.main.service.RecipeService;
import com.cookmate.main.service.StepService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for recipe management endpoints.
 * Provides CRUD operations and search capabilities for recipes.
 */
@RestController
@RequestMapping("/api/recipes")
public class RecipeController {

    private final RecipeService recipeService;
    private final StepService stepService;

    /**
     * Construct RecipeController with recipe and step services.
     *
     * @param recipeService service for recipe operations
     * @param stepService service for step operations
     */
    public RecipeController(RecipeService recipeService, StepService stepService) {
        this.recipeService = recipeService;
        this.stepService = stepService;
    }

    /**
     * Get all recipes with optional search by name and pagination.
     * If pagination parameters are not provided, defaults to page=0, size=10.
     *
     * @param page page number (0-based), default is 0
     * @param size page size, default is 10
     * @param name optional recipe name filter
     * @return paginated list of recipes
     */
    @GetMapping
    public ResponseEntity<RecipeListResponse> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String name) {
        if (name != null && !name.isBlank()) {
            // For search results, apply pagination on filtered results
            return ResponseEntity.ok(recipeService.findByNamePaginated(name, page, size));
        }
        // Return all recipes with pagination
        return ResponseEntity.ok(recipeService.findPaginated(page, size));
    }

    /**
     * Get recipes with pagination.
     *
     * @param page page number (0-based)
     * @param size page size
     * @return paginated recipe response
     */
    @GetMapping("/paginated")
    public ResponseEntity<RecipeListResponse> getPaginated(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(recipeService.findPaginated(page, size));
    }

    /**
     * Get a recipe by ID.
     *
     * @param id recipe ID
     * @return recipe details
     */
    @GetMapping("/{id}")
    public ResponseEntity<Recipe> getById(@PathVariable Long id) {
        return recipeService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get all steps for a specific recipe.
     * Steps are sorted by stepNumber in ascending order.
     *
     * @param recipeId the recipe ID (TheMealDB ID or internal recipe identifier)
     * @return list of steps for the recipe, sorted by stepNumber
     */
    @GetMapping("/{recipeId}/steps")
    public ResponseEntity<List<StepDTO>> getRecipeSteps(@PathVariable String recipeId) {
        List<StepDTO> steps = stepService.getStepsByRecipeId(recipeId);
        return ResponseEntity.ok(steps);
    }

    /**
     * Create a new recipe.
     *
     * @param request recipe creation request
     * @return created recipe
     */
    @PostMapping
    public ResponseEntity<Recipe> create(@Valid @RequestBody RecipeCreateRequest request) {
        Recipe saved = recipeService.save(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    /**
     * Update an existing recipe.
     *
     * @param id recipe ID
     * @param request recipe update request
     * @return updated recipe
     */
    @PutMapping("/{id}")
    public ResponseEntity<Recipe> update(
        @PathVariable Long id,
        @Valid @RequestBody RecipeUpdateRequest request) {
        Recipe updated = new Recipe(
            request.name(),
            request.description(),
            request.ingredients(),
            request.instructions(),
            request.preparationTimeMinutes()
        );
        return recipeService.update(id, updated)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Delete a recipe.
     *
     * @param id recipe ID
     * @return no content if successful
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (recipeService.deleteById(id)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
}

