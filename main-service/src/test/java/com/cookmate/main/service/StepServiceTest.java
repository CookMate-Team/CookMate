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

        // 2. Mock Meal with various combinations of ingredients & measures
        Meal mockMeal = new Meal(
                mealId,
                "Test Chicken Recipe",
                null,
                "Chicken",
                "Italian",
                "Instructions text...",
                "thumb.jpg",
                "tags",
                "youtube.com",
                "source",
                "imgSource",
                "confirmed",
                "2026-06-04",
                "Chicken", "500g",         // 1. normal
                "Garlic", "",              // 2. empty measure
                "Salt", null,              // 3. null measure
                "  ", "1 tsp",             // 4. empty ingredient name
                null, "1 cup",             // 5. null ingredient name
                null, null,                // 6
                null, null,                // 7
                null, null,                // 8
                null, null,                // 9
                null, null,                // 10
                null, null,                // 11
                null, null,                // 12
                null, null,                // 13
                null, null,                // 14
                null, null,                // 15
                null, null,                // 16
                null, null,                // 17
                null, null,                // 18
                null, null,                // 19
                null, null                 // 20
        );

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
}
