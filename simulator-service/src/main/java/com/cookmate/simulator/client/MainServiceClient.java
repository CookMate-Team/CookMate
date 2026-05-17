package com.cookmate.simulator.client;

import com.cookmate.simulator.dto.MainServiceStepDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;
import java.util.Map;

@FeignClient(name = "main-service")
public interface MainServiceClient {

    @GetMapping("/api/recipes/{recipeId}/steps")
    List<MainServiceStepDto> getRecipeSteps(@PathVariable("recipeId") String recipeId);

    /**
     * Wysyła notyfikację o wykonanym kroku do main-service.
     * Event zawiera: sessionId, stepNumber, status, executedAt, recipeId
     *
     * @param event mapa zawierająca dane o wykonanym kroku
     */
    @PostMapping("/api/simulation-progress")
    void notifyStepCompleted(@RequestBody Map<String, Object> event);
}
