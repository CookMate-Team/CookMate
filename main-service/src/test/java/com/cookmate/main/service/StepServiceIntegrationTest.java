package com.cookmate.main.service;

import com.cookmate.main.dto.LLMResponseDTO;
import com.cookmate.main.dto.LLMStepDTO;
import com.cookmate.main.dto.Meal;
import com.cookmate.main.dto.MealSearchResponse;
import com.cookmate.main.dto.StepDTO;
import com.cookmate.main.dto.StepGenerationRequest;
import com.cookmate.main.dto.StepGenerationResponse;
import com.cookmate.main.exception.ExternalServiceException;
import com.cookmate.main.exception.MealNotFoundException;
import com.cookmate.main.model.ActionType;
import com.cookmate.main.model.Step;
import com.cookmate.main.repository.StepRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Integration tests for {@link StepService}.
 *
 * <p>The full Spring context is loaded (H2 in-memory DB). External HTTP clients
 * ({@link MealDbClient} and {@link GroqClient}) are replaced by {@code @MockBean}s.
 * Each test runs within a transaction that is rolled back afterwards so tests
 * remain independent.</p>
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class StepServiceIntegrationTest {

    @Autowired
    private StepService stepService;

    @Autowired
    private StepRepository stepRepository;

    @MockBean
    private MealDbClient mealDbClient;

    @MockBean
    private GroqClient groqClient;

    // -----------------------------------------------------------------------
    // Shared test fixtures
    // -----------------------------------------------------------------------

    private static final String MEAL_ID      = "52772";
    private static final String MEAL_NAME    = "Teriyaki Chicken Casserole";
    private static final String MEAL_INST    = "Mix soy sauce, mirin and sugar. Add chicken and marinate.";

    /**
     * Builds a minimal {@link Meal} record with only the fields used by {@link StepService}.
     * The Meal record has 53 components; unused ones are {@code null}.
     */
    private Meal buildMeal() {
        // Component order: idMeal, strMeal, strMealAlternate, strCategory, strArea,
        //                  strInstructions, strMealThumb, strTags, strYoutube, strSource,
        //                  strImageSource, strCreativeCommonsConfirmed, dateModified,
        //                  strIngredient1..20 (20 fields), strMeasure1..20 (20 fields)
        return new Meal(
                MEAL_ID, MEAL_NAME, null, "Chicken", "Japanese",
                MEAL_INST, null, null, null, null,
                null, null, null,
                "Soy Sauce", null, "Mirin", null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null,
                null, null, null
        );
    }

    private LLMResponseDTO buildLlmResponse() {
        LLMStepDTO step1 = new LLMStepDTO(
                1, "Marinate chicken with soy sauce and mirin.",
                ActionType.MARINATE, "chicken", 30, Map.of("temperature", 0, "speed", 0)
        );
        LLMStepDTO step2 = new LLMStepDTO(
                2, "Cook the chicken at 180°C in FRYING_PAN mode.",
                ActionType.FRYING_PAN, "chicken", 15, Map.of("temperature", 180, "speed", 3)
        );
        return new LLMResponseDTO(List.of(step1, step2));
    }

    @BeforeEach
    void cleanDatabase() {
        stepRepository.deleteAll();
    }

    // -----------------------------------------------------------------------
    // Test: generateAndSaveSteps end-to-end
    // -----------------------------------------------------------------------

    @Test
    void generateSteps_shouldFetchFromLlmAndPersistToDatabase() {
        when(mealDbClient.lookupById(MEAL_ID))
                .thenReturn(Mono.just(new MealSearchResponse(List.of(buildMeal()))));
        when(groqClient.generateSteps(MEAL_INST))
                .thenReturn(Mono.just(buildLlmResponse()));

        StepGenerationResponse response = stepService.generateSteps(new StepGenerationRequest(MEAL_ID));

        // Verify response
        assertThat(response.recipeId()).isEqualTo(MEAL_ID);
        assertThat(response.recipeName()).isEqualTo(MEAL_NAME);
        assertThat(response.steps()).hasSize(2);

        StepDTO first = response.steps().get(0);
        assertThat(first.stepNumber()).isEqualTo(1);
        assertThat(first.action()).isEqualTo(ActionType.MARINATE);
        assertThat(first.recipeId()).isEqualTo(MEAL_ID);
        assertThat(first.id()).isNotNull();

        // Verify persistence
        List<Step> persisted = stepRepository.findByRecipeIdOrderByStepNumberAsc(MEAL_ID);
        assertThat(persisted).hasSize(2);
        assertThat(persisted.get(0).getAction()).isEqualTo(ActionType.MARINATE);
        assertThat(persisted.get(1).getAction()).isEqualTo(ActionType.FRYING_PAN);

        verify(mealDbClient).lookupById(MEAL_ID);
        verify(groqClient).generateSteps(MEAL_INST);
    }

    // -----------------------------------------------------------------------
    // Test: cache hit – second request returns from database, no LLM call
    // -----------------------------------------------------------------------

    @Test
    void generateSteps_shouldReturnExistingStepsWithoutCallingLlm() {
        // Pre-seed the database with steps for MEAL_ID
        stepRepository.saveAll(List.of(
                Step.builder().stepNumber(1).description("Pre-seeded step 1")
                        .action(ActionType.CHOP).recipeId(MEAL_ID).durationMinutes(5).build(),
                Step.builder().stepNumber(2).description("Pre-seeded step 2")
                        .action(ActionType.STIR).recipeId(MEAL_ID).durationMinutes(10).build()
        ));

        StepGenerationResponse response = stepService.generateSteps(new StepGenerationRequest(MEAL_ID));

        // Cached – returns from DB
        assertThat(response.recipeId()).isEqualTo(MEAL_ID);
        assertThat(response.recipeName()).isNull(); // cached response has no recipeName
        assertThat(response.steps()).hasSize(2);
        assertThat(response.steps().get(0).description()).isEqualTo("Pre-seeded step 1");

        // External services must NOT have been called
        verifyNoInteractions(mealDbClient);
        verifyNoInteractions(groqClient);
    }

    // -----------------------------------------------------------------------
    // Test: meal not found in TheMealDB
    // -----------------------------------------------------------------------

    @Test
    void generateSteps_shouldThrowMealNotFoundExceptionWhenMealResponseIsNull() {
        when(mealDbClient.lookupById("99999"))
                .thenReturn(Mono.just(new MealSearchResponse(null)));

        assertThatThrownBy(() -> stepService.generateSteps(new StepGenerationRequest("99999")))
                .isInstanceOf(MealNotFoundException.class)
                .hasMessage("Meal with id 99999 not found");

        verifyNoInteractions(groqClient);
    }

    @Test
    void generateSteps_shouldThrowMealNotFoundExceptionWhenMealListIsEmpty() {
        when(mealDbClient.lookupById("00000"))
                .thenReturn(Mono.just(new MealSearchResponse(List.of())));

        assertThatThrownBy(() -> stepService.generateSteps(new StepGenerationRequest("00000")))
                .isInstanceOf(MealNotFoundException.class)
                .hasMessage("Meal with id 00000 not found");

        verifyNoInteractions(groqClient);
    }

    // -----------------------------------------------------------------------
    // Test: Groq / LLM error handling
    // -----------------------------------------------------------------------

    @Test
    void generateSteps_shouldThrowExternalServiceExceptionWhenGroqFails() {
        when(mealDbClient.lookupById(MEAL_ID))
                .thenReturn(Mono.just(new MealSearchResponse(List.of(buildMeal()))));
        when(groqClient.generateSteps(MEAL_INST))
                .thenReturn(Mono.error(new ExternalServiceException("Groq", new RuntimeException("timeout"))));

        assertThatThrownBy(() -> stepService.generateSteps(new StepGenerationRequest(MEAL_ID)))
                .isInstanceOf(ExternalServiceException.class)
                .hasMessageContaining("Groq");

        // Nothing should have been persisted
        assertThat(stepRepository.findByRecipeIdOrderByStepNumberAsc(MEAL_ID)).isEmpty();
    }

    @Test
    void generateSteps_shouldThrowExternalServiceExceptionWhenLlmReturnsEmptySteps() {
        when(mealDbClient.lookupById(MEAL_ID))
                .thenReturn(Mono.just(new MealSearchResponse(List.of(buildMeal()))));
        when(groqClient.generateSteps(MEAL_INST))
                .thenReturn(Mono.just(new LLMResponseDTO(List.of()))); // empty steps

        assertThatThrownBy(() -> stepService.generateSteps(new StepGenerationRequest(MEAL_ID)))
                .isInstanceOf(ExternalServiceException.class);

        assertThat(stepRepository.findByRecipeIdOrderByStepNumberAsc(MEAL_ID)).isEmpty();
    }

    // -----------------------------------------------------------------------
    // Test: database persistence – step fields are correctly mapped and stored
    // -----------------------------------------------------------------------

    @Test
    void generateSteps_shouldPersistAllStepFieldsCorrectly() {
        when(mealDbClient.lookupById(MEAL_ID))
                .thenReturn(Mono.just(new MealSearchResponse(List.of(buildMeal()))));
        when(groqClient.generateSteps(MEAL_INST))
                .thenReturn(Mono.just(buildLlmResponse()));

        stepService.generateSteps(new StepGenerationRequest(MEAL_ID));

        List<Step> steps = stepRepository.findByRecipeIdOrderByStepNumberAsc(MEAL_ID);
        assertThat(steps).hasSize(2);

        Step step1 = steps.get(0);
        assertThat(step1.getId()).isNotNull();
        assertThat(step1.getStepNumber()).isEqualTo(1);
        assertThat(step1.getDescription()).isEqualTo("Marinate chicken with soy sauce and mirin.");
        assertThat(step1.getAction()).isEqualTo(ActionType.MARINATE);
        assertThat(step1.getMainIngredient()).isEqualTo("chicken");
        assertThat(step1.getDurationMinutes()).isEqualTo(30);
        assertThat(step1.getRecipeId()).isEqualTo(MEAL_ID);
        assertThat(step1.getParameters()).containsEntry("temperature", 0);

        Step step2 = steps.get(1);
        assertThat(step2.getStepNumber()).isEqualTo(2);
        assertThat(step2.getAction()).isEqualTo(ActionType.FRYING_PAN);
        assertThat(step2.getParameters()).containsEntry("temperature", 180);
    }

    // -----------------------------------------------------------------------
    // Test: getStep – delegates to repository
    // -----------------------------------------------------------------------

    @Test
    void getStep_shouldReturnDtoForExistingStep() {
        Step saved = stepRepository.save(Step.builder()
                .stepNumber(1)
                .description("Chop onions finely.")
                .action(ActionType.CHOP)
                .recipeId("recipe-abc")
                .durationMinutes(5)
                .build());

        StepDTO dto = stepService.getStep(saved.getId());

        assertThat(dto.id()).isEqualTo(saved.getId());
        assertThat(dto.description()).isEqualTo("Chop onions finely.");
        assertThat(dto.action()).isEqualTo(ActionType.CHOP);
    }

    // -----------------------------------------------------------------------
    // Test: getStepsByRecipeId – returns all steps ordered by stepNumber
    // -----------------------------------------------------------------------

    @Test
    void getStepsByRecipeId_shouldReturnStepsInAscendingOrder() {
        stepRepository.saveAll(List.of(
                Step.builder().stepNumber(3).description("Third").action(ActionType.WAIT)
                        .recipeId("order-test").durationMinutes(1).build(),
                Step.builder().stepNumber(1).description("First").action(ActionType.CUT)
                        .recipeId("order-test").durationMinutes(2).build(),
                Step.builder().stepNumber(2).description("Second").action(ActionType.POUR)
                        .recipeId("order-test").durationMinutes(3).build()
        ));

        List<StepDTO> result = stepService.getStepsByRecipeId("order-test");

        assertThat(result).hasSize(3);
        assertThat(result.get(0).stepNumber()).isEqualTo(1);
        assertThat(result.get(1).stepNumber()).isEqualTo(2);
        assertThat(result.get(2).stepNumber()).isEqualTo(3);
    }
}
