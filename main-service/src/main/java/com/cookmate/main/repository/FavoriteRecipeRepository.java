package com.cookmate.main.repository;

import com.cookmate.main.model.FavoriteRecipe;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FavoriteRecipeRepository extends JpaRepository<FavoriteRecipe, Long> {
    
    Page<FavoriteRecipe> findByUserId(String userId, Pageable pageable);
    
    boolean existsByUserIdAndRecipeId(String userId, String recipeId);
    
    Optional<FavoriteRecipe> findByUserIdAndRecipeId(String userId, String recipeId);
    
    void deleteByUserIdAndRecipeId(String userId, String recipeId);
}
