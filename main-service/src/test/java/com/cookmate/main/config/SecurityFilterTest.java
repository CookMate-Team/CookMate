package com.cookmate.main.config;

import com.cookmate.main.service.MealDbClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Security filter tests for main-service.
 * Verifies that:
 * - Requests without a JWT token return 401 Unauthorized
 * - Requests with insufficient roles return 403 Forbidden
 * - Public endpoints (actuator) are accessible without authentication
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("main-service — Security Filter Tests")
class SecurityFilterTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @MockitoBean
    private MealDbClient mealDbClient;

    // ─── 401 Unauthorized (no token) ───────────────────────────

    @Test
    @DisplayName("GET /api/recipes without token → 401")
    void getRecipes_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/recipes"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /api/recipes without token → 401")
    void postRecipe_withoutToken_returns401() throws Exception {
        mockMvc.perform(post("/api/recipes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Test","description":"d","ingredients":"i","instructions":"x","preparationTimeMinutes":10}
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("DELETE /api/recipes/1 without token → 401")
    void deleteRecipe_withoutToken_returns401() throws Exception {
        mockMvc.perform(delete("/api/recipes/{id}", 1L))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /api/steps/generate without token → 401")
    void generateSteps_withoutToken_returns401() throws Exception {
        mockMvc.perform(post("/api/steps/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"mealId":"52772"}
                                """))
                .andExpect(status().isUnauthorized());
    }

    // ─── 403 Forbidden (wrong role) ────────────────────────────

    @Test
    @DisplayName("DELETE /api/recipes/1 as ROLE_USER (not ADMIN) → 403")
    void deleteRecipe_asUser_returns403() throws Exception {
        mockMvc.perform(delete("/api/recipes/{id}", 1L)
                        .with(jwt().authorities(List.of(
                                new SimpleGrantedAuthority("ROLE_USER")
                        ))))
                .andExpect(status().isForbidden());
    }

    // ─── Public endpoints ──────────────────────────────────────

    @Test
    @DisplayName("GET /actuator/health without token → 200")
    void actuatorHealth_isPublic() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }
}
