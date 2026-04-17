package com.cookmate.simulator.service;

import com.cookmate.simulator.client.MainServiceClient;
import com.cookmate.simulator.config.SimulationProperties;
import com.cookmate.simulator.dto.HealthCheckResponseDto;
import com.cookmate.simulator.dto.MealPlanItemDto;
import com.cookmate.simulator.dto.MealPlanResponseDto;
import com.cookmate.simulator.dto.RecipeDto;
import com.cookmate.simulator.exception.MainServiceCommunicationException;
import com.cookmate.simulator.model.HealthStatus;
import feign.FeignException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Supplier;

@Service
public class SimulationService {

    private static final String EMPTY_RECIPES_MESSAGE = "No recipes available. Please add recipes to main-service first.";

    private final MainServiceClient mainServiceClient;
    private final SimulationProperties simulationProperties;
    private final Random random;

    public SimulationService(
            MainServiceClient mainServiceClient,
            SimulationProperties simulationProperties,
            Random random
    ) {
        this.mainServiceClient = mainServiceClient;
        this.simulationProperties = simulationProperties;
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
}
