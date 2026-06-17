package com.cookmate.mealplanner.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "weekly_plans", indexes = {
        @Index(name = "idx_weekly_plans_user_id", columnList = "user_id")
})
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class WeeklyPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false, length = 36)
    private String userId;

    @Column(name = "meals_per_day", nullable = false)
    private int mealsPerDay;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "weeklyPlan", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<WeeklyPlanMeal> meals = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
