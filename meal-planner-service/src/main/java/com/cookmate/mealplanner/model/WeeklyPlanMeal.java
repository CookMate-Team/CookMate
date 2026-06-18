package com.cookmate.mealplanner.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "weekly_plan_meals", indexes = {
        @Index(name = "idx_weekly_plan_meals_plan_id", columnList = "weekly_plan_id")
})
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class WeeklyPlanMeal {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "weekly_plan_id", nullable = false)
    private WeeklyPlan weeklyPlan;

    @Column(name = "day_name", nullable = false, length = 20)
    private String dayName;

    @Column(name = "meal_id", nullable = false)
    private String mealId;

    @Column(name = "meal_name", nullable = false)
    private String mealName;

    @Column(name = "thumbnail_url")
    private String thumbnailUrl;
}
