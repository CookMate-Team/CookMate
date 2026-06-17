package com.cookmate.mealplanner.controller;

import com.cookmate.mealplanner.dto.SavedShoppingListResponse;
import com.cookmate.mealplanner.dto.SavedWeeklyPlanResponse;
import com.cookmate.mealplanner.dto.ShoppingListRequest;
import com.cookmate.mealplanner.dto.ShoppingListResponse;
import com.cookmate.mealplanner.dto.WeeklyPlanResponse;
import com.cookmate.mealplanner.service.MealPlanService;
import com.cookmate.mealplanner.service.ShoppingListPersistenceService;
import com.cookmate.mealplanner.service.ShoppingListService;
import com.cookmate.mealplanner.service.WeeklyPlanPersistenceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/planner")
@RequiredArgsConstructor
@Validated
@Tag(name = "Meal Planner", description = "Weekly meal plan generation and persistence endpoints")
public class MealPlanController {

    private final MealPlanService mealPlanService;
    private final ShoppingListService shoppingListService;
    private final WeeklyPlanPersistenceService weeklyPlanPersistenceService;
    private final ShoppingListPersistenceService shoppingListPersistenceService;

    @GetMapping("/weekly-plan")
    @Operation(summary = "Generate weekly meal plan",
               description = "Returns a 7-day meal plan with n meals per day from TheMealDB.")
    @ApiResponse(responseCode = "200", description = "Weekly plan generated",
                 content = @Content(schema = @Schema(implementation = WeeklyPlanResponse.class)))
    public ResponseEntity<WeeklyPlanResponse> getWeeklyPlan(@RequestParam @Min(1) @Max(5) int mealsPerDay) {
        return ResponseEntity.ok(mealPlanService.generateWeeklyPlan(mealsPerDay));
    }

    @PostMapping("/weekly-plan/save")
    @Operation(summary = "Save a weekly meal plan",
               description = "Persists a previously generated weekly plan for the authenticated user.")
    @ApiResponse(responseCode = "200", description = "Plan saved",
                 content = @Content(schema = @Schema(implementation = SavedWeeklyPlanResponse.class)))
    public ResponseEntity<SavedWeeklyPlanResponse> saveWeeklyPlan(
            @RequestBody WeeklyPlanResponse request,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(weeklyPlanPersistenceService.save(jwt.getSubject(), request));
    }

    @GetMapping("/weekly-plan/history")
    @Operation(summary = "Get weekly plan history",
               description = "Returns all saved weekly plans for the authenticated user, newest first.")
    @ApiResponse(responseCode = "200", description = "History returned")
    public ResponseEntity<List<SavedWeeklyPlanResponse>> getWeeklyPlanHistory(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(weeklyPlanPersistenceService.getHistory(jwt.getSubject()));
    }

    @PostMapping("/shopping-list")
    @Operation(summary = "Build shopping list",
               description = "Returns a deduplicated shopping list for the given meal IDs.")
    @ApiResponse(responseCode = "200", description = "Shopping list generated",
                 content = @Content(schema = @Schema(implementation = ShoppingListResponse.class)))
    public ResponseEntity<ShoppingListResponse> getShoppingList(@RequestBody ShoppingListRequest request) {
        return ResponseEntity.ok(shoppingListService.buildShoppingList(request));
    }

    @PostMapping("/shopping-list/save")
    @Operation(summary = "Save a shopping list",
               description = "Persists a previously generated shopping list for the authenticated user.")
    @ApiResponse(responseCode = "200", description = "Shopping list saved",
                 content = @Content(schema = @Schema(implementation = SavedShoppingListResponse.class)))
    public ResponseEntity<SavedShoppingListResponse> saveShoppingList(
            @RequestBody ShoppingListResponse request,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(shoppingListPersistenceService.save(jwt.getSubject(), request));
    }

    @GetMapping("/shopping-list/history")
    @Operation(summary = "Get shopping list history",
               description = "Returns all saved shopping lists for the authenticated user, newest first.")
    @ApiResponse(responseCode = "200", description = "History returned")
    public ResponseEntity<List<SavedShoppingListResponse>> getShoppingListHistory(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(shoppingListPersistenceService.getHistory(jwt.getSubject()));
    }
}
