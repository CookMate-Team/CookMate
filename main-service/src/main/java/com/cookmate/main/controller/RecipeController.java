package com.cookmate.main.controller;

import com.cookmate.main.dto.RecipeCreateRequest;
import com.cookmate.main.dto.RecipeDTO;
import com.cookmate.main.dto.RecipeListResponse;
import com.cookmate.main.dto.RecipeUpdateRequest;
import com.cookmate.main.model.Recipe;
import com.cookmate.main.service.RecipeService;
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

    /**
     * Construct RecipeController with recipe service.
     *
     * @param recipeService service for recipe operations
     */
    public RecipeController(RecipeService recipeService) {
        this.recipeService = recipeService;
    }

    /**
     * Get all recipes with optional search by name.
     *
     * @param name optional recipe name filter
     * @return list of recipes
     */
    @GetMapping
    public ResponseEntity<List<Recipe>> getAll(@RequestParam(required = false) String name) {
        if (name != null && !name.isBlank()) {
            return ResponseEntity.ok(recipeService.findByName(name));
        }
        return ResponseEntity.ok(recipeService.findAll());
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

