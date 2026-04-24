package com.cookmate.main.repository;


import com.cookmate.main.model.Step;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
// automatycznie  CRUD, tworzenie zapytania po metodie zapytania
@Repository
public interface StepRepository extends JpaRepository<Step, Long> {

    // Pobiera wszystkie kroki
    //SELECT * FROM steps WHERE recipe_id = ?
    List<Step> findByRecipeIdOrderByStepNumber(String recipeId);

    // Popiera konkretny krok - upewniając sie że należy do odpowiedniego przepisu
    //SELECT * FROM steps WHERE recipe_id = ? WHERE id = ? AND recipe_id = ?.
    Optional<Step> findByIdAndRecipeId(Long id, String recipeId);
}
