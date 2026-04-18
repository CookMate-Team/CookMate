package com.cookmate.simulator.repository;

import com.cookmate.simulator.model.SimulationStep;
import com.cookmate.simulator.model.StepStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SimulationStepRepository extends JpaRepository<SimulationStep, Long> {

    List<SimulationStep> findBySessionIdOrderByStepNumberAsc(String sessionId);

    Optional<SimulationStep> findFirstBySessionIdAndStatusOrderByStepNumberAsc(String sessionId, StepStatus status);

    long countBySessionIdAndStatus(String sessionId, StepStatus status);
}
