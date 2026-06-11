package com.cookmate.simulator.repository;

import com.cookmate.simulator.model.SimulationSession;
import com.cookmate.simulator.model.SimulationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SimulationSessionRepository extends JpaRepository<SimulationSession, String> {
    List<SimulationSession> findByStatusAndUserId(SimulationStatus status, String userId);
    Optional<SimulationSession> findByIdAndUserId(String id, String userId);
}
