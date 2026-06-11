package com.cookmate.simulator.controller;

import com.cookmate.simulator.client.CookingSessionClient;
import com.cookmate.simulator.client.MainServiceClient;
import com.cookmate.simulator.dto.MainServiceStepDto;
import com.cookmate.simulator.repository.SimulationSessionRepository;
import com.cookmate.simulator.repository.SimulationStepRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration test dla SimulatorController — pełny kontekst Spring z H2.
 * MainServiceClient jest mockowany aby nie łączyć się z main-service.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("SimulatorController — integration tests")
class SimulationControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private SimulationSessionRepository sessionRepo;
    @Autowired private SimulationStepRepository stepRepo;

    @MockitoBean
    private MainServiceClient mainServiceClient;

    @MockitoBean
    private CookingSessionClient cookingSessionClient;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @BeforeEach
    void cleanDb() {
        stepRepo.deleteAll();
        sessionRepo.deleteAll();

        org.springframework.security.oauth2.jwt.Jwt mockJwt = org.mockito.Mockito.mock(org.springframework.security.oauth2.jwt.Jwt.class);
        org.mockito.Mockito.when(mockJwt.getSubject()).thenReturn("user");
        org.mockito.Mockito.when(mockJwt.getClaimAsString("preferred_username")).thenReturn("test.user");
        org.mockito.Mockito.when(mockJwt.getClaimAsMap("realm_access")).thenReturn(java.util.Map.of("roles", java.util.List.of("ROLE_USER")));
        org.mockito.Mockito.when(jwtDecoder.decode(org.mockito.Mockito.anyString())).thenReturn(mockJwt);
    }

    // --- POST /api/simulator/sessions/start ---

    @Test
    @DisplayName("POST /sessions/start — tworzy sesję 201")
    void startSimulation_returns201() throws Exception {
        mockMainSteps("52772");

        mockMvc.perform(post("/api/simulator/sessions/start")
                .header("Authorization", "Bearer mock-token")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER")))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"recipeId\":\"52772\"}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.sessionId").isNotEmpty())
            .andExpect(jsonPath("$.status").value("RUNNING"))
            .andExpect(jsonPath("$.totalSteps").value(2))
            .andExpect(jsonPath("$.history", hasSize(2)));
    }

    @Test
    @DisplayName("POST /sessions/start — 400 gdy brak recipeId")
    void startSimulation_returns400WhenNoRecipeId() throws Exception {
        mockMvc.perform(post("/api/simulator/sessions/start")
                .header("Authorization", "Bearer mock-token")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER")))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"recipeId\":\"\"}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /sessions/start — 503 gdy main-service niedostępny")
    void startSimulation_returns503WhenMainDown() throws Exception {
        when(mainServiceClient.getRecipeSteps("52772"))
            .thenThrow(new RuntimeException("Connection refused"));

        mockMvc.perform(post("/api/simulator/sessions/start")
                .header("Authorization", "Bearer mock-token")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER")))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"recipeId\":\"52772\"}"))
            .andExpect(status().isServiceUnavailable())
            .andExpect(jsonPath("$.code").value("MAIN_SERVICE_UNAVAILABLE"));
    }

    // --- POST /api/simulator/sessions/{id}/steps/execute ---

    @Test
    @DisplayName("POST /sessions/{id}/steps/execute — wykonuje krok")
    void executeNextStep_returns200() throws Exception {
        String sessionId = createSession("52772");

        mockMvc.perform(post("/api/simulator/sessions/" + sessionId + "/steps/execute")
                .header("Authorization", "Bearer mock-token")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.stepNumber").value(1));
    }

    @Test
    @DisplayName("POST /sessions/{id}/steps/execute — 404 dla nieistniejącej sesji")
    void executeNextStep_returns404() throws Exception {
        mockMvc.perform(post("/api/simulator/sessions/nonexistent/steps/execute")
                .header("Authorization", "Bearer mock-token")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"))))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("SIMULATION_NOT_FOUND"));
    }

    // --- POST /api/simulator/sessions/{id}/step ---

    @Test
    @DisplayName("POST /sessions/{id}/step — przetwarza krok z payloadem")
    void receiveStep_returns200() throws Exception {
        String sessionId = createSession("52772");
        String body = """
            {"stepNumber":1,"description":"Heat pan","durationSeconds":30}
            """;

        mockMvc.perform(post("/api/simulator/sessions/" + sessionId + "/step")
                .header("Authorization", "Bearer mock-token")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("POST /sessions/{id}/step — 400 przy nieprawidłowym payloadzie")
    void receiveStep_returns400OnInvalidPayload() throws Exception {
        String sessionId = createSession("52772");

        mockMvc.perform(post("/api/simulator/sessions/" + sessionId + "/step")
                .header("Authorization", "Bearer mock-token")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER")))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"stepNumber\":null}"))
            .andExpect(status().isBadRequest());
    }

    // --- GET /api/simulator/sessions/{id}/status ---

    @Test
    @DisplayName("GET /sessions/{id}/status — zwraca status sesji")
    void getStatus_returns200() throws Exception {
        String sessionId = createSession("52772");

        mockMvc.perform(get("/api/simulator/sessions/" + sessionId + "/status")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.sessionId").value(sessionId))
            .andExpect(jsonPath("$.status").value("RUNNING"));
    }

    @Test
    @DisplayName("GET /sessions/{id}/status — 404 dla nieistniejącej sesji")
    void getStatus_returns404() throws Exception {
        mockMvc.perform(get("/api/simulator/sessions/fake/status")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"))))
            .andExpect(status().isNotFound());
    }

    // --- GET /api/simulator/sessions/{id}/history ---

    @Test
    @DisplayName("GET /sessions/{id}/history — zwraca historię kroków")
    void getHistory_returns200() throws Exception {
        String sessionId = createSession("52772");

        mockMvc.perform(get("/api/simulator/sessions/" + sessionId + "/history")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(2)))
            .andExpect(jsonPath("$[0].status").value("PENDING"));
    }

    // --- POST /api/simulator/sessions/{id}/rewind ---

    @Test
    @DisplayName("POST /sessions/{id}/rewind — cofa sesję do kroku")
    void rewind_returns200() throws Exception {
        String sessionId = createSession("52772");
        // Najpierw wykonaj krok
        mockMvc.perform(post("/api/simulator/sessions/" + sessionId + "/steps/execute")
                .header("Authorization", "Bearer mock-token")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"))));

        mockMvc.perform(post("/api/simulator/sessions/" + sessionId + "/rewind?stepNumber=0")
                .header("Authorization", "Bearer mock-token")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.currentStep").value(0))
            .andExpect(jsonPath("$.status").value("RUNNING"));
    }

    // --- Helpers ---

    private void mockMainSteps(String recipeId) {
        when(mainServiceClient.getRecipeSteps(recipeId)).thenReturn(List.of(
            new MainServiceStepDto(1L, 1, "Pokrój cebulę", null, 5, recipeId),
            new MainServiceStepDto(2L, 2, "Podsmaż na oleju", null, 10, recipeId)
        ));
    }

    private String createSession(String recipeId) throws Exception {
        mockMainSteps(recipeId);
        MvcResult result = mockMvc.perform(post("/api/simulator/sessions/start")
                .header("Authorization", "Bearer mock-token")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER")))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"recipeId\":\"" + recipeId + "\"}"))
            .andExpect(status().isCreated())
            .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("sessionId").asText();
    }
}
