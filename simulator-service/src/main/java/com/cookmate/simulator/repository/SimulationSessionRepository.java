package com.cookmate.simulator.repository;

import com.cookmate.simulator.model.SimulationSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SimulationSessionRepository extends JpaRepository<SimulationSession, String> {
}
