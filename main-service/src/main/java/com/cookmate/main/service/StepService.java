package com.cookmate.main.service;

import com.cookmate.main.dto.StepDTO;
import com.cookmate.main.dto.StepGenerationRequest;
import com.cookmate.main.dto.StepGenerationResponse;
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
        
        try {
            var mealResponse = mealDbClient.lookupById(mealId).block();
            
            if (mealResponse == null || mealResponse.meals() == null || mealResponse.meals().isEmpty()) {
                logger.error("Przepis nie znaleziony w TheMealDB dla mealId: {}", mealId);
                return new StepGenerationResponse(mealId, null, List.of());
            }
            
            var meal = mealResponse.meals().get(0);
            String recipeInstructions = meal.strInstructions();
            String recipeName = meal.strMeal();
            
            logger.info("Wysyłanie przepisu '{}' do Groq LLM...", recipeName);
            var llmResponse = groqClient.generateSteps(recipeInstructions).block();
            
            if (llmResponse == null || llmResponse.steps().isEmpty()) {
                logger.warn("LLM zwrócił pustą listę kroków dla mealId: {}", mealId);
                return new StepGenerationResponse(mealId, recipeName, List.of());
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
            
        } catch (Exception e) {
            logger.error("Błąd podczas generowania kroków dla mealId: {}", mealId, e);
            return new StepGenerationResponse(mealId, null, List.of());
        }
    }
}
