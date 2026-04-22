package com.cookmate.simulator.service;

import com.cookmate.simulator.dto.RecipeStepRequestDto;
import com.cookmate.simulator.dto.RecipeStepResponseDto;
import com.cookmate.simulator.dto.SimulationStatusResponseDto;
import com.cookmate.simulator.dto.SimulationStepHistoryItemDto;
import com.cookmate.simulator.exception.InvalidSimulationStateException;
import com.cookmate.simulator.exception.SimulationSessionNotFoundException;
import com.cookmate.simulator.model.SimulationSession;
import com.cookmate.simulator.model.SimulationStatus;
import com.cookmate.simulator.model.SimulationStep;
import com.cookmate.simulator.model.StepStatus;
import com.cookmate.simulator.repository.SimulationSessionRepository;
import com.cookmate.simulator.repository.SimulationStepRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
@RequiredArgsConstructor
public class SimulationService {

    private final ConcurrentMap<String, Object> sessionExecutionLocks = new ConcurrentHashMap<>();

    private Object getExecutionLock(String sessionId) {
        return sessionExecutionLocks.computeIfAbsent(sessionId, key -> new Object());
    }

    private static final String ONLY_RUNNING_ALLOWED = "Only RUNNING simulation can execute steps.";

    private final SimulationSessionRepository simulationSessionRepository;
    private final SimulationStepRepository simulationStepRepository;

    public SimulationStatusResponseDto startSession() {
        SimulationSession session = new SimulationSession();
        session.setId(UUID.randomUUID().toString());
        session.setCurrentStep(0);
        session.setTotalSteps(0);
        session.setStatus(SimulationStatus.RUNNING);
        session.setMessage(null);
        simulationSessionRepository.save(session);

        return mapStatusResponse(session, List.of());
    }

    @Transactional
    public RecipeStepResponseDto processStep(String sessionId, RecipeStepRequestDto stepDto) {
        synchronized (getExecutionLock(sessionId)) {
            SimulationSession session = getSessionOrThrow(sessionId);
            validateState(session, SimulationStatus.RUNNING, ONLY_RUNNING_ALLOWED);

            SimulationStep step = new SimulationStep();
            step.setSessionId(sessionId);
            step.setStepNumber(stepDto.stepNumber());
            step.setRecipeName(stepDto.description());
            step.setPreparationTime(stepDto.durationSeconds() + " seconds");
            step.setStatus(StepStatus.PENDING);
            simulationStepRepository.save(step);

            session.setTotalSteps(stepDto.stepNumber());
            simulationSessionRepository.save(session);

            try {
                Thread.sleep(stepDto.durationSeconds() * 1000L);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return new RecipeStepResponseDto(sessionId, stepDto.stepNumber(), false, "INTERRUPTED", "Step was interrupted");
            }

            step.setStatus(StepStatus.EXECUTED);
            step.setExecutedAt(LocalDateTime.now());
            simulationStepRepository.save(step);

            session.setCurrentStep(stepDto.stepNumber());
            simulationSessionRepository.save(session);

            return new RecipeStepResponseDto(sessionId, stepDto.stepNumber(), true, "COMPLETED", "Step completed successfully");
        }
    }

    public SimulationStatusResponseDto getStatus(String sessionId) {
        SimulationSession session = getSessionOrThrow(sessionId);
        List<SimulationStep> steps = simulationStepRepository.findBySessionIdOrderByStepNumberAsc(sessionId);
        return mapStatusResponseReadOnly(session, steps);
    }

    private SimulationStatusResponseDto mapStatusResponseReadOnly(SimulationSession session, List<SimulationStep> steps) {
        int recalculatedCurrentStep = (int) steps.stream()
                .filter(step -> step.getStatus() == StepStatus.EXECUTED)
                .count();

        if (session.getCurrentStep() == recalculatedCurrentStep) {
            return mapStatusResponse(session, steps);
        }

        return mapStatusResponse(buildResponseOnlySession(session, recalculatedCurrentStep), steps);
    }

    private SimulationSession buildResponseOnlySession(SimulationSession session, int currentStep) {
        SimulationSession responseSession = new SimulationSession();
        responseSession.setId(session.getId());
        responseSession.setCurrentStep(currentStep);
        responseSession.setStatus(session.getStatus());
        responseSession.setTotalSteps(session.getTotalSteps());
        responseSession.setMessage(session.getMessage());
        responseSession.setCompletedAt(session.getCompletedAt());
        return responseSession;
    }

    @Transactional
    public SimulationStatusResponseDto executeStep(String sessionId) {
        synchronized (getExecutionLock(sessionId)) {
            SimulationSession session = getSessionOrThrow(sessionId);
            validateState(session, SimulationStatus.RUNNING, ONLY_RUNNING_ALLOWED);

            SimulationStep nextStep = simulationStepRepository
                    .findFirstBySessionIdAndStatusOrderByStepNumberAsc(sessionId, StepStatus.PENDING)
                    .orElse(null);

            if (nextStep == null) {
                completeInternal(session);
                return getStatus(sessionId);
            }

            nextStep.setStatus(StepStatus.EXECUTED);
            nextStep.setExecutedAt(LocalDateTime.now());
            simulationStepRepository.save(nextStep);

            session.setCurrentStep(session.getCurrentStep() + 1);
            simulationSessionRepository.save(session);

            if (session.getCurrentStep() >= session.getTotalSteps()) {
                completeInternal(session);
            }

            return getStatus(sessionId);
        }
    }

    @Transactional
    public SimulationStatusResponseDto complete(String sessionId) {
        SimulationSession session = getSessionOrThrow(sessionId);

        if (session.getStatus() == SimulationStatus.CANCELLED) {
            throw new InvalidSimulationStateException("Cancelled simulation cannot be completed.");
        }
        if (session.getStatus() == SimulationStatus.COMPLETED) {
            return getStatus(sessionId);
        }
        if (session.getStatus() != SimulationStatus.RUNNING && session.getStatus() != SimulationStatus.PAUSED) {
            throw new InvalidSimulationStateException("Only RUNNING or PAUSED simulation can be completed.");
        }

        completeInternal(session);
        return getStatus(sessionId);
    }

    @Transactional
    public SimulationStatusResponseDto pause(String sessionId) {
        SimulationSession session = getSessionOrThrow(sessionId);
        validateState(session, SimulationStatus.RUNNING, "Only RUNNING simulation can be paused.");

        session.setStatus(SimulationStatus.PAUSED);
        simulationSessionRepository.save(session);
        return getStatus(sessionId);
    }

    @Transactional
    public SimulationStatusResponseDto resume(String sessionId) {
        SimulationSession session = getSessionOrThrow(sessionId);
        validateState(session, SimulationStatus.PAUSED, "Only PAUSED simulation can be resumed.");

        session.setStatus(SimulationStatus.RUNNING);
        simulationSessionRepository.save(session);
        return getStatus(sessionId);
    }

    @Transactional
    public SimulationStatusResponseDto cancel(String sessionId) {
        SimulationSession session = getSessionOrThrow(sessionId);

        if (session.getStatus() == SimulationStatus.CANCELLED) {
            return getStatus(sessionId);
        }
        if (session.getStatus() == SimulationStatus.COMPLETED) {
            throw new InvalidSimulationStateException("Completed simulation cannot be cancelled.");
        }

        markPendingAsSkipped(sessionId);
        session.setStatus(SimulationStatus.CANCELLED);
        session.setCompletedAt(LocalDateTime.now());
        simulationSessionRepository.save(session);

        return getStatus(sessionId);
    }

    public List<SimulationStepHistoryItemDto> history(String sessionId) {
        getSessionOrThrow(sessionId);
        return mapHistory(simulationStepRepository.findBySessionIdOrderByStepNumberAsc(sessionId));
    }

    private SimulationSession getSessionOrThrow(String sessionId) {
        return simulationSessionRepository.findById(sessionId)
                .orElseThrow(() -> new SimulationSessionNotFoundException(sessionId));
    }

    private void validateState(SimulationSession session, SimulationStatus expectedStatus, String message) {
        if (session.getStatus() != expectedStatus) {
            throw new InvalidSimulationStateException(message);
        }
    }

    private void completeInternal(SimulationSession session) {
        markPendingAsSkipped(session.getId());
        session.setStatus(SimulationStatus.COMPLETED);
        session.setCompletedAt(LocalDateTime.now());
        simulationSessionRepository.save(session);
    }

    private void markPendingAsSkipped(String sessionId) {
        List<SimulationStep> steps = simulationStepRepository.findBySessionIdOrderByStepNumberAsc(sessionId);
        LocalDateTime now = LocalDateTime.now();

        for (SimulationStep step : steps) {
            if (step.getStatus() == StepStatus.PENDING) {
                step.setStatus(StepStatus.SKIPPED);
                step.setExecutedAt(now);
            }
        }

        simulationStepRepository.saveAll(steps);
    }

    private SimulationStatusResponseDto mapStatusResponse(SimulationSession session, List<SimulationStep> steps) {
        long executedSteps = simulationStepRepository.countBySessionIdAndStatus(session.getId(), StepStatus.EXECUTED);
        int recalculatedCurrentStep = (int) executedSteps;
        if (session.getCurrentStep() != recalculatedCurrentStep) {
            session.setCurrentStep(recalculatedCurrentStep);
            simulationSessionRepository.save(session);
        }

        return new SimulationStatusResponseDto(
                session.getId(),
                session.getStatus().name(),
                session.getCurrentStep(),
                session.getTotalSteps(),
                0,
                session.getMessage(),
                mapHistory(steps)
        );
    }

    private List<SimulationStepHistoryItemDto> mapHistory(List<SimulationStep> steps) {
        return steps.stream()
                .map(step -> new SimulationStepHistoryItemDto(
                        step.getStepNumber(),
                        step.getRecipeId(),
                        step.getRecipeName(),
                        step.getPreparationTime(),
                        step.getStatus().name(),
                        step.getExecutedAt()
                ))
                .toList();
    }
}
