package com.cookmate.simulator.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.Map;

@FeignClient(
        name = "cooking-session-service",
        url = "${cooking-session-service.url:http://cooking-session-service:8083}"
)
public interface CookingSessionClient {

    /**
     * Wysyła notyfikację o wykonanym kroku do cooking-session-service.
     * Event zawiera: sessionId, stepNumber, status, executedAt, recipeId
     *
     * @param authHeader nagłówek autoryzacji JWT
     * @param event mapa zawierająca dane o wykonanym kroku
     */
    @PostMapping("/api/cooking-sessions/progress")
    void notifyStepCompleted(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody Map<String, Object> event
    );
}
