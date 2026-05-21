package com.cookmate.cookingsession.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
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
@Table(
        name = "cooking_session_progress",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_cooking_session_step", columnNames = {"session_id", "step_number"})
        },
        indexes = {
                @Index(name = "idx_cooking_progress_session_id", columnList = "session_id"),
                @Index(name = "idx_cooking_progress_recipe_id", columnList = "recipe_id")
        }
)
@EntityListeners(AuditingEntityListener.class)
public class CookingSessionProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_id", nullable = false)
    private String sessionId;

    @Column(name = "recipe_id", nullable = false)
    private String recipeId;

    @Column(name = "step_number", nullable = false)
    private Integer stepNumber;

    @Column(nullable = false, length = 50)
    private String status;

    @Column(name = "executed_at", nullable = false)
    private LocalDateTime executedAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
