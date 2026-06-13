package com.cookmate.main.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;

/**
 * Entity representing a user's favorite recipe.
 */
@Entity
@Table(
    name = "user_favorite_recipes",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "recipe_id"})
    }
)
public class FavoriteRecipe {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(name = "user_id", nullable = false)
    private String userId;

    @NotBlank
    @Column(name = "recipe_id", nullable = false)
    private String recipeId;

    @Column(name = "recipe_title")
    private String recipeTitle;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public FavoriteRecipe() {}

    public FavoriteRecipe(String userId, String recipeId, String recipeTitle, String imageUrl) {
        this.userId = userId;
        this.recipeId = recipeId;
        this.recipeTitle = recipeTitle;
        this.imageUrl = imageUrl;
    }

    public Long getId() {
        return id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getRecipeId() {
        return recipeId;
    }

    public void setRecipeId(String recipeId) {
        this.recipeId = recipeId;
    }

    public String getRecipeTitle() {
        return recipeTitle;
    }

    public void setRecipeTitle(String recipeTitle) {
        this.recipeTitle = recipeTitle;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
