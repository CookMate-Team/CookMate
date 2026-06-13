package com.cookmate.mealplanner.controller;

import com.cookmate.mealplanner.dto.ShoppingListRequest;
import com.cookmate.mealplanner.dto.ShoppingListResponse;
import com.cookmate.mealplanner.dto.WeeklyPlanResponse;
import com.cookmate.mealplanner.service.MealPlanService;
import com.cookmate.mealplanner.service.ShoppingListService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/planner")
@RequiredArgsConstructor
@Validated
@Tag(name = "Meal Planner", description = "Weekly meal plan generation endpoints")
public class MealPlanController {

    private final MealPlanService mealPlanService;
    private final ShoppingListService shoppingListService;

    @GetMapping("/weekly-plan")
    @Operation(summary = "Generate weekly meal plan",
               description = "Returns a 7-day meal plan with n meals per day from TheMealDB.")
    @ApiResponse(responseCode = "200", description = "Weekly plan generated",
                 content = @Content(schema = @Schema(implementation = WeeklyPlanResponse.class)))
    public ResponseEntity<WeeklyPlanResponse> getWeeklyPlan(@RequestParam @Min(1) @Max(5) int mealsPerDay) {
        return ResponseEntity.ok(mealPlanService.generateWeeklyPlan(mealsPerDay));
    }

    @PostMapping("/shopping-list")
    @Operation(summary = "Build shopping list",
               description = "Returns a deduplicated shopping list for the given meal IDs.")
    @ApiResponse(responseCode = "200", description = "Shopping list generated",
                 content = @Content(schema = @Schema(implementation = ShoppingListResponse.class)))
    public ResponseEntity<ShoppingListResponse> getShoppingList(@RequestBody ShoppingListRequest request) {
        return ResponseEntity.ok(shoppingListService.buildShoppingList(request));
    }
}
