package com.cookmate.simulator.service;

import com.cookmate.simulator.dto.RecipeStepRequestDto;
import com.cookmate.simulator.dto.RecipeStepResponseDto;
import com.cookmate.simulator.dto.SimulationStatusResponseDto;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class SimulationServiceTest {

    @Mock
    private SimulationSessionRepository sessionRepository;

    @Mock
    private SimulationStepRepository stepRepository;

    @InjectMocks
    private SimulationService simulationService;

    private SimulationSession testSession;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        
        testSession = new SimulationSession();
        testSession.setId("test-session-123");
        testSession.setStatus(SimulationStatus.RUNNING);
        testSession.setCurrentStep(0);
        testSession.setTotalSteps(0);
        testSession.setMessage(null);
    }

    @Test
    void testStartSession() {
        when(sessionRepository.save(any(SimulationSession.class))).thenReturn(testSession);
        when(stepRepository.findBySessionIdOrderByStepNumberAsc("test-session-123")).thenReturn(new ArrayList<>());

        SimulationStatusResponseDto response = simulationService.startSession();

        assertNotNull(response);
        assertEquals(SimulationStatus.RUNNING.name(), response.status());
        assertEquals(0, response.currentStep());
        assertEquals(0, response.totalSteps());
        verify(sessionRepository, times(1)).save(any(SimulationSession.class));
    }

    @Test
    void testGetSessionStatus() {
        when(sessionRepository.findById("test-session-123")).thenReturn(Optional.of(testSession));
        when(stepRepository.findBySessionIdOrderByStepNumberAsc("test-session-123")).thenReturn(new ArrayList<>());

        SimulationStatusResponseDto response = simulationService.getStatus("test-session-123");

        assertNotNull(response);
        assertEquals("test-session-123", response.sessionId());
        assertEquals(SimulationStatus.RUNNING.name(), response.status());
    }

    @Test
    void testGetSessionStatusNotFound() {
        when(sessionRepository.findById("unknown-session")).thenReturn(Optional.empty());

        assertThrows(SimulationSessionNotFoundException.class, () -> {
            simulationService.getStatus("unknown-session");
        });
    }

    @Test
    void testProcessStep() {
        when(sessionRepository.findById("test-session-123")).thenReturn(Optional.of(testSession));
        when(stepRepository.save(any(SimulationStep.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(sessionRepository.save(any(SimulationSession.class))).thenReturn(testSession);

        RecipeStepRequestDto stepRequest = new RecipeStepRequestDto(
                1,
                "Test step",
                1,
                "180°C",
                "200g",
                "Test notes"
        );

        RecipeStepResponseDto response = simulationService.processStep("test-session-123", stepRequest);

        assertNotNull(response);
        assertEquals("test-session-123", response.sessionId());
        assertEquals(1, response.stepNumber());
        assertTrue(response.completed());
        assertEquals("COMPLETED", response.status());
    }

    @Test
    void testProcessStepNotRunning() {
        SimulationSession pausedSession = new SimulationSession();
        pausedSession.setId("test-session-123");
        pausedSession.setStatus(SimulationStatus.PAUSED);

        when(sessionRepository.findById("test-session-123")).thenReturn(Optional.of(pausedSession));

        RecipeStepRequestDto stepRequest = new RecipeStepRequestDto(
                1,
                "Test step",
                1,
                "180°C",
                "200g",
                "Test notes"
        );

        assertThrows(InvalidSimulationStateException.class, () -> {
            simulationService.processStep("test-session-123", stepRequest);
        });
    }

    @Test
    void testPauseSession() {
        when(sessionRepository.findById("test-session-123")).thenReturn(Optional.of(testSession));
        when(sessionRepository.save(any(SimulationSession.class))).thenReturn(testSession);
        when(stepRepository.findBySessionIdOrderByStepNumberAsc("test-session-123")).thenReturn(new ArrayList<>());

        SimulationStatusResponseDto response = simulationService.pause("test-session-123");

        assertNotNull(response);
        verify(sessionRepository, times(1)).save(argThat(session -> session.getStatus() == SimulationStatus.PAUSED));
    }

    @Test
    void testResumeSession() {
        SimulationSession pausedSession = new SimulationSession();
        pausedSession.setId("test-session-123");
        pausedSession.setStatus(SimulationStatus.PAUSED);

        when(sessionRepository.findById("test-session-123")).thenReturn(Optional.of(pausedSession));
        when(sessionRepository.save(any(SimulationSession.class))).thenReturn(pausedSession);
        when(stepRepository.findBySessionIdOrderByStepNumberAsc("test-session-123")).thenReturn(new ArrayList<>());

        SimulationStatusResponseDto response = simulationService.resume("test-session-123");

        assertNotNull(response);
        verify(sessionRepository, times(1)).save(argThat(session -> session.getStatus() == SimulationStatus.RUNNING));
    }

    @Test
    void testCancelSession() {
        when(sessionRepository.findById("test-session-123")).thenReturn(Optional.of(testSession));
        when(stepRepository.findBySessionIdOrderByStepNumberAsc("test-session-123")).thenReturn(new ArrayList<>());
        when(sessionRepository.save(any(SimulationSession.class))).thenReturn(testSession);

        SimulationStatusResponseDto response = simulationService.cancel("test-session-123");

        assertNotNull(response);
        verify(sessionRepository, times(1)).save(argThat(session -> session.getStatus() == SimulationStatus.CANCELLED));
    }

    @Test
    void testCompleteSession() {
        when(sessionRepository.findById("test-session-123")).thenReturn(Optional.of(testSession));
        when(stepRepository.findBySessionIdOrderByStepNumberAsc("test-session-123")).thenReturn(new ArrayList<>());
        when(sessionRepository.save(any(SimulationSession.class))).thenReturn(testSession);

        SimulationStatusResponseDto response = simulationService.complete("test-session-123");

        assertNotNull(response);
        verify(sessionRepository, times(1)).save(argThat(session -> session.getStatus() == SimulationStatus.COMPLETED));
    }

    @Test
    void testGetHistory() {
        SimulationStep step = new SimulationStep();
        step.setStepNumber(1);
        step.setRecipeName("Test step");
        step.setPreparationTime("5 seconds");
        step.setStatus(StepStatus.EXECUTED);
        step.setExecutedAt(LocalDateTime.now());

        when(sessionRepository.findById("test-session-123")).thenReturn(Optional.of(testSession));
        when(stepRepository.findBySessionIdOrderByStepNumberAsc("test-session-123")).thenReturn(List.of(step));

        var history = simulationService.history("test-session-123");

        assertNotNull(history);
        assertEquals(1, history.size());
        assertEquals(1, history.get(0).stepNumber());
        assertEquals("EXECUTED", history.get(0).status());
    }
}
