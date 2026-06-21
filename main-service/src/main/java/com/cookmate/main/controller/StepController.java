package com.cookmate.main.controller;

import com.cookmate.main.dto.StepDTO;
import com.cookmate.main.dto.StepGenerationRequest;
import com.cookmate.main.dto.StepGenerationResponse;
import com.cookmate.main.dto.CustomStepGenerationRequest;
import com.cookmate.main.service.StepService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
    @PreAuthorize("hasRole('ROLE_USER')")
    public ResponseEntity<StepGenerationResponse> generateSteps(
            @Valid @RequestBody StepGenerationRequest request,
            HttpSession session) {
        StepGenerationResponse response = stepService.generateSteps(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Generuje podgląd kroków dla własnego przepisu użytkownika.
     * Używane w kreatorze przepisów we frontendzie.
     *
     * @param request instrukcje i składniki
     * @return wygenerowane kroki bez zapisu do bazy
     */
    @PostMapping("/generate-custom")
    @PreAuthorize("hasRole('ROLE_USER')")
    public ResponseEntity<StepGenerationResponse> generateCustomSteps(
            @Valid @RequestBody CustomStepGenerationRequest request) {
        StepGenerationResponse response = stepService.generateCustomStepsPreview(request);
        return ResponseEntity.ok(response);
    }
}
