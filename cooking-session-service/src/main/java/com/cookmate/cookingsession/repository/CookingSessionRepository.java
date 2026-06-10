package com.cookmate.cookingsession.repository;

import com.cookmate.cookingsession.model.CookingSession;
import com.cookmate.cookingsession.model.CookingSessionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CookingSessionRepository extends JpaRepository<CookingSession, String> {

    List<CookingSession> findByRecipeIdAndStatusAndUserId(String recipeId, CookingSessionStatus status, String userId);

    Optional<CookingSession> findFirstByRecipeIdAndStatusAndUserIdOrderByLastExecutedAtDesc(
            String recipeId,
            CookingSessionStatus status,
            String userId
    );

    List<CookingSession> findByStatusAndUserId(CookingSessionStatus status, String userId);

    Optional<CookingSession> findBySessionIdAndUserId(String sessionId, String userId);
}
