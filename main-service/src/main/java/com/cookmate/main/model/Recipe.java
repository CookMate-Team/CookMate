package com.cookmate.main.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

/**
 * Recipe entity representing a cooking recipe.
 * Stores recipe details including name, ingredients, instructions, and preparation time.
 */
@Entity
@Table(name = "recipes")
public class Recipe {

    /**
     * Unique identifier for the recipe (auto-generated).
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Recipe name (required, cannot be blank).
     */
    @NotBlank(message = "Recipe name cannot be blank")
    @Column(nullable = false)
    private String name;

    /**
     * Detailed description of the recipe.
     */
    @Column(columnDefinition = "TEXT")
    private String description;

    /**
     * List of ingredients required for the recipe (required, cannot be blank).
     */
    @NotBlank(message = "Ingredients cannot be blank")
    @Column(nullable = false)
    private String ingredients;

    /**
     * Step-by-step cooking instructions.
     */
    @Column(columnDefinition = "TEXT")
    private String instructions;

    /**
     * Preparation time in minutes (non-negative if provided).
     */
    @Min(value = 0, message = "Preparation time must be non-negative")
    @Column(name = "preparation_time_minutes")
    private Integer preparationTimeMinutes;

    /**
     * Default number of portions for the recipe.
     */
    @Min(value = 1, message = "Portions must be at least 1")
    @Column(name = "default_portions")
    private Integer defaultPortions = 4;

    /**
     * User ID who created the recipe. Null for system/public recipes.
     */
    @Column(name = "user_id", length = 36)
    private String userId;

    /**
     * Flag indicating if the recipe is custom created by a user.
     */
    @Column(name = "is_custom", nullable = false)
    private boolean isCustom = false;

    /**
     * Timestamp when the recipe was created (auto-set on persist).
     */
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    /**
     * URL to the recipe's image.
     */
    @Column(name = "image_url", length = 1024)
    private String imageUrl;

    /**
     * Auto-set creation timestamp before persisting.
     */
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    /**
     * Default constructor.
     */
    public Recipe() {}

    /**
     * Construct a Recipe with all required fields.
     *
     * @param name Recipe name
     * @param description Recipe description
     * @param ingredients List of ingredients
     * @param instructions Cooking instructions
     * @param preparationTimeMinutes Preparation time in minutes
     */
    public Recipe(String name, String description, String ingredients, String instructions, Integer preparationTimeMinutes) {
        this.name = name;
        this.description = description;
        this.ingredients = ingredients;
        this.instructions = instructions;
        this.preparationTimeMinutes = preparationTimeMinutes;
        this.defaultPortions = 4;
        this.isCustom = false;
    }

    /**
     * Construct a custom Recipe.
     *
     * @param name Recipe name
     * @param description Recipe description
     * @param ingredients List of ingredients
     * @param instructions Cooking instructions
     * @param preparationTimeMinutes Preparation time in minutes
     * @param userId User ID of the creator
     */
    public Recipe(String name, String description, String ingredients, String instructions, Integer preparationTimeMinutes, String userId) {
        this.name = name;
        this.description = description;
        this.ingredients = ingredients;
        this.instructions = instructions;
        this.preparationTimeMinutes = preparationTimeMinutes;
        this.defaultPortions = 4;
        this.userId = userId;
        this.isCustom = true;
    }

    /**
     * Get the recipe ID.
     *
     * @return recipe ID
     */
    public Long getId() { return id; }

    /**
     * Set the recipe ID.
     *
     * @param id recipe ID
     */
    public void setId(Long id) { this.id = id; }

    /**
     * Get the recipe name.
     *
     * @return recipe name
     */
    public String getName() { return name; }

    /**
     * Set the recipe name.
     *
     * @param name recipe name
     */
    public void setName(String name) { this.name = name; }

    /**
     * Get the recipe description.
     *
     * @return recipe description
     */
    public String getDescription() { return description; }

    /**
     * Set the recipe description.
     *
     * @param description recipe description
     */
    public void setDescription(String description) { this.description = description; }

    /**
     * Get the ingredients list.
     *
     * @return ingredients
     */
    public String getIngredients() { return ingredients; }

    /**
     * Set the ingredients list.
     *
     * @param ingredients ingredients
     */
    public void setIngredients(String ingredients) { this.ingredients = ingredients; }

    /**
     * Get the cooking instructions.
     *
     * @return instructions
     */
    public String getInstructions() { return instructions; }

    /**
     * Set the cooking instructions.
     *
     * @param instructions instructions
     */
    public void setInstructions(String instructions) { this.instructions = instructions; }

    /**
     * Get the preparation time in minutes.
     *
     * @return preparation time minutes
     */
    public Integer getPreparationTimeMinutes() { return preparationTimeMinutes; }

    /**
     * Set the preparation time in minutes.
     *
     * @param preparationTimeMinutes preparation time in minutes
     */
    public void setPreparationTimeMinutes(Integer preparationTimeMinutes) {
        this.preparationTimeMinutes = preparationTimeMinutes;
    }

    /**
     * Get the default portions.
     *
     * @return default portions
     */
    public Integer getDefaultPortions() { return defaultPortions; }

    /**
     * Set the default portions.
     *
     * @param defaultPortions default portions
     */
    public void setDefaultPortions(Integer defaultPortions) { this.defaultPortions = defaultPortions; }

    /**
     * Get the creation timestamp.
     *
     * @return creation timestamp
     */
    public LocalDateTime getCreatedAt() { return createdAt; }

    /**
     * Set the creation timestamp.
     *
     * @param createdAt creation timestamp
     */
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    /**
     * Get the user ID.
     *
     * @return user ID
     */
    public String getUserId() { return userId; }

    /**
     * Set the user ID.
     *
     * @param userId user ID
     */
    public void setUserId(String userId) { this.userId = userId; }

    /**
     * Check if the recipe is custom.
     *
     * @return true if custom
     */
    public boolean isCustom() { return isCustom; }

    /**
     * Set if the recipe is custom.
     *
     * @param custom true if custom
     */
    public void setCustom(boolean custom) { isCustom = custom; }

    /**
     * Get the recipe image URL.
     *
     * @return image URL
     */
    public String getImageUrl() { return imageUrl; }

    /**
     * Set the recipe image URL.
     *
     * @param imageUrl image URL
     */
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
}
