package com.cookmate.simulator.controller;

import com.cookmate.simulator.dto.RecipeStepRequestDto;
import com.cookmate.simulator.dto.SimulationStatusResponseDto;
import com.cookmate.simulator.dto.SimulationStepHistoryItemDto;
import com.cookmate.simulator.dto.StartSimulationRequestDto;
import com.cookmate.simulator.dto.StepExecutionResultDto;
import com.cookmate.simulator.service.SimulationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/simulator")
@RequiredArgsConstructor
@Validated
@Tag(name = "Simulator", description = "One-click cooking simulation endpoints")
public class SimulatorController {

    private final SimulationService simulationService;

    @PostMapping("/sessions/start")
    @PreAuthorize("hasRole('ROLE_USER')")
    @Operation(summary = "Start simulation session", description = "Creates a new session and loads recipe steps from main-service.")
    @ApiResponse(responseCode = "201", description = "Session started", content = @Content(schema = @Schema(implementation = SimulationStatusResponseDto.class)))
    public ResponseEntity<SimulationStatusResponseDto> startSimulation(
            @Valid @RequestBody StartSimulationRequestDto request,
            @AuthenticationPrincipal Jwt jwt) {
        String userId = jwt != null ? jwt.getSubject() : null;
        return ResponseEntity.status(HttpStatus.CREATED).body(simulationService.startSession(request, userId));
    }

    @PostMapping("/sessions/{sessionId}/step")
    @PreAuthorize("hasRole('ROLE_USER')")
    @Operation(summary = "Execute specific step", description = "Fallback endpoint to execute a specific step payload.")
    @ApiResponse(responseCode = "200", description = "Step execution result", content = @Content(schema = @Schema(implementation = StepExecutionResultDto.class)))
    public ResponseEntity<StepExecutionResultDto> receiveStep(
            @Parameter(description = "Simulation session identifier") @PathVariable String sessionId,
            @Valid @RequestBody RecipeStepRequestDto stepDto
    ) {
        return ResponseEntity.ok(simulationService.processStep(sessionId, stepDto));
    }

    @PostMapping("/sessions/{sessionId}/steps/execute")
    @PreAuthorize("hasRole('ROLE_USER')")
    @Operation(summary = "Execute next step", description = "One-click endpoint to execute the next pending step in the session.")
    @ApiResponse(responseCode = "200", description = "Step execution result", content = @Content(schema = @Schema(implementation = StepExecutionResultDto.class)))
    public ResponseEntity<StepExecutionResultDto> executeNextStep(@PathVariable String sessionId) {
        return ResponseEntity.ok(simulationService.executeNextStep(sessionId));
    }

    @PostMapping("/sessions/{sessionId}/rewind")
    @PreAuthorize("hasRole('ROLE_USER')")
    @Operation(summary = "Rewind session", description = "Moves session progress back to the given step number.")
    @ApiResponse(responseCode = "200", description = "Session rewound", content = @Content(schema = @Schema(implementation = SimulationStatusResponseDto.class)))
    public ResponseEntity<SimulationStatusResponseDto> rewindToStep(
            @Parameter(description = "Simulation session identifier") @PathVariable String sessionId,
            @Parameter(description = "Target step number (0..totalSteps)")
            @RequestParam @Min(0) int stepNumber
    ) {
        return ResponseEntity.ok(simulationService.rewindToStep(sessionId, stepNumber));
    }

    @GetMapping("/sessions/{sessionId}/status")
    @Operation(summary = "Get session status", description = "Returns current progress and full step history for a session.")
    @ApiResponse(responseCode = "200", description = "Session status", content = @Content(schema = @Schema(implementation = SimulationStatusResponseDto.class)))
    public ResponseEntity<SimulationStatusResponseDto> getSimulationStatus(@PathVariable String sessionId) {
        return ResponseEntity.ok(simulationService.getStatus(sessionId));
    }

    @GetMapping("/sessions/{sessionId}/history")
    @Operation(summary = "Get session history", description = "Returns full ordered step history for a session.")
    @ApiResponse(responseCode = "200", description = "Step history")
    public ResponseEntity<List<SimulationStepHistoryItemDto>> history(@PathVariable String sessionId) {
        return ResponseEntity.ok(simulationService.history(sessionId));
    }

    @PostMapping("/sessions/{sessionId}/complete")
    @PreAuthorize("hasRole('ROLE_USER')")
    @Operation(summary = "Complete session manually", description = "Marks session as completed and notifies cooking-session-service.")
    public ResponseEntity<Void> completeSession(@PathVariable String sessionId) {
        simulationService.completeSession(sessionId);
        return ResponseEntity.ok().build();
    }
}
