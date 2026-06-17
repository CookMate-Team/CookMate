package com.cookmate.mealplanner.repository;

import com.cookmate.mealplanner.model.WeeklyPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface WeeklyPlanRepository extends JpaRepository<WeeklyPlan, UUID> {
}
