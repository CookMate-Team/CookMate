package com.cookmate.main.service;

import com.cookmate.main.dto.FavoriteRecipeAddRequest;
import com.cookmate.main.dto.FavoriteRecipeDTO;
import com.cookmate.main.model.FavoriteRecipe;
import com.cookmate.main.repository.FavoriteRecipeRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FavoriteRecipeService {

    private final FavoriteRecipeRepository favoriteRecipeRepository;

    public FavoriteRecipeService(FavoriteRecipeRepository favoriteRecipeRepository) {
        this.favoriteRecipeRepository = favoriteRecipeRepository;
    }

    public Page<FavoriteRecipeDTO> getUserFavorites(String userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<FavoriteRecipe> favorites = favoriteRecipeRepository.findByUserId(userId, pageable);
        
        return favorites.map(fav -> new FavoriteRecipeDTO(
            fav.getId(),
            fav.getRecipeId(),
            fav.getRecipeTitle(),
            fav.getImageUrl(),
            fav.getCreatedAt()
        ));
    }

    @Transactional
    public FavoriteRecipeDTO addFavorite(String userId, String recipeId, FavoriteRecipeAddRequest request) {
        if (favoriteRecipeRepository.existsByUserIdAndRecipeId(userId, recipeId)) {
            // Alternatively, throw a custom exception
            throw new IllegalArgumentException("Recipe is already in favorites");
        }

        FavoriteRecipe favorite = new FavoriteRecipe(
            userId,
            recipeId,
            request.recipeTitle(),
            request.imageUrl()
        );

        FavoriteRecipe saved = favoriteRecipeRepository.save(favorite);

        return new FavoriteRecipeDTO(
            saved.getId(),
            saved.getRecipeId(),
            saved.getRecipeTitle(),
            saved.getImageUrl(),
            saved.getCreatedAt()
        );
    }

    public boolean isFavorite(String userId, String recipeId) {
        return favoriteRecipeRepository.existsByUserIdAndRecipeId(userId, recipeId);
    }

    @Transactional
    public void removeFavorite(String userId, String recipeId) {
        favoriteRecipeRepository.deleteByUserIdAndRecipeId(userId, recipeId);
    }
}
