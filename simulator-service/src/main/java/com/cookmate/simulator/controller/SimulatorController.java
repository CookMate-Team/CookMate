package com.cookmate.simulator.controller;

import com.cookmate.simulator.dto.HealthCheckResponseDto;
import com.cookmate.simulator.dto.MealPlanResponseDto;
import com.cookmate.simulator.dto.RecipeDto;
import com.cookmate.simulator.service.SimulationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/simulator")
public class SimulatorController {

    private final SimulationService simulationService;

    public SimulatorController(SimulationService simulationService) {
        this.simulationService = simulationService;
    }

    @GetMapping("/recipes")
    public ResponseEntity<List<RecipeDto>> listRecipes() {
        return ResponseEntity.ok(simulationService.listRecipes());
    }

    @GetMapping("/recipes/{id}")
    public ResponseEntity<RecipeDto> getRecipe(@PathVariable Long id) {
        return ResponseEntity.ok(simulationService.getRecipe(id));
    }

    @GetMapping("/meal-plan")
    public ResponseEntity<MealPlanResponseDto> generateMealPlan(@RequestParam(required = false) Integer days) {
        return ResponseEntity.ok(simulationService.generateMealPlan(days));
    }

    @GetMapping("/health-check")
    public ResponseEntity<HealthCheckResponseDto> serviceHealthCheck() {
        return ResponseEntity.ok(simulationService.serviceHealthCheck());
    }
}
