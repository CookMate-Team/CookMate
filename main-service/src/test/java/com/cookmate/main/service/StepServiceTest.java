package com.cookmate.main.service;

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
        Step step1 = Step.builder().id(1L).stepNumber(2).description("Second").action(ActionType.STIR)
            .recipeId(recipeId).durationMinutes(5).createdAt(LocalDateTime.now()).build();
        Step step2 = Step.builder().id(2L).stepNumber(1).description("First").action(ActionType.CHOP)
            .recipeId(recipeId).durationMinutes(3).createdAt(LocalDateTime.now()).build();

        StepDTO dto1 = StepDTO.builder().id(1L).stepNumber(2).description("Second").action(ActionType.STIR)
            .recipeId(recipeId).durationMinutes(5).build();
        StepDTO dto2 = StepDTO.builder().id(2L).stepNumber(1).description("First").action(ActionType.CHOP)
            .recipeId(recipeId).durationMinutes(3).build();

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
}
