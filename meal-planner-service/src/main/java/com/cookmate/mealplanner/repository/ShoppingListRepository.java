package com.cookmate.mealplanner.repository;

import com.cookmate.mealplanner.model.ShoppingList;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ShoppingListRepository extends JpaRepository<ShoppingList, UUID> {
}
