package com.cookmate.main.controller;

import com.cookmate.main.dto.StepDTO;
import com.cookmate.main.service.StepService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/steps")
public class StepController {

    private final StepService stepService;

    public StepController(StepService stepService) {
        this.stepService = stepService;
    }

    @GetMapping("/{stepId}")
    public ResponseEntity<StepDTO> getStep(@PathVariable Long stepId) {
        return ResponseEntity.ok(stepService.getStep(stepId));
    }
}
