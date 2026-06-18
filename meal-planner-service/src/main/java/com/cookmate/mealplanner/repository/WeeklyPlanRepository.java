package com.cookmate.mealplanner.repository;

import com.cookmate.mealplanner.model.WeeklyPlan;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WeeklyPlanRepository extends JpaRepository<WeeklyPlan, UUID> {

    @EntityGraph(attributePaths = "meals")
    List<WeeklyPlan> findByUserIdOrderByCreatedAtDesc(String userId);

    @EntityGraph(attributePaths = "meals")
    Optional<WeeklyPlan> findByIdAndUserId(UUID id, String userId);
}
