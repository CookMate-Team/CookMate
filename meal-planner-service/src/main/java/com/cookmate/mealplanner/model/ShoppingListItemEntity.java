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
@Table(name = "shopping_list_items", indexes = {
        @Index(name = "idx_shopping_list_items_list_id", columnList = "shopping_list_id")
})
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class ShoppingListItemEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shopping_list_id", nullable = false)
    private ShoppingList shoppingList;

    @Column(name = "ingredient_name", nullable = false)
    private String ingredientName;

    @Column(columnDefinition = "TEXT")
    private String measures;

    @Column(name = "recipe_names", columnDefinition = "TEXT")
    private String recipeNames;
}
