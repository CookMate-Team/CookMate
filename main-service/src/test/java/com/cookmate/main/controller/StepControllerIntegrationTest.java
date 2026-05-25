package com.cookmate.main.controller;

import com.cookmate.main.dto.LLMResponseDTO;
import com.cookmate.main.dto.LLMStepDTO;
import com.cookmate.main.dto.MealSearchResponse;
import com.cookmate.main.dto.Meal;
import com.cookmate.main.model.ActionType;
import com.cookmate.main.repository.StepRepository;
import com.cookmate.main.service.GroqClient;
import com.cookmate.main.service.MealDbClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for {@link StepController} – POST /api/steps/generate endpoint.
 *
 * <p>The full Spring context is started with an H2 in-memory database.
 * External HTTP clients ({@link MealDbClient} and {@link GroqClient}) are replaced
 * by Mockito mocks so no real network calls are made.</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class StepControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private StepRepository stepRepository;

    @MockBean
    private MealDbClient mealDbClient;

    @MockBean
    private GroqClient groqClient;

    // -----------------------------------------------------------------------
    // Shared test fixtures
    // -----------------------------------------------------------------------

    private static final String MEAL_ID = "52772";
    private static final String MEAL_NAME = "Teriyaki Chicken Casserole";
    private static final String MEAL_INSTRUCTIONS =
            "Mix soy sauce, mirin and sugar. Add chicken and marinate for 30 minutes.";

    private Meal buildMeal() {
        // Meal record: idMeal, strMeal, strMealAlternate, strCategory, strArea,
        //              strInstructions, strMealThumb, strTags, strYoutube, strSource,
        //              strImageSource, strCreativeCommonsConfirmed, dateModified,
        //              strIngredient1..20, strMeasure1..20
        return new Meal(
                MEAL_ID, MEAL_NAME, null, "Chicken", "Japanese",
                MEAL_INSTRUCTIONS, null, null, null, null,
                null, null, null,
                "Soy Sauce", null, "Mirin", null, "Sugar", null, null, null,
                null, null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null,
                null, null, null
        );
    }

    private LLMResponseDTO buildLlmResponse() {
        LLMStepDTO step1 = new LLMStepDTO(
                1, "Marinate chicken with soy sauce and mirin in the CookMate bowl.",
                ActionType.MARINATE, "chicken", 30, Map.of("temperature", 0, "speed", 0)
        );
        LLMStepDTO step2 = new LLMStepDTO(
                2, "Set the device to FRYING_PAN mode at 180°C and cook the marinated chicken.",
                ActionType.FRYING_PAN, "chicken", 15, Map.of("temperature", 180, "speed", 3)
        );
        return new LLMResponseDTO(List.of(step1, step2));
    }

    @BeforeEach
    void setUp() {
        stepRepository.deleteAll();
    }

    // -----------------------------------------------------------------------
    // Test: generateSteps succeeds – happy path
    // -----------------------------------------------------------------------

    @Test
    void generateSteps_shouldReturn200WithStepsOnSuccess() throws Exception {
        when(mealDbClient.lookupById(MEAL_ID))
                .thenReturn(Mono.just(new MealSearchResponse(List.of(buildMeal()))));
        when(groqClient.generateSteps(MEAL_INSTRUCTIONS))
                .thenReturn(Mono.just(buildLlmResponse()));

        String requestBody = objectMapper.writeValueAsString(Map.of("mealId", MEAL_ID));

        mockMvc.perform(post("/api/steps/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.recipeId").value(MEAL_ID))
                .andExpect(jsonPath("$.recipeName").value(MEAL_NAME))
                .andExpect(jsonPath("$.steps").isArray())
                .andExpect(jsonPath("$.steps", hasSize(2)))
                .andExpect(jsonPath("$.steps[0].stepNumber").value(1))
                .andExpect(jsonPath("$.steps[0].action").value("MARINATE"))
                .andExpect(jsonPath("$.steps[1].stepNumber").value(2))
                .andExpect(jsonPath("$.steps[1].action").value("FRYING_PAN"));
    }

    // -----------------------------------------------------------------------
    // Test: cache hit – second request returns steps from database
    // -----------------------------------------------------------------------

    @Test
    void generateSteps_shouldReturnExistingStepsFromDatabaseOnSecondRequest() throws Exception {
        when(mealDbClient.lookupById(MEAL_ID))
                .thenReturn(Mono.just(new MealSearchResponse(List.of(buildMeal()))));
        when(groqClient.generateSteps(MEAL_INSTRUCTIONS))
                .thenReturn(Mono.just(buildLlmResponse()));

        String requestBody = objectMapper.writeValueAsString(Map.of("mealId", MEAL_ID));

        // First request – generates and persists steps
        mockMvc.perform(post("/api/steps/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.steps", hasSize(2)));

        // Manually flush to ensure DB state is visible (within same transaction the check still applies)
        // Second request – should be served from DB (recipeName will be null in cached response)
        mockMvc.perform(post("/api/steps/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recipeId").value(MEAL_ID))
                .andExpect(jsonPath("$.steps").isArray())
                .andExpect(jsonPath("$.steps", hasSize(2)));
    }

    // -----------------------------------------------------------------------
    // Test: meal not found in TheMealDB
    // -----------------------------------------------------------------------

    @Test
    void generateSteps_shouldReturn404WhenMealNotFoundInTheMealDb() throws Exception {
        when(mealDbClient.lookupById("99999"))
                .thenReturn(Mono.just(new MealSearchResponse(null)));

        String requestBody = objectMapper.writeValueAsString(Map.of("mealId", "99999"));

        mockMvc.perform(post("/api/steps/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.code").value("MEAL_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Meal with id 99999 not found"));
    }

    @Test
    void generateSteps_shouldReturn404WhenMealResponseIsEmpty() throws Exception {
        when(mealDbClient.lookupById("00000"))
                .thenReturn(Mono.just(new MealSearchResponse(List.of())));

        String requestBody = objectMapper.writeValueAsString(Map.of("mealId", "00000"));

        mockMvc.perform(post("/api/steps/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("MEAL_NOT_FOUND"));
    }

    // -----------------------------------------------------------------------
    // Test: LLM API error
    // -----------------------------------------------------------------------

    @Test
    void generateSteps_shouldReturn502WhenGroqClientFails() throws Exception {
        when(mealDbClient.lookupById(MEAL_ID))
                .thenReturn(Mono.just(new MealSearchResponse(List.of(buildMeal()))));
        when(groqClient.generateSteps(MEAL_INSTRUCTIONS))
                .thenReturn(Mono.error(new com.cookmate.main.exception.ExternalServiceException(
                        "Groq", new RuntimeException("Connection refused")
                )));

        String requestBody = objectMapper.writeValueAsString(Map.of("mealId", MEAL_ID));

        mockMvc.perform(post("/api/steps/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.status").value(502))
                .andExpect(jsonPath("$.code").value("EXTERNAL_SERVICE_ERROR"));
    }

    // -----------------------------------------------------------------------
    // Test: validation – blank mealId
    // -----------------------------------------------------------------------

    @Test
    void generateSteps_shouldReturn400WhenMealIdIsBlank() throws Exception {
        String requestBody = objectMapper.writeValueAsString(Map.of("mealId", ""));

        mockMvc.perform(post("/api/steps/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("REQUEST_VALIDATION_FAILED"))
                .andExpect(jsonPath("$.details[0].field").value("mealId"));
    }

    @Test
    void generateSteps_shouldReturn400WhenRequestBodyIsMissing() throws Exception {
        mockMvc.perform(post("/api/steps/generate")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    // -----------------------------------------------------------------------
    // Test: Content-Type negotiation
    // -----------------------------------------------------------------------

    @Test
    void generateSteps_shouldReturn415WhenContentTypeIsNotJson() throws Exception {
        mockMvc.perform(post("/api/steps/generate")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("{\"mealId\":\"52772\"}"))
                .andExpect(status().isUnsupportedMediaType());
    }
}
