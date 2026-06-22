package com.cookmate.main.service;

import com.cookmate.main.dto.Meal;
import com.cookmate.main.dto.MealSearchResponse;
import com.cookmate.main.dto.LLMResponseDTO;
import com.cookmate.main.dto.LLMStepDTO;
import com.cookmate.main.dto.StepGenerationRequest;
import com.cookmate.main.dto.StepGenerationResponse;
import com.cookmate.main.dto.StepDTO;
import com.cookmate.main.exception.StepNotFoundException;
import com.cookmate.main.mapper.StepMapper;
import com.cookmate.main.model.ActionType;
import com.cookmate.main.model.Step;
import com.cookmate.main.repository.RecipeRepository;
import com.cookmate.main.repository.StepRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StepServiceTest {

    @Mock
    private StepRepository stepRepository;

    @Mock
    private StepMapper stepMapper;

    @Mock
    private RecipeRepository recipeRepository;

    @Mock
    private MealDbClient mealDbClient;

    @Mock
    private GroqClient groqClient;

    @InjectMocks
    private StepService stepService;

    @Test
    void shouldReturnStepWhenFound() {
        Step step = Step.builder()
            .id(1L)
            .stepNumber(1)
            .description("Pokroj cebule")
            .action(ActionType.CHOP)
            .recipeId("recipe-123")
            .durationMinutes(60)
            .createdAt(LocalDateTime.now())
            .build();
        StepDTO expectedDto = StepDTO.builder()
            .id(step.getId())
            .stepNumber(step.getStepNumber())
            .description(step.getDescription())
            .action(step.getAction())
            .recipeId(step.getRecipeId())
            .durationMinutes(step.getDurationMinutes())
            .createdAt(step.getCreatedAt())
            .build();

        when(stepRepository.findById(1L)).thenReturn(Optional.of(step));
        when(stepMapper.toDTO(step)).thenReturn(expectedDto);

        StepDTO result = stepService.getStep(1L);

        assertThat(result).isEqualTo(expectedDto);
        verify(stepRepository).findById(1L);
        verify(stepMapper).toDTO(step);
    }

    @Test
    void shouldThrowWhenStepNotFound() {
        when(stepRepository.findById(42L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> stepService.getStep(42L))
            .isInstanceOf(StepNotFoundException.class)
            .hasMessage("Step with id 42 not found");

        verify(stepRepository).findById(42L);
        verifyNoInteractions(stepMapper);
    }

    @Test
    void shouldReturnStepsByRecipeIdPreservingRepositoryOrder() {
        String recipeId = "recipe-999";
        // Steps returned by the repository are already sorted ASC by stepNumber
        Step step1 = Step.builder().id(1L).stepNumber(1).description("First").action(ActionType.CHOP)
            .recipeId(recipeId).durationMinutes(3).createdAt(LocalDateTime.now()).build();
        Step step2 = Step.builder().id(2L).stepNumber(2).description("Second").action(ActionType.STIR)
            .recipeId(recipeId).durationMinutes(5).createdAt(LocalDateTime.now()).build();

        StepDTO dto1 = StepDTO.builder().id(1L).stepNumber(1).description("First").action(ActionType.CHOP)
            .recipeId(recipeId).durationMinutes(3).build();
        StepDTO dto2 = StepDTO.builder().id(2L).stepNumber(2).description("Second").action(ActionType.STIR)
            .recipeId(recipeId).durationMinutes(5).build();

        when(stepRepository.findByRecipeIdOrderByStepNumberAsc(recipeId)).thenReturn(List.of(step1, step2));
        when(stepMapper.toDTO(step1)).thenReturn(dto1);
        when(stepMapper.toDTO(step2)).thenReturn(dto2);

        List<StepDTO> result = stepService.getStepsByRecipeId(recipeId);

        assertThat(result).hasSize(2);
        assertThat(result.get(0)).isEqualTo(dto1);
        assertThat(result.get(1)).isEqualTo(dto2);
        verify(stepRepository).findByRecipeIdOrderByStepNumberAsc(recipeId);
        verify(stepMapper).toDTO(step1);
        verify(stepMapper).toDTO(step2);
    }

    @Test
    void shouldCorrectlyParseAndFormatIngredientsWhenGeneratingSteps() {
        String mealId = "12345";
        StepGenerationRequest request = new StepGenerationRequest(mealId);

        // 1. Mock repository to return no existing steps
        when(stepRepository.findByRecipeIdOrderByStepNumberAsc(mealId))
                .thenReturn(List.of());
        when(recipeRepository.findById(12345L)).thenReturn(Optional.empty());

        // 2. Mock Meal with various combinations of ingredients & measures
        Meal mockMeal = Meal.builder()
                .idMeal(mealId)
                .strMeal("Test Chicken Recipe")
                .strCategory("Chicken")
                .strArea("Italian")
                .strInstructions("Instructions text...")
                .strMealThumb("thumb.jpg")
                .strTags("tags")
                .strYoutube("youtube.com")
                .strSource("source")
                .strImageSource("imgSource")
                .strCreativeCommonsConfirmed("confirmed")
                .dateModified("2026-06-04")
                .strIngredient1("Chicken").strMeasure1("500g")
                .strIngredient2("Garlic").strMeasure2("")
                .strIngredient3("Salt").strMeasure3(null)
                .strIngredient4("  ").strMeasure4("1 tsp")
                .strIngredient5(null).strMeasure5("1 cup")
                .build();

        MealSearchResponse mockMealResponse = new MealSearchResponse(List.of(mockMeal));
        when(mealDbClient.lookupById(mealId)).thenReturn(reactor.core.publisher.Mono.just(mockMealResponse));

        // 3. Mock GroqClient response
        LLMStepDTO mockLlmStep = new LLMStepDTO(
                1,
                "Weigh 500g of chicken",
                ActionType.WEIGH,
                "Chicken",
                5,
                Map.of()
        );
        LLMResponseDTO mockLlmResponse = new LLMResponseDTO(List.of(mockLlmStep));

        // Expect the formatted ingredients to be:
        // "- Chicken (500g)\n- Garlic\n- Salt"
        String expectedIngredients = "- Chicken (500g)\n- Garlic\n- Salt";
        when(groqClient.generateSteps("Instructions text...", expectedIngredients))
                .thenReturn(reactor.core.publisher.Mono.just(mockLlmResponse));

        // 4. Mock stepRepository.saveAll and stepMapper
        Step stepEntity = Step.builder()
                .id(10L)
                .stepNumber(1)
                .description("Weigh 500g of chicken")
                .action(ActionType.WEIGH)
                .mainIngredient("Chicken")
                .durationMinutes(5)
                .parameters(Map.of())
                .recipeId(mealId)
                .build();
        when(stepRepository.saveAll(anyList())).thenReturn(List.of(stepEntity));

        StepDTO stepDto = StepDTO.builder()
                .id(10L)
                .stepNumber(1)
                .description("Weigh 500g of chicken")
                .action(ActionType.WEIGH)
                .recipeId(mealId)
                .durationMinutes(5)
                .build();
        when(stepMapper.toDTO(stepEntity)).thenReturn(stepDto);

        // Act
        StepGenerationResponse response = stepService.generateSteps(request);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.recipeId()).isEqualTo(mealId);
        assertThat(response.recipeName()).isEqualTo("Test Chicken Recipe");
        assertThat(response.steps()).hasSize(1);
        assertThat(response.steps().get(0).description()).isEqualTo("Weigh 500g of chicken");

        verify(stepRepository).findByRecipeIdOrderByStepNumberAsc(mealId);
        verify(mealDbClient).lookupById(mealId);
        verify(groqClient).generateSteps("Instructions text...", expectedIngredients);
        verify(stepRepository).saveAll(anyList());
        verify(stepMapper).toDTO(stepEntity);
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldApplyGuardrailsAndSequenceCorrectlyWhenGeneratingSteps() {
        String mealId = "999";
        StepGenerationRequest request = new StepGenerationRequest(mealId);

        when(stepRepository.findByRecipeIdOrderByStepNumberAsc(mealId)).thenReturn(List.of());
        when(recipeRepository.findById(999L)).thenReturn(Optional.empty());

        Meal mockMeal = Meal.builder()
                .idMeal(mealId)
                .strMeal("Test Recipe")
                .strCategory("Test")
                .strArea("Test Area")
                .strInstructions("Instructions...")
                .strMealThumb("thumb")
                .strTags("tags")
                .strYoutube("yt")
                .strSource("src")
                .strImageSource("img")
                .strCreativeCommonsConfirmed("CC")
                .dateModified("date")
                .strIngredient1("Ingredient").strMeasure1("10g")
                .build();
        when(mealDbClient.lookupById(mealId)).thenReturn(reactor.core.publisher.Mono.just(new MealSearchResponse(List.of(mockMeal))));

        // Gapped and out-of-order step numbers with unsafe parameters
        LLMStepDTO step1 = new LLMStepDTO(5, "Weighing step", ActionType.WEIGH, "Flour", 1, new java.util.HashMap<>(Map.of("temperature", 100, "speed", 5)));
        LLMStepDTO step2 = new LLMStepDTO(2, "Chopping step", ActionType.CHOP, "Onion", 1, new java.util.HashMap<>(Map.of("temperature", 80, "speed", 4)));
        LLMStepDTO step3 = new LLMStepDTO(8, "Pot step", ActionType.POT, "Water", 10, new java.util.HashMap<>(Map.of("temperature", 100, "speed", 6)));
        LLMStepDTO step4 = new LLMStepDTO(12, "Blending hot", ActionType.BLEND, "Soup", 2, new java.util.HashMap<>(Map.of("temperature", 90, "speed", 8)));
        LLMStepDTO step5 = new LLMStepDTO(13, "Blending cold", ActionType.BLEND, "Smoothie", 2, new java.util.HashMap<>(Map.of("temperature", 40, "speed", 8)));

        LLMResponseDTO llmResponse = new LLMResponseDTO(List.of(step1, step2, step3, step4, step5));
        when(groqClient.generateSteps(anyString(), anyString())).thenReturn(reactor.core.publisher.Mono.just(llmResponse));

        when(stepRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        stepService.generateSteps(request);

        // Assert and capture
        org.mockito.ArgumentCaptor<List<Step>> stepsCaptor = org.mockito.ArgumentCaptor.forClass(List.class);
        verify(stepRepository).saveAll(stepsCaptor.capture());
        List<Step> savedSteps = stepsCaptor.getValue();

        assertThat(savedSteps).hasSize(5);

        // Verify sorted and re-indexed step numbers
        assertThat(savedSteps.get(0).getStepNumber()).isEqualTo(1); // was 2 (CHOP)
        assertThat(savedSteps.get(1).getStepNumber()).isEqualTo(2); // was 5 (WEIGH)
        assertThat(savedSteps.get(2).getStepNumber()).isEqualTo(3); // was 8 (POT)
        assertThat(savedSteps.get(3).getStepNumber()).isEqualTo(4); // was 12 (BLEND hot)
        assertThat(savedSteps.get(4).getStepNumber()).isEqualTo(5); // was 13 (BLEND cold)

        // Verify Guardrail 1: WEIGH (original temp=100, speed=5 -> expected temp=0, speed=0)
        Step weighStep = savedSteps.stream().filter(s -> s.getAction() == ActionType.WEIGH).findFirst().orElseThrow();
        assertThat(weighStep.getParameters().get("temperature")).isEqualTo(0);
        assertThat(weighStep.getParameters().get("speed")).isEqualTo(0);

        // Verify Guardrail 2: CHOP (original temp=80, speed=4 -> expected temp=0, speed=4)
        Step chopStep = savedSteps.stream().filter(s -> s.getAction() == ActionType.CHOP).findFirst().orElseThrow();
        assertThat(chopStep.getParameters().get("temperature")).isEqualTo(0);
        assertThat(chopStep.getParameters().get("speed")).isEqualTo(4);

        // Verify Guardrail 3: POT (original temp=100, speed=6 -> expected temp=100, speed=3)
        Step potStep = savedSteps.stream().filter(s -> s.getAction() == ActionType.POT).findFirst().orElseThrow();
        assertThat(potStep.getParameters().get("temperature")).isEqualTo(100);
        assertThat(potStep.getParameters().get("speed")).isEqualTo(3);

        // Verify Guardrail 4: BLEND hot (original temp=90, speed=8 -> expected temp=90, speed=4)
        Step blendHot = savedSteps.stream().filter(s -> s.getDescription().contains("hot")).findFirst().orElseThrow();
        assertThat(blendHot.getParameters().get("temperature")).isEqualTo(90);
        assertThat(blendHot.getParameters().get("speed")).isEqualTo(4);

        // Verify Guardrail 4 (negative case): BLEND cold (original temp=40, speed=8 -> expected temp=40, speed=8)
        Step blendCold = savedSteps.stream().filter(s -> s.getDescription().contains("cold")).findFirst().orElseThrow();
        assertThat(blendCold.getParameters().get("temperature")).isEqualTo(40);
        assertThat(blendCold.getParameters().get("speed")).isEqualTo(8);
    }
}
