package com.cookmate.cookingsession.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "cooking_sessions", indexes = {
        @Index(name = "idx_cooking_sessions_recipe_id", columnList = "recipe_id"),
        @Index(name = "idx_cooking_sessions_status", columnList = "status"),
        @Index(name = "idx_cooking_sessions_user_status", columnList = "user_id, status")
})
@EntityListeners(AuditingEntityListener.class)
public class CookingSession {

    @Id
    @Column(name = "session_id", nullable = false, length = 64)
    private String sessionId;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "recipe_id", nullable = false)
    private String recipeId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CookingSessionStatus status;

    @Column(name = "current_step", nullable = false)
    private Integer currentStep;

    @Column(name = "last_executed_at")
    private LocalDateTime lastExecutedAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;
}
