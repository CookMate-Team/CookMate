package com.cookmate.main.repository;

import com.cookmate.main.model.SimulationProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SimulationProgressRepository extends JpaRepository<SimulationProgress, Long> {

    List<SimulationProgress> findBySessionIdOrderByStepNumberAsc(String sessionId);

    List<SimulationProgress> findByRecipeIdOrderByCreatedAtDesc(String recipeId);

    boolean existsBySessionIdAndStepNumber(String sessionId, Integer stepNumber);
}
