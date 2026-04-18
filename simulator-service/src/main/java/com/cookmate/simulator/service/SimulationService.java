package com.cookmate.simulator.service;

import com.cookmate.simulator.client.MainServiceClient;
import com.cookmate.simulator.config.SimulationProperties;
import com.cookmate.simulator.dto.HealthCheckResponseDto;
import com.cookmate.simulator.dto.MealPlanItemDto;
import com.cookmate.simulator.dto.MealPlanResponseDto;
import com.cookmate.simulator.dto.RecipeDto;
import com.cookmate.simulator.dto.SimulationStatusResponseDto;
import com.cookmate.simulator.dto.SimulationStepHistoryItemDto;
import com.cookmate.simulator.exception.InvalidSimulationStateException;
import com.cookmate.simulator.exception.MainServiceCommunicationException;
import com.cookmate.simulator.exception.SimulationSessionNotFoundException;
import com.cookmate.simulator.model.HealthStatus;
import com.cookmate.simulator.model.SimulationSession;
import com.cookmate.simulator.model.SimulationStatus;
import com.cookmate.simulator.model.SimulationStep;
import com.cookmate.simulator.model.StepStatus;
import com.cookmate.simulator.repository.SimulationSessionRepository;
import com.cookmate.simulator.repository.SimulationStepRepository;
import feign.FeignException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Supplier;

@Service
public class SimulationService {

    private final ConcurrentMap<String, Object> sessionExecutionLocks = new ConcurrentHashMap<>();

    private Object getExecutionLock(String sessionId) {
        return sessionExecutionLocks.computeIfAbsent(sessionId, key -> new Object());
    }

    private static final String EMPTY_RECIPES_MESSAGE = "No recipes available. Please add recipes to main-service first.";
    private static final String ONLY_RUNNING_ALLOWED = "Only RUNNING simulation can execute steps.";

    private final MainServiceClient mainServiceClient;
    private final SimulationProperties simulationProperties;
    private final SimulationSessionRepository simulationSessionRepository;
    private final SimulationStepRepository simulationStepRepository;
    private final Random random;

    public SimulationService(
            MainServiceClient mainServiceClient,
            SimulationProperties simulationProperties,
            SimulationSessionRepository simulationSessionRepository,
            SimulationStepRepository simulationStepRepository,
            Random random
    ) {
        this.mainServiceClient = mainServiceClient;
        this.simulationProperties = simulationProperties;
        this.simulationSessionRepository = simulationSessionRepository;
        this.simulationStepRepository = simulationStepRepository;
        this.random = random;
    }

    public List<RecipeDto> listRecipes() {
        return executeMainServiceCall(mainServiceClient::getAllRecipes);
    }

    public RecipeDto getRecipe(Long id) {
        return executeMainServiceCall(() -> mainServiceClient.getRecipeById(id));
    }

    public MealPlanResponseDto generateMealPlan(Integer requestedDays) {
        int days = normalizeDays(requestedDays);
        List<RecipeDto> recipes = listRecipes();
        List<MealPlanItemDto> plan = new ArrayList<>();

        if (recipes.isEmpty()) {
            return new MealPlanResponseDto(days, 0, EMPTY_RECIPES_MESSAGE, plan);
        }

        for (int day = 1; day <= days; day++) {
            RecipeDto recipe = recipes.get(random.nextInt(recipes.size()));
            plan.add(new MealPlanItemDto(
                    day,
                    recipe.getId(),
                    recipe.getName(),
                    formatPreparationTime(recipe.getPreparationTimeMinutes())
            ));
        }

        return new MealPlanResponseDto(days, recipes.size(), null, plan);
    }

    @Transactional
    public SimulationStatusResponseDto start(Integer requestedDays) {
        int days = normalizeDays(requestedDays);
        List<RecipeDto> recipes = listRecipes();

        SimulationSession session = new SimulationSession();
        session.setId(UUID.randomUUID().toString());
        session.setCurrentStep(0);
        session.setTotalRecipes(recipes.size());

        if (recipes.isEmpty()) {
            session.setStatus(SimulationStatus.COMPLETED);
            session.setTotalSteps(0);
            session.setMessage(EMPTY_RECIPES_MESSAGE);
            session.setCompletedAt(LocalDateTime.now());
            simulationSessionRepository.save(session);
            return mapStatusResponse(session, List.of());
        }

        session.setStatus(SimulationStatus.RUNNING);
        session.setTotalSteps(days);
        session.setMessage(null);
        simulationSessionRepository.save(session);

        List<SimulationStep> steps = new ArrayList<>(days);
        for (int step = 1; step <= days; step++) {
            RecipeDto recipe = recipes.get(random.nextInt(recipes.size()));

            SimulationStep simulationStep = new SimulationStep();
            simulationStep.setSessionId(session.getId());
            simulationStep.setStepNumber(step);
            simulationStep.setRecipeId(recipe.getId());
            simulationStep.setRecipeName(recipe.getName());
            simulationStep.setPreparationTime(formatPreparationTime(recipe.getPreparationTimeMinutes()));
            simulationStep.setStatus(StepStatus.PENDING);

            steps.add(simulationStep);
        }

        simulationStepRepository.saveAll(steps);
        return mapStatusResponse(session, steps);
    }

    public SimulationStatusResponseDto getStatus(String sessionId) {
        SimulationSession session = getSessionOrThrow(sessionId);
        List<SimulationStep> steps = simulationStepRepository.findBySessionIdOrderByStepNumberAsc(sessionId);
        return mapStatusResponse(session, steps);
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

    public HealthCheckResponseDto serviceHealthCheck() {
        try {
            int recipeCount = mainServiceClient.getAllRecipes().size();
            return new HealthCheckResponseDto(HealthStatus.OK.name(), "REACHABLE", String.valueOf(recipeCount), null);
        } catch (FeignException exception) {
            return new HealthCheckResponseDto(
                    HealthStatus.DEGRADED.name(),
                    "UNREACHABLE",
                    null,
                    resolveErrorMessage(exception)
            );
        }
    }

    private <T> T executeMainServiceCall(Supplier<T> call) {
        try {
            return call.get();
        } catch (FeignException exception) {
            throw new MainServiceCommunicationException("Main service is currently unavailable.", exception);
        }
    }

    private int normalizeDays(Integer requestedDays) {
        if (requestedDays == null || requestedDays < simulationProperties.getMinDays()) {
            return simulationProperties.getDefaultDays();
        }

        return Math.min(requestedDays, simulationProperties.getMaxDays());
    }

    private String formatPreparationTime(Integer preparationTimeMinutes) {
        if (preparationTimeMinutes == null) {
            return "N/A";
        }
        return preparationTimeMinutes + " minutes";
    }

    private String resolveErrorMessage(Exception exception) {
        if (exception.getMessage() == null || exception.getMessage().isBlank()) {
            return "Connection failed";
        }
        return exception.getMessage();
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
}
