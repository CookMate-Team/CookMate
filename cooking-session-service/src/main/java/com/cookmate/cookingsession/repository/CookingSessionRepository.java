package com.cookmate.cookingsession.repository;

import com.cookmate.cookingsession.model.CookingSession;
import com.cookmate.cookingsession.model.CookingSessionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CookingSessionRepository extends JpaRepository<CookingSession, String> {

    List<CookingSession> findByRecipeIdAndStatus(String recipeId, CookingSessionStatus status);

    Optional<CookingSession> findFirstByRecipeIdAndStatusOrderByLastExecutedAtDesc(
            String recipeId,
            CookingSessionStatus status
    );

    List<CookingSession> findByStatus(CookingSessionStatus status);

    // Per-user queries
    Optional<CookingSession> findFirstByUserIdAndStatusOrderByLastExecutedAtDesc(
            String userId,
            CookingSessionStatus status
    );

    Optional<CookingSession> findFirstByUserIdAndRecipeIdAndStatusOrderByLastExecutedAtDesc(
            String userId,
            String recipeId,
            CookingSessionStatus status
    );

    List<CookingSession> findByUserIdAndStatus(String userId, CookingSessionStatus status);

    List<CookingSession> findByUserIdAndRecipeIdAndStatus(String userId, String recipeId, CookingSessionStatus status);
}
