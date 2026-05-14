package com.cookmate.main.controller;

import com.cookmate.main.dto.StepCompletionEventDto;
import com.cookmate.main.model.SimulationProgress;
import com.cookmate.main.service.SimulationProgressService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/simulation-progress")
@RequiredArgsConstructor
@Tag(name = "Simulation Progress", description = "Endpoints to track simulation progress from simulator-service")
public class SimulationProgressController {

    private final SimulationProgressService simulationProgressService;

    /**
     * Odbiera event wykonania kroku z симуlatora.
     * Zapisuje go w bazie danych dla celów śledzenia postępu.
     *
     * @param event event zawierający dane o wykonanym kroku
     * @return saved progress with 201 Created status
     */
    @PostMapping
    @Operation(summary = "Receive step completion event", description = "Receives step completion event from simulator-service and saves it for tracking.")
    @ApiResponse(responseCode = "201", description = "Event saved", content = @Content(schema = @Schema(implementation = SimulationProgress.class)))
    @ApiResponse(responseCode = "400", description = "Invalid event data")
    public ResponseEntity<SimulationProgress> receiveStepCompletionEvent(
            @Valid @RequestBody StepCompletionEventDto event
    ) {
        SimulationProgress saved = simulationProgressService.handleStepCompletionEvent(event);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    /**
     * Pobiera historię wszystkich kroków dla danej sesji symulacji.
     *
     * @param sessionId ID sesji symulacji z симуlatora
     * @return lista kroków posortowana rosnąco po numerze
     */
    @GetMapping("/sessions/{sessionId}")
    @Operation(summary = "Get session progress history", description = "Returns all steps executed in a simulation session, sorted by step number.")
    @ApiResponse(responseCode = "200", description = "Session history", content = @Content(schema = @Schema(implementation = SimulationProgress[].class)))
    public ResponseEntity<List<SimulationProgress>> getSessionHistory(
            @Parameter(description = "Simulation session identifier from simulator-service")
            @PathVariable String sessionId
    ) {
        List<SimulationProgress> history = simulationProgressService.getSessionHistory(sessionId);
        return ResponseEntity.ok(history);
    }

    /**
     * Pobiera aktualny stan sesji (najnowszy wykonany krok).
     *
     * @param sessionId ID sesji symulacji z симуlatora
     * @return ostatnio wykonany krok
     */
    @GetMapping("/sessions/{sessionId}/latest")
    @Operation(summary = "Get latest step progress", description = "Returns the most recent step executed in a simulation session.")
    @ApiResponse(responseCode = "200", description = "Latest step progress")
    public ResponseEntity<SimulationProgress> getLatestProgress(
            @Parameter(description = "Simulation session identifier from simulator-service")
            @PathVariable String sessionId
    ) {
        SimulationProgress latest = simulationProgressService.getLatestProgress(sessionId);
        return latest != null ? ResponseEntity.ok(latest) : ResponseEntity.notFound().build();
    }

    /**
     * Pobiera wszystkie eventy dla danego przepisu (ze wszystkich sesji).
     *
     * @param recipeId ID przepisu
     * @return lista eventów posortowana malejąco po dacie
     */
    @GetMapping("/recipes/{recipeId}")
    @Operation(summary = "Get recipe progress across all sessions", description = "Returns all step completion events for a specific recipe across all simulation sessions.")
    @ApiResponse(responseCode = "200", description = "Recipe progress events")
    public ResponseEntity<List<SimulationProgress>> getRecipeProgress(
            @Parameter(description = "Recipe identifier")
            @PathVariable String recipeId
    ) {
        List<SimulationProgress> progress = simulationProgressService.getRecipeProgress(recipeId);
        return ResponseEntity.ok(progress);
    }
}
