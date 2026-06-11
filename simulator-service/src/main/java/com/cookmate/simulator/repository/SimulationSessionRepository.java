package com.cookmate.simulator.repository;

import com.cookmate.simulator.model.SimulationSession;
import com.cookmate.simulator.model.SimulationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SimulationSessionRepository extends JpaRepository<SimulationSession, String> {
    List<SimulationSession> findByStatus(SimulationStatus status);
    List<SimulationSession> findByStatusAndUserId(SimulationStatus status, String userId);
}
