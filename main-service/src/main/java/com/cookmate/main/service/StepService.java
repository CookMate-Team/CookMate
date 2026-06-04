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
        String recipeInstructions = meal.strInstructions();
        String recipeName = meal.strMeal();

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

    private List<String> parseIngredients(Meal meal) {
        List<String> ingredientsList = new java.util.ArrayList<>();
        addIngredientIfValid(ingredientsList, meal.strIngredient1(), meal.strMeasure1());
        addIngredientIfValid(ingredientsList, meal.strIngredient2(), meal.strMeasure2());
        addIngredientIfValid(ingredientsList, meal.strIngredient3(), meal.strMeasure3());
        addIngredientIfValid(ingredientsList, meal.strIngredient4(), meal.strMeasure4());
        addIngredientIfValid(ingredientsList, meal.strIngredient5(), meal.strMeasure5());
        addIngredientIfValid(ingredientsList, meal.strIngredient6(), meal.strMeasure6());
        addIngredientIfValid(ingredientsList, meal.strIngredient7(), meal.strMeasure7());
        addIngredientIfValid(ingredientsList, meal.strIngredient8(), meal.strMeasure8());
        addIngredientIfValid(ingredientsList, meal.strIngredient9(), meal.strMeasure9());
        addIngredientIfValid(ingredientsList, meal.strIngredient10(), meal.strMeasure10());
        addIngredientIfValid(ingredientsList, meal.strIngredient11(), meal.strMeasure11());
        addIngredientIfValid(ingredientsList, meal.strIngredient12(), meal.strMeasure12());
        addIngredientIfValid(ingredientsList, meal.strIngredient13(), meal.strMeasure13());
        addIngredientIfValid(ingredientsList, meal.strIngredient14(), meal.strMeasure14());
        addIngredientIfValid(ingredientsList, meal.strIngredient15(), meal.strMeasure15());
        addIngredientIfValid(ingredientsList, meal.strIngredient16(), meal.strMeasure16());
        addIngredientIfValid(ingredientsList, meal.strIngredient17(), meal.strMeasure17());
        addIngredientIfValid(ingredientsList, meal.strIngredient18(), meal.strMeasure18());
        addIngredientIfValid(ingredientsList, meal.strIngredient19(), meal.strMeasure19());
        addIngredientIfValid(ingredientsList, meal.strIngredient20(), meal.strMeasure20());
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
}
