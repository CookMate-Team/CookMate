package com.cookmate.main.service;

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

import java.util.List;
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

        logger.info("Wysyłanie przepisu '{}' do Groq LLM...", recipeName);
        var llmResponse = groqClient.generateSteps(recipeInstructions).block();
        if (llmResponse == null || llmResponse.steps().isEmpty()) {
            throw new ExternalServiceException("Groq", new IllegalStateException("Generated steps are empty"));
        }

        // 3. Zapisać kroki do bazy
        logger.info("Zapisywanie {} kroków do bazy dla mealId: {}", llmResponse.steps().size(), mealId);
        List<Step> stepsToSave = llmResponse.steps().stream()
            .map(llmStep -> Step.builder()
                .stepNumber(llmStep.stepNumber())
                .description(llmStep.description())
                .action(llmStep.action())
                .mainIngredient(llmStep.mainIngredient())
                .durationMinutes(llmStep.duration())
                .parameters(llmStep.parameters())
                .recipeId(mealId)
                .build())
            .toList();

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
}
