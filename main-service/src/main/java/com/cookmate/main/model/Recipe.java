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
     * Timestamp when the recipe was created (auto-set on persist).
     */
    @Column(name = "created_at")
    private LocalDateTime createdAt;

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
}
