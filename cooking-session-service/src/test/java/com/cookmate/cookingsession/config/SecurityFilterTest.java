package com.cookmate.cookingsession.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

/**
 * Security filter tests for cooking-session-service (reactive/WebFlux).
 * Verifies that:
 * - Requests without a JWT token return 401 Unauthorized
 * - Public endpoints (actuator) are accessible without authentication
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@DisplayName("cooking-session-service — Security Filter Tests")
class SecurityFilterTest {

    private WebTestClient webTestClient;

    @LocalServerPort
    private int port;

    @BeforeEach
    void setUp() {
        webTestClient = WebTestClient.bindToServer()
                .baseUrl("http://localhost:" + port)
                .build();
    }

    // ─── 401 Unauthorized (no token) ───────────────────────────

    @Test
    @DisplayName("POST /api/cooking-sessions/progress without token → 401")
    void postProgress_withoutToken_returns401() {
        webTestClient.post()
                .uri("/api/cooking-sessions/progress")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"recipeId":"52772","stepNumber":1,"completed":true}
                        """)
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    @DisplayName("GET /api/cooking-sessions/recipes/xyz/history without token → 401")
    void getHistory_withoutToken_returns401() {
        webTestClient.get()
                .uri("/api/cooking-sessions/recipes/xyz/history")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    @DisplayName("POST /api/cooking-sessions/sessions/xyz/complete without token → 401")
    void completeSession_withoutToken_returns401() {
        webTestClient.post()
                .uri("/api/cooking-sessions/sessions/xyz/complete")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    // ─── Public endpoints ──────────────────────────────────────

    @Test
    @DisplayName("GET /actuator/health without token → 200")
    void actuatorHealth_isPublic() {
        webTestClient.get()
                .uri("/actuator/health")
                .exchange()
                .expectStatus().isOk();
    }
}
