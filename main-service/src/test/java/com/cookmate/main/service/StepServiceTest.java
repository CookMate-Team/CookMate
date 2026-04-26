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
            .duration(60)
            .createdAt(LocalDateTime.now())
            .build();
        StepDTO expectedDto = StepDTO.builder()
            .id(step.getId())
            .stepNumber(step.getStepNumber())
            .description(step.getDescription())
            .action(step.getAction())
            .recipeId(step.getRecipeId())
            .duration(step.getDuration())
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
}
