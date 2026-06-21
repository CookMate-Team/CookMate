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
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for recipe management endpoints.
 * Provides CRUD operations and search capabilities for recipes.
 */
@RestController
@RequestMapping("/api/recipes")
@Validated
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
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size,
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
        @RequestParam(defaultValue = "0") @Min(0) int page,
        @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size) {
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
        return ResponseEntity.ok(recipeService.findByIdOrThrow(id));
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
     * Get all custom recipes for the authenticated user.
     *
     * @param jwt authenticated user token
     * @param page page number
     * @param size page size
     * @return paginated list of user's custom recipes
     */
    @GetMapping("/my")
    @PreAuthorize("hasRole('ROLE_USER')")
    public ResponseEntity<RecipeListResponse> getMyRecipes(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size) {
        String userId = jwt.getSubject();
        return ResponseEntity.ok(recipeService.findMyRecipesPaginated(userId, page, size));
    }

    /**
     * Create a new custom recipe.
     *
     * @param request recipe creation request
     * @param jwt authenticated user token
     * @return created recipe
     */
    @PostMapping
    @PreAuthorize("hasRole('ROLE_USER')")
    public ResponseEntity<Recipe> create(@Valid @RequestBody RecipeCreateRequest request, @AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getSubject();
        Recipe saved = recipeService.saveCustom(request, userId);
        if (request.steps() != null && !request.steps().isEmpty()) {
            stepService.saveCustomSteps(saved.getId().toString(), request.steps());
        } else {
            stepService.generateAndSaveStepsForCustomRecipeAsync(saved);
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    /**
     * Update an existing recipe.
     *
     * @param id recipe ID
     * @param request recipe update request
     * @param jwt authenticated user token
     * @return updated recipe
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_USER')")
    public ResponseEntity<Recipe> update(
        @PathVariable Long id,
        @Valid @RequestBody RecipeUpdateRequest request,
        @AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getSubject();
        Recipe updated = new Recipe(
            request.name(),
            request.description(),
            request.ingredients(),
            request.instructions(),
            request.preparationTimeMinutes(),
            userId
        );
        Recipe saved = recipeService.updateCustomOrThrow(id, updated, userId);
        if (request.steps() != null && !request.steps().isEmpty()) {
            stepService.saveCustomSteps(saved.getId().toString(), request.steps());
        } else {
            // Skasuj stare kroki i wygeneruj nowe asynchronicznie w tle
            stepService.deleteStepsByRecipeId(saved.getId().toString());
            stepService.generateAndSaveStepsForCustomRecipeAsync(saved);
        }
        return ResponseEntity.ok(saved);
    }

    /**
     * Delete a recipe.
     *
     * @param id recipe ID
     * @param jwt authenticated user token
     * @return no content if successful
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_USER')")
    public ResponseEntity<Void> delete(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getSubject();
        recipeService.deleteCustomByIdOrThrow(id, userId);
        stepService.deleteStepsByRecipeId(String.valueOf(id));
        return ResponseEntity.noContent().build();
    }
}

