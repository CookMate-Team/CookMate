package com.cookmate.simulator.integration;

import com.cookmate.simulator.client.CookingSessionClient;
import com.cookmate.simulator.client.MainServiceClient;
import com.cookmate.simulator.dto.MainServiceStepDto;
import com.cookmate.simulator.repository.SimulationSessionRepository;
import com.cookmate.simulator.repository.SimulationStepRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

/**
 * End-to-end flow test dla guided cooking:
 * start → load step 1 → execute → load step 2 → execute → verify completion.
 *
 * Symuluje pełny cykl życia sesji gotowania.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Guided Cooking Flow — end-to-end")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class GuidedCookingFlowTest {

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

    @Test
    @DisplayName("Pełny flow: start → execute step 1 → execute step 2 → COMPLETED")
    void fullGuidedCookingFlow() throws Exception {
        // Arrange: main-service zwraca 2 kroki
        when(mainServiceClient.getRecipeSteps("52772")).thenReturn(List.of(
            new MainServiceStepDto(1L, 1, "Pokrój warzywa", null, 10, "52772"),
            new MainServiceStepDto(2L, 2, "Ugotuj w garnku", null, 20, "52772")
        ));

        // === 1. START — utwórz sesję ===
        MvcResult startResult = mockMvc.perform(post("/api/simulator/sessions/start")
                .header("Authorization", "Bearer mock-token")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER")))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"recipeId\":\"52772\"}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("RUNNING"))
            .andExpect(jsonPath("$.totalSteps").value(2))
            .andExpect(jsonPath("$.currentStep").value(0))
            .andReturn();

        String sessionId = extractSessionId(startResult);
        verify(mainServiceClient).getRecipeSteps("52772");

        // === 2. STATUS — sprawdź stan przed wykonaniem ===
        mockMvc.perform(get("/api/simulator/sessions/" + sessionId + "/status")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("RUNNING"))
            .andExpect(jsonPath("$.currentStep").value(0))
            .andExpect(jsonPath("$.history[0].status").value("PENDING"))
            .andExpect(jsonPath("$.history[1].status").value("PENDING"));

        // === 3. EXECUTE STEP 1 ===
        mockMvc.perform(post("/api/simulator/sessions/" + sessionId + "/steps/execute")
                .header("Authorization", "Bearer mock-token")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.stepNumber").value(1));

        // === 4. STATUS po kroku 1 — sprawdź postęp ===
        mockMvc.perform(get("/api/simulator/sessions/" + sessionId + "/status")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.currentStep").value(1))
            .andExpect(jsonPath("$.history[0].status").value("EXECUTED"))
            .andExpect(jsonPath("$.history[1].status").value("PENDING"));

        // === 5. EXECUTE STEP 2 ===
        mockMvc.perform(post("/api/simulator/sessions/" + sessionId + "/steps/execute")
                .header("Authorization", "Bearer mock-token")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.stepNumber").value(2));

        // === 6. STATUS — sesja powinna być COMPLETED ===
        mockMvc.perform(get("/api/simulator/sessions/" + sessionId + "/status")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("COMPLETED"))
            .andExpect(jsonPath("$.currentStep").value(2))
            .andExpect(jsonPath("$.history[0].status").value("EXECUTED"))
            .andExpect(jsonPath("$.history[1].status").value("EXECUTED"));

        // === 7. HISTORY — pełna historia ===
        mockMvc.perform(get("/api/simulator/sessions/" + sessionId + "/history")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(2)))
            .andExpect(jsonPath("$[0].stepNumber").value(1))
            .andExpect(jsonPath("$[0].status").value("EXECUTED"))
            .andExpect(jsonPath("$[1].stepNumber").value(2))
            .andExpect(jsonPath("$[1].status").value("EXECUTED"));
    }

    @Test
    @DisplayName("Flow z rewind: start → execute → rewind → re-execute → COMPLETED")
    void flowWithRewind() throws Exception {
        when(mainServiceClient.getRecipeSteps("52772")).thenReturn(List.of(
            new MainServiceStepDto(1L, 1, "Krok A", null, 5, "52772"),
            new MainServiceStepDto(2L, 2, "Krok B", null, 5, "52772")
        ));

        // Start
        String sessionId = extractSessionId(
            mockMvc.perform(post("/api/simulator/sessions/start")
                    .header("Authorization", "Bearer mock-token")
                    .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER")))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"recipeId\":\"52772\"}"))
                .andExpect(status().isCreated())
                .andReturn()
        );

        // Execute step 1
        mockMvc.perform(post("/api/simulator/sessions/" + sessionId + "/steps/execute")
                .header("Authorization", "Bearer mock-token")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.stepNumber").value(1));

        // Execute step 2
        mockMvc.perform(post("/api/simulator/sessions/" + sessionId + "/steps/execute")
                .header("Authorization", "Bearer mock-token")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.stepNumber").value(2));

        // Verify COMPLETED
        mockMvc.perform(get("/api/simulator/sessions/" + sessionId + "/status")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"))))
            .andExpect(jsonPath("$.status").value("COMPLETED"));

        // REWIND to step 1
        mockMvc.perform(post("/api/simulator/sessions/" + sessionId + "/rewind?stepNumber=1")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("RUNNING"))
            .andExpect(jsonPath("$.currentStep").value(1));

        // Re-execute step 2
        mockMvc.perform(post("/api/simulator/sessions/" + sessionId + "/steps/execute")
                .header("Authorization", "Bearer mock-token")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.stepNumber").value(2));

        // Verify COMPLETED again
        mockMvc.perform(get("/api/simulator/sessions/" + sessionId + "/status")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"))))
            .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    @DisplayName("Flow z processStep: start → processStep(1) → processStep(2) → COMPLETED")
    void flowWithProcessStep() throws Exception {
        when(mainServiceClient.getRecipeSteps("52772")).thenReturn(List.of(
            new MainServiceStepDto(1L, 1, "Krok 1", null, 5, "52772"),
            new MainServiceStepDto(2L, 2, "Krok 2", null, 5, "52772")
        ));

        String sessionId = extractSessionId(
            mockMvc.perform(post("/api/simulator/sessions/start")
                    .header("Authorization", "Bearer mock-token")
                    .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER")))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"recipeId\":\"52772\"}"))
                .andExpect(status().isCreated())
                .andReturn()
        );

        // processStep 1
        mockMvc.perform(post("/api/simulator/sessions/" + sessionId + "/step")
                .header("Authorization", "Bearer mock-token")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER")))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"stepNumber\":1,\"description\":\"Exec 1\",\"durationSeconds\":5}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));

        // processStep 2
        mockMvc.perform(post("/api/simulator/sessions/" + sessionId + "/step")
                .header("Authorization", "Bearer mock-token")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER")))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"stepNumber\":2,\"description\":\"Exec 2\",\"durationSeconds\":5}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));

        // Verify COMPLETED
        mockMvc.perform(get("/api/simulator/sessions/" + sessionId + "/status")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"))))
            .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    @DisplayName("Error flow: operacja na nieistniejącej sesji → 404")
    void errorFlow_sessionNotFound() throws Exception {
        mockMvc.perform(post("/api/simulator/sessions/fake-id/steps/execute")
                .header("Authorization", "Bearer mock-token")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"))))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("SIMULATION_NOT_FOUND"));

        mockMvc.perform(get("/api/simulator/sessions/fake-id/status")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"))))
            .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/simulator/sessions/fake-id/history")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"))))
            .andExpect(status().isNotFound());
    }

    private String extractSessionId(MvcResult result) throws Exception {
        String json = result.getResponse().getContentAsString();
        JsonNode node = objectMapper.readTree(json);
        return node.get("sessionId").asText();
    }
}
