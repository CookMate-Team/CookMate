package com.cookmate.main.service;

import com.cookmate.main.dto.StepDTO;
import com.cookmate.main.dto.StepGenerationRequest;
import com.cookmate.main.dto.StepGenerationResponse;
import com.cookmate.main.exception.StepNotFoundException;
import com.cookmate.main.mapper.StepMapper;
import com.cookmate.main.model.Step;
import com.cookmate.main.repository.StepRepository;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class StepService {

    private static final Logger logger = LoggerFactory.getLogger(StepService.class);

    private final StepRepository stepRepository;
    private final StepMapper stepMapper;

    public StepService(StepRepository stepRepository, StepMapper stepMapper) {
        this.stepRepository = stepRepository;
        this.stepMapper = stepMapper;
    }

    public StepDTO getStep(Long stepId) {
        Step step = stepRepository.findById(stepId)
            .orElseThrow(() -> new StepNotFoundException(stepId));
        return stepMapper.toDTO(step);
    }

    /**
     * Generuje kroki do przepisu z TheMealDB.
     * Jeśli kroki już istnieją dla danego mealId, zwraca istniejące.
     * W przeciwnym razie loguje i zwraca pusty response (placeholder dla LLM).
     *
     * @param request żądanie z mealId
     * @param session sesja HTTP zawierająca metadane przepisu
     * @return response z listą kroków i metadanymi
     */
    public StepGenerationResponse generateSteps(StepGenerationRequest request, HttpSession session) {
        String mealId = request.mealId();

        // 1. Sprawdz czy kroki juz istnieja
        List<Step> existingSteps = stepRepository.findByRecipeIdOrderByStepNumber(mealId);
        
        if (!existingSteps.isEmpty()) {
            logger.info("Zwracanie istniejących kroków dla mealId: {}", mealId);
            List<StepDTO> stepDTOs = existingSteps.stream()
                .map(stepMapper::toDTO)
                .collect(Collectors.toList());
            
            String recipeName = (String) session.getAttribute("recipeName");
            return new StepGenerationResponse(mealId, recipeName, stepDTOs);
        }
        
        // 2. Kroki nie istnieją - zalogować i placeholder dla LLM
        logger.info("Brak istniejących kroków dla mealId: {}. TODO: Integracja z Hugging Face LLM", mealId);
        logger.debug("Metadane przepisu z sesji: recipeName={}", session.getAttribute("recipeName"));
        
        // 3. Na razie zwrócić pusty response z metadanymi
        String recipeName = (String) session.getAttribute("recipeName");
        return new StepGenerationResponse(mealId, recipeName, List.of());
    }
}
