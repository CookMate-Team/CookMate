package com.cookmate.main.repository;


import com.cookmate.main.model.Step;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StepRepository extends JpaRepository<Step, Long> {

    // Pobiera wszystkie kroki
    List<Step> findByRecipeIdOrderByStepNumber(String recipeId);

    // Popiera konkretny krok - upewniając sie że należy do odpowiedniego przepisu
    Optional<Step> findByIdAndRecipeId(Long id, String recipeId);
}
