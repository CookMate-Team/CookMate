package com.cookmate.simulator.client;

import feign.FeignException;
import feign.Request;
import feign.RequestTemplate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;

@ExtendWith(MockitoExtension.class)
@DisplayName("CookingSessionClient — komunikacja z cooking-session-service")
class CookingSessionClientTest {

    @Mock
    private CookingSessionClient cookingSessionClient;

    @Test
    @DisplayName("notifyStepCompleted — wywołuje POST z poprawnymi danymi")
    void notifyStepCompleted_sendsCorrectData() {
        Map<String, Object> event = Map.of(
            "sessionId", "s-123",
            "stepNumber", 1,
            "status", "EXECUTED",
            "executedAt", "2026-05-18T12:00:00",
            "recipeId", "r-42"
        );
        doNothing().when(cookingSessionClient).notifyStepCompleted(event);

        cookingSessionClient.notifyStepCompleted(event);
    }

    @Test
    @DisplayName("notifyStepCompleted — rzuca wyjątek przy błędzie serwera")
    void notifyStepCompleted_throwsOnServerError() {
        Request feignRequest = Request.create(
            Request.HttpMethod.POST, "/api/cooking-sessions/progress",
            Collections.emptyMap(), null, new RequestTemplate()
        );
        doThrow(new FeignException.InternalServerError("500", feignRequest, null, null))
            .when(cookingSessionClient).notifyStepCompleted(Map.of());

        assertThrows(FeignException.InternalServerError.class,
            () -> cookingSessionClient.notifyStepCompleted(Map.of()));
    }
}
