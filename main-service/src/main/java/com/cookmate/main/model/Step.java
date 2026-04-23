package com.cookmate.main.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.accessibility.AccessibleIcon;
import java.time.LocalDateTime;
@Entity
@Table(name = "steps")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Step {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "step_number", nullable = false)
    private int stepNumber;

    @Column(nullable = false, length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ActionType action;

    @Column(columnDefinition = "TEXT")
    private String parameters;

    @Column(name = "duration_seconds")
    @PositiveOrZero
    private Integer duration;

    @Column(name = "recipe_id", nullable = false)
    private String recipeId;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
