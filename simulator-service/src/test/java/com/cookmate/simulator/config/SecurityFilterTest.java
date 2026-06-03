package com.cookmate.simulator.config;

import com.cookmate.simulator.client.CookingSessionClient;
import com.cookmate.simulator.client.MainServiceClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Security filter tests for simulator-service.
 * Verifies that:
 * - Requests without a JWT token return 401 Unauthorized
 * - Public endpoints (actuator, swagger) are accessible without authentication
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("simulator-service — Security Filter Tests")
class SecurityFilterTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @MockitoBean
    private MainServiceClient mainServiceClient;

    @MockitoBean
    private CookingSessionClient cookingSessionClient;

    // ─── 401 Unauthorized (no token) ───────────────────────────

    @Test
    @DisplayName("POST /api/simulator/sessions/start without token → 401")
    void startSession_withoutToken_returns401() throws Exception {
        mockMvc.perform(post("/api/simulator/sessions/start")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"recipeId":"52772"}
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/simulator/sessions/xyz/status without token → 401")
    void getStatus_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/simulator/sessions/xyz/status"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /api/simulator/sessions/xyz/steps/execute without token → 401")
    void executeStep_withoutToken_returns401() throws Exception {
        mockMvc.perform(post("/api/simulator/sessions/xyz/steps/execute"))
                .andExpect(status().isUnauthorized());
    }

    // ─── Public endpoints ──────────────────────────────────────

    @Test
    @DisplayName("GET /actuator/health without token → 200")
    void actuatorHealth_isPublic() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /swagger-ui.html without token → accessible")
    void swaggerUi_isPublic() throws Exception {
        mockMvc.perform(get("/swagger-ui.html"))
                .andExpect(status().is3xxRedirection());
    }
}
