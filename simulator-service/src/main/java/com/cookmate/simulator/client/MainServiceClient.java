package com.cookmate.simulator.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

import com.cookmate.simulator.dto.RecipeStepResponseDto;

@FeignClient(name = "main-service")
public interface MainServiceClient {

    @PostMapping("/api/cooking-sessions/{sessionId}/step-completed")
    void reportStepCompletion(@PathVariable("sessionId") String sessionId, @RequestBody RecipeStepResponseDto stepResponse);
}
