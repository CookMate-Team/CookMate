package com.cookmate.cookingsession.repository;

import com.cookmate.cookingsession.model.CookingSessionProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CookingSessionProgressRepository extends JpaRepository<CookingSessionProgress, Long> {

    Optional<CookingSessionProgress> findFirstBySessionIdAndStepNumber(String sessionId, Integer stepNumber);

    List<CookingSessionProgress> findBySessionIdOrderByStepNumberAsc(String sessionId);

    Optional<CookingSessionProgress> findFirstBySessionIdOrderByStepNumberDesc(String sessionId);
}
