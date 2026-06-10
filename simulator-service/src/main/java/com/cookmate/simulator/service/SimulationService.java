package com.cookmate.simulator.service;

import com.cookmate.simulator.client.CookingSessionClient;
import com.cookmate.simulator.client.MainServiceClient;
import com.cookmate.simulator.dto.MainServiceStepDto;
import com.cookmate.simulator.dto.RecipeStepRequestDto;
import com.cookmate.simulator.dto.SimulationStatusResponseDto;
import com.cookmate.simulator.dto.SimulationStepHistoryItemDto;
import com.cookmate.simulator.dto.StartSimulationRequestDto;
import com.cookmate.simulator.dto.StepExecutionResultDto;
import com.cookmate.simulator.exception.InvalidSimulationStateException;
import com.cookmate.simulator.exception.MainServiceCommunicationException;
import com.cookmate.simulator.exception.SimulationSessionNotFoundException;
import com.cookmate.simulator.model.SimulationSession;
import com.cookmate.simulator.model.SimulationStatus;
import com.cookmate.simulator.model.SimulationStep;
import com.cookmate.simulator.model.StepStatus;
import com.cookmate.simulator.repository.SimulationSessionRepository;
import com.cookmate.simulator.repository.SimulationStepRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@RequiredArgsConstructor
public class SimulationService {

    private static final String ONLY_RUNNING_ALLOWED = "Only RUNNING simulation can execute steps.";
    private final ConcurrentMap<String, Object> sessionExecutionLocks = new ConcurrentHashMap<>();

    private final SimulationSessionRepository simulationSessionRepository;
    private final SimulationStepRepository simulationStepRepository;
    private final MainServiceClient mainServiceClient;
    private final CookingSessionClient cookingSessionClient;

    @Transactional
    public SimulationStatusResponseDto startSession(StartSimulationRequestDto request, String userId, String authHeader) {
        List<SimulationSession> runningSessions = simulationSessionRepository.findByStatusAndUserId(SimulationStatus.RUNNING, userId);

        List<MainServiceStepDto> recipeSteps = fetchRecipeSteps(request.recipeId());
        if (recipeSteps.isEmpty()) {
            throw new InvalidSimulationStateException("Recipe has no steps to simulate.");
        }
        validateMainServiceSteps(recipeSteps);
        List<MainServiceStepDto> normalizedSteps = normalizeStepsForSession(recipeSteps);

        SimulationSession session = new SimulationSession();
        session.setId(UUID.randomUUID().toString());
        session.setUserId(userId);
        session.setStatus(SimulationStatus.RUNNING);
        session.setCurrentStep(0);
        session.setTotalSteps(normalizedSteps.size());
        session.setRecipeId(request.recipeId());
        session.setTotalRecipes(1);
        session.setMessage(null);
        simulationSessionRepository.save(session);

        List<SimulationStep> steps = normalizedSteps.stream()
                .map(step -> toPendingStep(session.getId(), step))
                .toList();
        simulationStepRepository.saveAll(steps);

        // Complete previously active sessions only after the new session is successfully created
        for (SimulationSession oldSession : runningSessions) {
            oldSession.setStatus(SimulationStatus.COMPLETED);
            oldSession.setCompletedAt(LocalDateTime.now());
            simulationSessionRepository.save(oldSession);
            notifyCookingSessionAsync(
                    oldSession.getId(),
                    oldSession.getCurrentStep(),
                    "COMPLETED",
                    LocalDateTime.now(),
                    oldSession.getRecipeId(),
                    authHeader
            );
        }

        notifyCookingSessionAsync(
                session.getId(),
                0,
                "RUNNING",
                LocalDateTime.now(),
                session.getRecipeId(),
                authHeader
        );

        return mapStatusResponse(session, steps);
    }

    @Transactional
    public StepExecutionResultDto processStep(String sessionId, RecipeStepRequestDto stepDto, String userId, String authHeader) {
        synchronized (getExecutionLock(sessionId)) {
            SimulationSession session = getSessionOrThrow(sessionId, userId);
            validateState(session, SimulationStatus.RUNNING, ONLY_RUNNING_ALLOWED);

            SimulationStep step = simulationStepRepository.findBySessionIdAndStepNumber(sessionId, stepDto.stepNumber())
                    .orElseGet(SimulationStep::new);
            step.setSessionId(sessionId);
            step.setStepNumber(stepDto.stepNumber());
            step.setRecipeName(stepDto.description());
            step.setPreparationTime(stepDto.durationSeconds() + " seconds");
            step.setStatus(StepStatus.EXECUTED);
            step.setExecutedAt(LocalDateTime.now());
            simulationStepRepository.save(step);

            session.setCurrentStep(Math.max(session.getCurrentStep(), stepDto.stepNumber()));
            session.setTotalSteps(Math.max(session.getTotalSteps(), stepDto.stepNumber()));
            finalizeIfLastStep(sessionId, session);

            // Asynchronicznie wysłać notyfikację do main-service
            boolean completed = session.getStatus() == SimulationStatus.COMPLETED;
            notifyCookingSessionAsync(sessionId, stepDto.stepNumber(), completed ? "COMPLETED" : "EXECUTED", step.getExecutedAt(), session.getRecipeId(), authHeader);

            return StepExecutionResultDto.builder()
                    .stepNumber(stepDto.stepNumber())
                    .success(true)
                    .build();
        }
    }

    @Transactional
    public StepExecutionResultDto executeNextStep(String sessionId, String userId, String authHeader) {
        synchronized (getExecutionLock(sessionId)) {
            SimulationSession session = getSessionOrThrow(sessionId, userId);
            validateState(session, SimulationStatus.RUNNING, ONLY_RUNNING_ALLOWED);

            SimulationStep nextStep = simulationStepRepository
                    .findFirstBySessionIdAndStatusOrderByStepNumberAsc(sessionId, StepStatus.PENDING)
                    .orElse(null);
            if (nextStep == null) {
                finalizeIfLastStep(sessionId, session);
                return StepExecutionResultDto.builder()
                        .stepNumber(session.getCurrentStep())
                        .success(false)
                        .build();
            }

            nextStep.setStatus(StepStatus.EXECUTED);
            nextStep.setExecutedAt(LocalDateTime.now());
            simulationStepRepository.save(nextStep);

            session.setCurrentStep(nextStep.getStepNumber());
            finalizeIfLastStep(sessionId, session);

            // Asynchronicznie wysłać notyfikację do main-service
            boolean completed = session.getStatus() == SimulationStatus.COMPLETED;
            notifyCookingSessionAsync(sessionId, nextStep.getStepNumber(), completed ? "COMPLETED" : "EXECUTED", nextStep.getExecutedAt(), session.getRecipeId(), authHeader);

            return StepExecutionResultDto.builder()
                    .stepNumber(nextStep.getStepNumber())
                    .success(true)
                    .build();
        }
    }

    @Transactional
    public SimulationStatusResponseDto rewindToStep(String sessionId, int stepNumber, String userId) {
        synchronized (getExecutionLock(sessionId)) {
            SimulationSession session = getSessionOrThrow(sessionId, userId);
            List<SimulationStep> steps = simulationStepRepository.findBySessionIdOrderByStepNumberAsc(sessionId);

            if (stepNumber < 0 || stepNumber > steps.size()) {
                throw new InvalidSimulationStateException("stepNumber must be between 0 and " + steps.size());
            }

            LocalDateTime now = LocalDateTime.now();
            for (SimulationStep step : steps) {
                if (step.getStepNumber() <= stepNumber) {
                    step.setStatus(StepStatus.EXECUTED);
                    if (step.getExecutedAt() == null) {
                        step.setExecutedAt(now);
                    }
                } else {
                    step.setStatus(StepStatus.PENDING);
                    step.setExecutedAt(null);
                }
            }
            simulationStepRepository.saveAll(steps);

            session.setCurrentStep(stepNumber);
            if (stepNumber >= steps.size()) {
                session.setStatus(SimulationStatus.COMPLETED);
                session.setCompletedAt(now);
            } else {
                session.setStatus(SimulationStatus.RUNNING);
                session.setCompletedAt(null);
            }
            simulationSessionRepository.save(session);
            return mapStatusResponse(session, steps);
        }
    }

    public SimulationStatusResponseDto getStatus(String sessionId, String userId) {
        SimulationSession session = getSessionOrThrow(sessionId, userId);
        List<SimulationStep> steps = simulationStepRepository.findBySessionIdOrderByStepNumberAsc(sessionId);
        return mapStatusResponse(session, steps);
    }

    public List<SimulationStepHistoryItemDto> history(String sessionId, String userId) {
        getSessionOrThrow(sessionId, userId);
        return mapHistory(simulationStepRepository.findBySessionIdOrderByStepNumberAsc(sessionId));
    }

    private Object getExecutionLock(String sessionId) {
        return sessionExecutionLocks.computeIfAbsent(sessionId, key -> new Object());
    }

    private SimulationSession getSessionOrThrow(String sessionId, String userId) {
        return simulationSessionRepository.findByIdAndUserId(sessionId, userId)
                .orElseThrow(() -> new SimulationSessionNotFoundException(sessionId));
    }

    private void validateState(SimulationSession session, SimulationStatus expectedStatus, String message) {
        if (session.getStatus() != expectedStatus) {
            throw new InvalidSimulationStateException(message);
        }
    }

    private List<MainServiceStepDto> fetchRecipeSteps(String recipeId) {
        try {
            return mainServiceClient.getRecipeSteps(recipeId);
        } catch (Exception exception) {
            throw new MainServiceCommunicationException(
                    "Failed to fetch recipe steps from main-service for recipeId=" + recipeId,
                    exception
            );
        }
    }

    private void validateMainServiceSteps(List<MainServiceStepDto> steps) {
        for (MainServiceStepDto step : steps) {
            if (step.description() == null || step.description().isBlank()) {
                throw new InvalidSimulationStateException("Invalid step description from main-service.");
            }
        }
    }

    private List<MainServiceStepDto> normalizeStepsForSession(List<MainServiceStepDto> steps) {
        AtomicInteger nextStepNumber = new AtomicInteger(1);
        return steps.stream()
                .sorted(Comparator.comparing(MainServiceStepDto::stepNumber, Comparator.nullsLast(Integer::compareTo)))
                .map(step -> new MainServiceStepDto(
                        step.id(),
                        nextStepNumber.getAndIncrement(),
                        step.description(),
                        step.parameters(),
                        step.durationMinutes(),
                        step.recipeId()
                ))
                .toList();
    }

    private SimulationStep toPendingStep(String sessionId, MainServiceStepDto step) {
        SimulationStep simulationStep = new SimulationStep();
        simulationStep.setSessionId(sessionId);
        simulationStep.setStepNumber(step.stepNumber());
        simulationStep.setRecipeName(step.description());
        simulationStep.setPreparationTime(formatDuration(step.durationMinutes()));
        simulationStep.setStatus(StepStatus.PENDING);
        return simulationStep;
    }

    private String formatDuration(Integer durationMinutes) {
        if (durationMinutes == null) {
            return null;
        }
        return durationMinutes + " minutes";
    }

    private void finalizeIfLastStep(String sessionId, SimulationSession session) {
        long pendingSteps = simulationStepRepository.countBySessionIdAndStatus(sessionId, StepStatus.PENDING);
        if (pendingSteps == 0) {
            session.setStatus(SimulationStatus.COMPLETED);
            session.setCompletedAt(LocalDateTime.now());
        }
        simulationSessionRepository.save(session);
    }

    private SimulationStatusResponseDto mapStatusResponse(SimulationSession session, List<SimulationStep> steps) {
        int recalculatedCurrentStep = steps.stream()
                .filter(step -> step.getStatus() == StepStatus.EXECUTED)
                .mapToInt(SimulationStep::getStepNumber)
                .max()
                .orElse(0);
        if (session.getCurrentStep() != recalculatedCurrentStep) {
            session.setCurrentStep(recalculatedCurrentStep);
            simulationSessionRepository.save(session);
        }

        return new SimulationStatusResponseDto(
                session.getId(),
                session.getStatus().name(),
                session.getCurrentStep(),
                session.getTotalSteps(),
                session.getTotalRecipes(),
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

    /**
     * Asynchronicznie wysłać notyfikację o wykonanym kroku do cooking-session-service.
     * Nie blokuje bieżącego wątku - wysyłka odbywa się w tle.
     * Jeśli wysyłka się nie powiedzie, loguje błąd ale nie rzuca wyjątku.
     *
     * @param sessionId ID sesji symulacji
     * @param stepNumber numer wykonanego kroku
     * @param status status kroku (np. EXECUTED)
     * @param executedAt czas wykonania
     * @param recipeId ID przepisu
     * @param authHeader nagłówek autoryzacji JWT
     */
    private void notifyCookingSessionAsync(String sessionId, Integer stepNumber, String status, LocalDateTime executedAt, String recipeId, String authHeader) {
        final Logger logger = LoggerFactory.getLogger(SimulationService.class);
        
        CompletableFuture.runAsync(() -> {
            try {
                java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
                Map<String, Object> event = Map.of(
                        "sessionId", sessionId,
                        "stepNumber", stepNumber,
                        "status", status,
                        "executedAt", executedAt.format(formatter),
                        "recipeId", recipeId
                );
                cookingSessionClient.notifyStepCompleted(authHeader, event);
                logger.info("Notyfikacja wysłana do cooking-session-service: sessionId={}, stepNumber={}", sessionId, stepNumber);
            } catch (Exception e) {
                logger.error("Błąd podczas wysyłania notyfikacji do cooking-session-service: sessionId={}, stepNumber={}", sessionId, stepNumber, e);
            }
        });
    }

    @Transactional
    public void completeSession(String sessionId, String userId, String authHeader) {
        synchronized (getExecutionLock(sessionId)) {
            SimulationSession session = getSessionOrThrow(sessionId, userId);
            session.setStatus(SimulationStatus.COMPLETED);
            session.setCompletedAt(LocalDateTime.now());
            simulationSessionRepository.save(session);

            notifyCookingSessionAsync(
                    sessionId,
                    session.getCurrentStep(),
                    "COMPLETED",
                    LocalDateTime.now(),
                    session.getRecipeId(),
                    authHeader
            );
        }
    }
}
