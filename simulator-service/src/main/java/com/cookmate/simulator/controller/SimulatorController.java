package com.cookmate.simulator.controller;

import com.cookmate.simulator.dto.HealthCheckResponseDto;
import com.cookmate.simulator.dto.MealPlanResponseDto;
import com.cookmate.simulator.dto.RecipeDto;
import com.cookmate.simulator.dto.SimulationStatusResponseDto;
import com.cookmate.simulator.dto.SimulationStepHistoryItemDto;
import com.cookmate.simulator.service.SimulationService;
import org.springframework.http.HttpStatus;
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

    @PostMapping("/sessions/start")
    public ResponseEntity<SimulationStatusResponseDto> startSimulation(@RequestParam(required = false) Integer days) {
        return ResponseEntity.status(HttpStatus.CREATED).body(simulationService.start(days));
    }

    @GetMapping("/sessions/{sessionId}/status")
    public ResponseEntity<SimulationStatusResponseDto> getSimulationStatus(@PathVariable String sessionId) {
        return ResponseEntity.ok(simulationService.getStatus(sessionId));
    }

    @PostMapping("/sessions/{sessionId}/steps/execute")
    public ResponseEntity<SimulationStatusResponseDto> executeStep(@PathVariable String sessionId) {
        return ResponseEntity.ok(simulationService.executeStep(sessionId));
    }

    @PostMapping("/sessions/{sessionId}/complete")
    public ResponseEntity<SimulationStatusResponseDto> complete(@PathVariable String sessionId) {
        return ResponseEntity.ok(simulationService.complete(sessionId));
    }

    @PostMapping("/sessions/{sessionId}/pause")
    public ResponseEntity<SimulationStatusResponseDto> pause(@PathVariable String sessionId) {
        return ResponseEntity.ok(simulationService.pause(sessionId));
    }

    @PostMapping("/sessions/{sessionId}/resume")
    public ResponseEntity<SimulationStatusResponseDto> resume(@PathVariable String sessionId) {
        return ResponseEntity.ok(simulationService.resume(sessionId));
    }

    @PostMapping("/sessions/{sessionId}/cancel")
    public ResponseEntity<SimulationStatusResponseDto> cancel(@PathVariable String sessionId) {
        return ResponseEntity.ok(simulationService.cancel(sessionId));
    }

    @GetMapping("/sessions/{sessionId}/history")
    public ResponseEntity<List<SimulationStepHistoryItemDto>> history(@PathVariable String sessionId) {
        return ResponseEntity.ok(simulationService.history(sessionId));
    }
}
