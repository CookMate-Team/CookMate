package com.cookmate.main.service;

import com.cookmate.main.dto.StepCompletionEventDto;
import com.cookmate.main.model.SimulationProgress;
import com.cookmate.main.repository.SimulationProgressRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SimulationProgressService {

    private static final Logger logger = LoggerFactory.getLogger(SimulationProgressService.class);

    private final SimulationProgressRepository simulationProgressRepository;

    /**
     * Zapisuje event wykonania kroku z симуlatora.
     * Jeśli event dla danego sessionId i stepNumber już istnieje, go nie duplikuje.
     *
     * @param event event zawierający dane o wykonanym kroku
     * @return zapisany lub istniejący SimulationProgress
     */
    @Transactional
    public SimulationProgress handleStepCompletionEvent(StepCompletionEventDto event) {
        logger.info("Otrzymano event wykonania kroku: sessionId={}, stepNumber={}, recipeId={}",
                event.sessionId(), event.stepNumber(), event.recipeId());

        // Sprawdzić czy event już istnieje (deduplikacja)
        if (simulationProgressRepository.existsBySessionIdAndStepNumber(event.sessionId(), event.stepNumber())) {
            logger.info("Event już istnieje: sessionId={}, stepNumber={}", event.sessionId(), event.stepNumber());
            return simulationProgressRepository.findBySessionIdOrderByStepNumberAsc(event.sessionId())
                    .stream()
                    .filter(p -> p.getStepNumber().equals(event.stepNumber()))
                    .findFirst()
                    .orElse(null);
        }

        SimulationProgress progress = SimulationProgress.builder()
                .sessionId(event.sessionId())
                .stepNumber(event.stepNumber())
                .status(event.status())
                .executedAt(event.executedAt())
                .recipeId(event.recipeId())
                .build();

        SimulationProgress saved = simulationProgressRepository.save(progress);
        logger.info("Event zapisany pomyślnie: sessionId={}, stepNumber={}, id={}", 
                event.sessionId(), event.stepNumber(), saved.getId());

        return saved;
    }

    /**
     * Pobiera historię kroków dla danej sesji symulacji.
     *
     * @param sessionId ID sesji symulacji
     * @return lista kroków posortowana rosnąco po numerze kroku
     */
    public List<SimulationProgress> getSessionHistory(String sessionId) {
        logger.info("Pobieranie historii dla sessionId={}", sessionId);
        return simulationProgressRepository.findBySessionIdOrderByStepNumberAsc(sessionId);
    }

    /**
     * Pobiera wszystkie eventy dla danego przepisu (wszystkie sesje).
     *
     * @param recipeId ID przepisu
     * @return lista eventów posortowana malejąco po dacie utworzenia
     */
    public List<SimulationProgress> getRecipeProgress(String recipeId) {
        logger.info("Pobieranie postępu dla recipeId={}", recipeId);
        return simulationProgressRepository.findByRecipeIdOrderByCreatedAtDesc(recipeId);
    }

    /**
     * Pobiera aktualny stan sesji (najnowszy wykonany krok).
     *
     * @param sessionId ID sesji symulacji
     * @return ostatnio wykonany krok lub null jeśli brak
     */
    public SimulationProgress getLatestProgress(String sessionId) {
        List<SimulationProgress> history = getSessionHistory(sessionId);
        return history.isEmpty() ? null : history.get(history.size() - 1);
    }
}
