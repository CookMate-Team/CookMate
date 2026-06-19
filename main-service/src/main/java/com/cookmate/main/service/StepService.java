package com.cookmate.main.service;

import com.cookmate.main.dto.LLMStepDTO;
import com.cookmate.main.dto.Meal;
import com.cookmate.main.dto.StepDTO;
import com.cookmate.main.dto.StepGenerationRequest;
import com.cookmate.main.dto.StepGenerationResponse;
import com.cookmate.main.exception.ExternalServiceException;
import com.cookmate.main.exception.MealNotFoundException;
import com.cookmate.main.exception.StepNotFoundException;
import com.cookmate.main.mapper.StepMapper;
import com.cookmate.main.model.Step;
import com.cookmate.main.repository.StepRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cookmate.main.model.ActionType;
import com.cookmate.main.dto.CustomStepGenerationRequest;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StepService {

    private static final Logger logger = LoggerFactory.getLogger(StepService.class);

    private final StepRepository stepRepository;
    private final StepMapper stepMapper;
    private final MealDbClient mealDbClient;
    private final GroqClient groqClient;

    public StepDTO getStep(Long stepId) {
        Step step = stepRepository.findById(stepId)
            .orElseThrow(() -> new StepNotFoundException(stepId));
        return stepMapper.toDTO(step);
    }

    /**
     * Pobiera wszystkie kroki przypisane do konkretnego przepisu.
     * Kroki są posortowane rosnąco po numerze kroku.
     *
     * @param recipeId ID przepisu
     * @return lista StepDTO dla danego przepisu, posortowana po stepNumber
     */
    public List<StepDTO> getStepsByRecipeId(String recipeId) {
        return stepRepository.findByRecipeIdOrderByStepNumberAsc(recipeId)
            .stream()
            .map(stepMapper::toDTO)
            .collect(Collectors.toList());
    }

    /**
     * Zwraca mapę z czasami wykonania dla poszczególnych przepisów (sumując duration_minutes).
     *
     * @param recipeIds lista ID przepisów
     * @return mapa id -> czas przygotowania
     */
    public Map<String, Integer> getPreparationTimes(List<String> recipeIds) {
        if (recipeIds == null || recipeIds.isEmpty()) {
            return Collections.emptyMap();
        }

        List<Object[]> results = stepRepository.sumDurationByRecipeIds(recipeIds);
        Map<String, Integer> times = new HashMap<>();

        for (Object[] result : results) {
            String recipeId = (String) result[0];
            Number sum = (Number) result[1];
            if (sum != null) {
                times.put(recipeId, sum.intValue());
            }
        }

        return times;
    }

    /**
     * Generuje kroki do przepisu z TheMealDB.
     * Jeśli kroki już istnieją dla danego mealId, zwraca istniejące.
     * W przeciwnym razie pobiera przepis z TheMealDB, wysyła do LLM i zapisuje kroki.
     *
     * @param request żądanie z mealId
     * @return response z listą kroków i metadanymi
     */
    @Transactional
    public StepGenerationResponse generateSteps(StepGenerationRequest request) {
        String mealId = request.mealId();

        // 1. Sprawdzić czy kroki już istnieją dla tego mealId
        List<Step> existingSteps = stepRepository.findByRecipeIdOrderByStepNumberAsc(mealId);

        if (!existingSteps.isEmpty()) {
            logger.info("Zwracanie istniejących kroków dla mealId: {}", mealId);
            List<StepDTO> stepDTOs = existingSteps.stream()
                .map(stepMapper::toDTO)
                .toList();

            return new StepGenerationResponse(mealId, null, stepDTOs);
        }

        // 2. Kroki nie istnieją - pobrać przepis z TheMealDB
        logger.info("Pobieranie przepisu z TheMealDB dla mealId: {}", mealId);

        var mealResponse = mealDbClient.lookupById(mealId).block();
        if (mealResponse == null || mealResponse.meals() == null || mealResponse.meals().isEmpty()) {
            throw new MealNotFoundException(mealId);
        }

        var meal = mealResponse.meals().get(0);
        String recipeInstructions = meal.getStrInstructions();
        String recipeName = meal.getStrMeal();

        List<String> ingredients = parseIngredients(meal);
        String formattedIngredients = ingredients.stream()
                .map(ing -> "- " + ing)
                .collect(Collectors.joining("\n"));

        logger.info("Wysyłanie przepisu '{}' z {} składnikami do Groq LLM...", recipeName, ingredients.size());
        var llmResponse = groqClient.generateSteps(recipeInstructions, formattedIngredients).block();
        if (llmResponse == null || llmResponse.steps().isEmpty()) {
            throw new ExternalServiceException("Groq", new IllegalStateException("Generated steps are empty"));
        }

        // 3. Uporządkowanie kroków i nałożenie reguł bezpieczeństwa (Guardrails)
        List<LLMStepDTO> sortedLlmSteps = llmResponse.steps().stream()
                .sorted(java.util.Comparator.comparing(LLMStepDTO::stepNumber))
                .toList();

        logger.info("Zapisywanie {} kroków do bazy dla mealId: {}", sortedLlmSteps.size(), mealId);
        List<Step> stepsToSave = new java.util.ArrayList<>();
        for (int i = 0; i < sortedLlmSteps.size(); i++) {
            var llmStep = sortedLlmSteps.get(i);
            int sequentialStepNumber = i + 1;
            Map<String, Object> guardedParams = applyGuardrails(llmStep.action(), llmStep.parameters(), sequentialStepNumber);

            stepsToSave.add(Step.builder()
                .stepNumber(sequentialStepNumber)
                .description(llmStep.description())
                .action(llmStep.action())
                .mainIngredient(llmStep.mainIngredient())
                .durationMinutes(llmStep.duration())
                .parameters(guardedParams)
                .recipeId(mealId)
                .build());
        }

        List<Step> savedSteps = stepRepository.saveAll(stepsToSave);

        // 4. Zmapować na DTOs i zwrócić
        List<StepDTO> stepDTOs = savedSteps.stream()
            .map(stepMapper::toDTO)
            .toList();

        return new StepGenerationResponse(mealId, recipeName, stepDTOs);
    }

    /**
     * Generuje kroki dla własnego przepisu użytkownika.
     * Nie zapisuje kroków w bazie danych. Zwraca je jako podgląd.
     *
     * @param request żądanie zawierające instrukcje i składniki
     * @return response z listą wygenerowanych kroków
     */
    public StepGenerationResponse generateCustomStepsPreview(CustomStepGenerationRequest request) {
        String recipeInstructions = request.instructions();
        String formattedIngredients = request.ingredients();

        logger.info("Wysyłanie własnego przepisu do Groq LLM...");
        var llmResponse = groqClient.generateSteps(recipeInstructions, formattedIngredients).block();
        if (llmResponse == null || llmResponse.steps().isEmpty()) {
            throw new ExternalServiceException("Groq", new IllegalStateException("Generated steps are empty"));
        }

        List<LLMStepDTO> sortedLlmSteps = llmResponse.steps().stream()
                .sorted(java.util.Comparator.comparing(LLMStepDTO::stepNumber))
                .toList();

        List<StepDTO> stepDTOs = new java.util.ArrayList<>();
        for (int i = 0; i < sortedLlmSteps.size(); i++) {
            var llmStep = sortedLlmSteps.get(i);
            int sequentialStepNumber = i + 1;
            Map<String, Object> guardedParams = applyGuardrails(llmStep.action(), llmStep.parameters(), sequentialStepNumber);

            stepDTOs.add(StepDTO.builder()
                .stepNumber(sequentialStepNumber)
                .description(llmStep.description())
                .action(llmStep.action())
                .mainIngredient(llmStep.mainIngredient())
                .durationMinutes(llmStep.duration())
                .parameters(guardedParams)
                .recipeId("preview")
                .build());
        }

        return new StepGenerationResponse("preview", "Własny przepis", stepDTOs);
    }

    private List<String> parseIngredients(Meal meal) {
        List<String> ingredientsList = new java.util.ArrayList<>();
        addIngredientIfValid(ingredientsList, meal.getStrIngredient1(), meal.getStrMeasure1());
        addIngredientIfValid(ingredientsList, meal.getStrIngredient2(), meal.getStrMeasure2());
        addIngredientIfValid(ingredientsList, meal.getStrIngredient3(), meal.getStrMeasure3());
        addIngredientIfValid(ingredientsList, meal.getStrIngredient4(), meal.getStrMeasure4());
        addIngredientIfValid(ingredientsList, meal.getStrIngredient5(), meal.getStrMeasure5());
        addIngredientIfValid(ingredientsList, meal.getStrIngredient6(), meal.getStrMeasure6());
        addIngredientIfValid(ingredientsList, meal.getStrIngredient7(), meal.getStrMeasure7());
        addIngredientIfValid(ingredientsList, meal.getStrIngredient8(), meal.getStrMeasure8());
        addIngredientIfValid(ingredientsList, meal.getStrIngredient9(), meal.getStrMeasure9());
        addIngredientIfValid(ingredientsList, meal.getStrIngredient10(), meal.getStrMeasure10());
        addIngredientIfValid(ingredientsList, meal.getStrIngredient11(), meal.getStrMeasure11());
        addIngredientIfValid(ingredientsList, meal.getStrIngredient12(), meal.getStrMeasure12());
        addIngredientIfValid(ingredientsList, meal.getStrIngredient13(), meal.getStrMeasure13());
        addIngredientIfValid(ingredientsList, meal.getStrIngredient14(), meal.getStrMeasure14());
        addIngredientIfValid(ingredientsList, meal.getStrIngredient15(), meal.getStrMeasure15());
        addIngredientIfValid(ingredientsList, meal.getStrIngredient16(), meal.getStrMeasure16());
        addIngredientIfValid(ingredientsList, meal.getStrIngredient17(), meal.getStrMeasure17());
        addIngredientIfValid(ingredientsList, meal.getStrIngredient18(), meal.getStrMeasure18());
        addIngredientIfValid(ingredientsList, meal.getStrIngredient19(), meal.getStrMeasure19());
        addIngredientIfValid(ingredientsList, meal.getStrIngredient20(), meal.getStrMeasure20());
        return ingredientsList;
    }

    private void addIngredientIfValid(List<String> list, String ingredient, String measure) {
        if (ingredient != null && !ingredient.trim().isEmpty()) {
            String formatted = ingredient.trim();
            if (measure != null && !measure.trim().isEmpty()) {
                formatted += " (" + measure.trim() + ")";
            }
            list.add(formatted);
        }
    }

    private Map<String, Object> applyGuardrails(ActionType action, Map<String, Object> rawParams, int stepNum) {
        Map<String, Object> params = rawParams == null ? new java.util.HashMap<>() : new java.util.HashMap<>(rawParams);

        int temperature = getIntParam(params, "temperature", 0);
        int speed = getIntParam(params, "speed", 0);

        boolean modified = false;

        // 1. WEIGH & POUR: Wymuszenie temperatury 0°C oraz obrotów 0
        if (action == ActionType.WEIGH || action == ActionType.POUR) {
            if (temperature != 0) {
                temperature = 0;
                modified = true;
            }
            if (speed != 0) {
                speed = 0;
                modified = true;
            }
        }

        // 2. CHOP & CUT: Wymuszenie temperatury 0°C
        if (action == ActionType.CHOP || action == ActionType.CUT) {
            if (temperature != 0) {
                temperature = 0;
                modified = true;
            }
        }

        // 3. POT & FRYING_PAN: Ograniczenie prędkości obrotowej do maksymalnie 3
        if (action == ActionType.POT || action == ActionType.FRYING_PAN) {
            if (speed > 3) {
                speed = 3;
                modified = true;
            }
        }

        // 4. BLEND: Ograniczenie obrotów do maksymalnie 4 w temperaturach powyżej 60°C
        if (action == ActionType.BLEND) {
            if (temperature > 60 && speed > 4) {
                speed = 4;
                modified = true;
            }
        }

        if (modified) {
            logger.warn("Skorygowano parametry bezpieczeństwa (Guardrails) dla kroku {}: akcja={}, nowa_temperatura={}, nowa_prędkość={}",
                    stepNum, action, temperature, speed);
            params.put("temperature", temperature);
            params.put("speed", speed);
        } else {
            if (!params.containsKey("temperature")) {
                params.put("temperature", temperature);
            }
            if (!params.containsKey("speed")) {
                params.put("speed", speed);
            }
        }

        return params;
    }

    private int getIntParam(Map<String, Object> params, String key, int defaultValue) {
        Object val = params.get(key);
        if (val == null) {
            return defaultValue;
        }
        if (val instanceof Number) {
            return ((Number) val).intValue();
        }
        try {
            return Integer.parseInt(val.toString());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    @Transactional
    public void saveCustomSteps(String recipeId, List<StepDTO> stepDTOs) {
        stepRepository.deleteByRecipeId(recipeId);
        if (stepDTOs == null || stepDTOs.isEmpty()) return;

        List<Step> stepsToSave = stepDTOs.stream().map(dto -> Step.builder()
                .stepNumber(dto.stepNumber())
                .description(dto.description())
                .action(dto.action())
                .mainIngredient(dto.mainIngredient())
                .durationMinutes(dto.durationMinutes())
                .parameters(dto.parameters())
                .recipeId(recipeId)
                .build()).collect(Collectors.toList());

        stepRepository.saveAll(stepsToSave);
    }

    @Transactional
    public void deleteStepsByRecipeId(String recipeId) {
        stepRepository.deleteByRecipeId(recipeId);
    }
}
