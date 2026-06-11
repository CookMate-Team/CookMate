package com.cookmate.simulator.service;

import com.cookmate.simulator.client.CookingSessionClient;
import com.cookmate.simulator.client.MainServiceClient;
import com.cookmate.simulator.dto.MainServiceStepDto;
import com.cookmate.simulator.dto.RecipeStepRequestDto;
import com.cookmate.simulator.dto.StartSimulationRequestDto;
import com.cookmate.simulator.dto.StepExecutionResultDto;
import com.cookmate.simulator.exception.InvalidSimulationStateException;
import com.cookmate.simulator.exception.SimulationSessionNotFoundException;
import com.cookmate.simulator.model.SimulationSession;
import com.cookmate.simulator.model.SimulationStatus;
import com.cookmate.simulator.model.SimulationStep;
import com.cookmate.simulator.model.StepStatus;
import com.cookmate.simulator.repository.SimulationSessionRepository;
import com.cookmate.simulator.repository.SimulationStepRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SimulationServiceTest {

    @Mock
    private SimulationSessionRepository sessionRepository;

    @Mock
    private SimulationStepRepository stepRepository;

    @Mock
    private MainServiceClient mainServiceClient;

    @Mock
    private CookingSessionClient cookingSessionClient;

    @InjectMocks
    private SimulationService simulationService;

    private SimulationSession testSession;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(sessionRepository.findByStatusAndUserId(SimulationStatus.RUNNING, "test-user-123")).thenReturn(List.of());

        testSession = new SimulationSession();
        testSession.setId("test-session-123");
        testSession.setUserId("test-user-123");
        testSession.setStatus(SimulationStatus.RUNNING);
        testSession.setCurrentStep(0);
        testSession.setTotalSteps(2);
        testSession.setRecipeId("meal-1");
        testSession.setTotalRecipes(1);
        testSession.setMessage(null);
    }

    @Test
    void testStartSessionLoadsStepsFromMainService() {
        List<MainServiceStepDto> mainSteps = List.of(
                new MainServiceStepDto(1L, 1, "Step 1", null, 5, "meal-1"),
                new MainServiceStepDto(2L, 2, "Step 2", null, 3, "meal-1")
        );

        when(mainServiceClient.getRecipeSteps("meal-1")).thenReturn(mainSteps);
        when(sessionRepository.save(any(SimulationSession.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(stepRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var response = simulationService.startSession(new StartSimulationRequestDto("meal-1"), "test-user-123", "Bearer test-token");

        assertNotNull(response);
        assertEquals(SimulationStatus.RUNNING.name(), response.status());
        assertEquals(2, response.totalSteps());
        verify(stepRepository, times(1)).saveAll(any());
    }

    @Test
    void testStartSessionWithoutStepsThrowsError() {
        when(mainServiceClient.getRecipeSteps("meal-1")).thenReturn(List.of());
        assertThrows(
                InvalidSimulationStateException.class,
                () -> simulationService.startSession(new StartSimulationRequestDto("meal-1"), "test-user-123", "Bearer test-token")
        );
    }

    @Test
    void testExecuteNextStepMarksPendingAsExecuted() {
        SimulationStep pending = new SimulationStep();
        pending.setSessionId("test-session-123");
        pending.setStepNumber(1);
        pending.setStatus(StepStatus.PENDING);

        when(sessionRepository.findByIdAndUserId("test-session-123", "test-user-123")).thenReturn(Optional.of(testSession));
        when(stepRepository.findFirstBySessionIdAndStatusOrderByStepNumberAsc("test-session-123", StepStatus.PENDING))
                .thenReturn(Optional.of(pending));
        when(stepRepository.countBySessionIdAndStatus("test-session-123", StepStatus.PENDING)).thenReturn(1L);
        when(stepRepository.save(any(SimulationStep.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(sessionRepository.save(any(SimulationSession.class))).thenAnswer(invocation -> invocation.getArgument(0));

        StepExecutionResultDto response = simulationService.executeNextStep("test-session-123", "test-user-123", "Bearer test-token");

        assertTrue(response.getSuccess());
        assertEquals(1, response.getStepNumber());
        verify(stepRepository).save(argThat(step -> step.getStatus() == StepStatus.EXECUTED));
    }

    @Test
    void testProcessStepDirectSuccess() {
        when(sessionRepository.findByIdAndUserId("test-session-123", "test-user-123")).thenReturn(Optional.of(testSession));
        when(stepRepository.findBySessionIdAndStepNumber("test-session-123", 1)).thenReturn(Optional.empty());
        when(stepRepository.countBySessionIdAndStatus("test-session-123", StepStatus.PENDING)).thenReturn(1L);
        when(stepRepository.save(any(SimulationStep.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(sessionRepository.save(any(SimulationSession.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RecipeStepRequestDto stepRequest = new RecipeStepRequestDto(1, "Test step", 1, "180C", "200g", "Note");
        StepExecutionResultDto response = simulationService.processStep("test-session-123", stepRequest, "test-user-123", "Bearer test-token");

        assertTrue(response.getSuccess());
        assertEquals(1, response.getStepNumber());
    }

    @Test
    void testGetStatusNotFound() {
        when(sessionRepository.findByIdAndUserId("unknown-session", "test-user-123")).thenReturn(Optional.empty());
        assertThrows(SimulationSessionNotFoundException.class, () -> simulationService.getStatus("unknown-session", "test-user-123"));
    }

    @Test
    void testRewindToStep() {
        SimulationStep step1 = new SimulationStep();
        step1.setSessionId("test-session-123");
        step1.setStepNumber(1);
        step1.setStatus(StepStatus.EXECUTED);
        step1.setExecutedAt(LocalDateTime.now());

        SimulationStep step2 = new SimulationStep();
        step2.setSessionId("test-session-123");
        step2.setStepNumber(2);
        step2.setStatus(StepStatus.EXECUTED);
        step2.setExecutedAt(LocalDateTime.now());

        when(sessionRepository.findByIdAndUserId("test-session-123", "test-user-123")).thenReturn(Optional.of(testSession));
        when(stepRepository.findBySessionIdOrderByStepNumberAsc("test-session-123")).thenReturn(new ArrayList<>(List.of(step1, step2)));
        when(stepRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(sessionRepository.save(any(SimulationSession.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(stepRepository.countBySessionIdAndStatus("test-session-123", StepStatus.EXECUTED)).thenReturn(1L);

        var status = simulationService.rewindToStep("test-session-123", 1, "test-user-123");

        assertEquals(1, status.currentStep());
        assertEquals(SimulationStatus.RUNNING.name(), status.status());
    }
}
