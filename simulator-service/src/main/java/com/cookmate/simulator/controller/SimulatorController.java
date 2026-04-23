package com.cookmate.simulator.controller;

import com.cookmate.simulator.dto.RecipeStepRequestDto;
import com.cookmate.simulator.dto.RecipeStepResponseDto;
import com.cookmate.simulator.dto.SimulationStatusResponseDto;
import com.cookmate.simulator.dto.SimulationStepHistoryItemDto;
import com.cookmate.simulator.service.SimulationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/simulator")
@RequiredArgsConstructor
public class SimulatorController {

    private final SimulationService simulationService;

    @PostMapping("/sessions/start")
    public ResponseEntity<SimulationStatusResponseDto> startSimulation() {
        return ResponseEntity.status(HttpStatus.CREATED).body(simulationService.startSession());
    }

    @PostMapping("/sessions/{sessionId}/step")
    public ResponseEntity<RecipeStepResponseDto> receiveStep(@PathVariable String sessionId, @RequestBody RecipeStepRequestDto stepDto) {
        return ResponseEntity.ok(simulationService.processStep(sessionId, stepDto));
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
