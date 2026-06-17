package com.cookmate.mealplanner.repository;

import com.cookmate.mealplanner.model.ShoppingList;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ShoppingListRepository extends JpaRepository<ShoppingList, UUID> {

    @EntityGraph(attributePaths = "items")
    List<ShoppingList> findByUserIdOrderByCreatedAtDesc(String userId);
}
