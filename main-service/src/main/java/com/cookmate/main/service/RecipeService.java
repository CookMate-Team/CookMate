package com.cookmate.main.service;

import com.cookmate.main.model.Recipe;
import com.cookmate.main.repository.RecipeRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class RecipeService {

    private final RecipeRepository recipeRepository;

    public RecipeService(RecipeRepository recipeRepository) {
        this.recipeRepository = recipeRepository;
    }

    public List<Recipe> findAll() {
        return recipeRepository.findAll();
    }

    public Optional<Recipe> findById(Long id) {
        return recipeRepository.findById(id);
    }

    public List<Recipe> findByName(String name) {
        return recipeRepository.findByNameContainingIgnoreCase(name);
    }

    public Recipe save(Recipe recipe) {
        return recipeRepository.save(recipe);
    }

    public Optional<Recipe> update(Long id, Recipe updatedRecipe) {
        return recipeRepository.findById(id).map(existing -> {
            existing.setName(updatedRecipe.getName());
            existing.setDescription(updatedRecipe.getDescription());
            existing.setIngredients(updatedRecipe.getIngredients());
            existing.setInstructions(updatedRecipe.getInstructions());
            existing.setPreparationTimeMinutes(updatedRecipe.getPreparationTimeMinutes());
            return recipeRepository.save(existing);
        });
    }

    public boolean deleteById(Long id) {
        if (recipeRepository.existsById(id)) {
            recipeRepository.deleteById(id);
            return true;
        }
        return false;
    }
}
