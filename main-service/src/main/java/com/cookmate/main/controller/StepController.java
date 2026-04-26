package com.cookmate.main.controller;

import com.cookmate.main.dto.StepDTO;
import com.cookmate.main.dto.StepGenerationRequest;
import com.cookmate.main.dto.StepGenerationResponse;
import com.cookmate.main.service.StepService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/steps")
public class StepController {

    private final StepService stepService;

    public StepController(StepService stepService) {
        this.stepService = stepService;
    }

    @GetMapping("/{stepId}")
    public ResponseEntity<StepDTO> getStep(@PathVariable Long stepId) {
        return ResponseEntity.ok(stepService.getStep(stepId));
    }

    /**
     * Generuje kroki do przepisu z TheMealDB.
     * Jeśli kroki już istnieją dla danego mealId, zwraca istniejące.
     *
     * @param request żądanie zawierające mealId
     * @param session sesja HTTP z metadanymi przepisu
     * @return response z listą kroków
     */
    @PostMapping("/generate")
    public ResponseEntity<StepGenerationResponse> generateSteps(
            @Valid @RequestBody StepGenerationRequest request,
            HttpSession session) {
        StepGenerationResponse response = stepService.generateSteps(request, session);
        return ResponseEntity.ok(response);
    }
}
