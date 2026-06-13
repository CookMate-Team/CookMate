package com.cookmate.main.controller;

import com.cookmate.main.dto.FavoriteRecipeAddRequest;
import com.cookmate.main.dto.FavoriteRecipeDTO;
import com.cookmate.main.service.FavoriteRecipeService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/recipes")
@Validated
public class FavoriteRecipeController {

    private final FavoriteRecipeService favoriteRecipeService;

    public FavoriteRecipeController(FavoriteRecipeService favoriteRecipeService) {
        this.favoriteRecipeService = favoriteRecipeService;
    }

    @GetMapping("/favorites")
    @PreAuthorize("hasRole('ROLE_USER')")
    public ResponseEntity<Page<FavoriteRecipeDTO>> getFavorites(
            JwtAuthenticationToken jwt,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        String userId = jwt.getName();
        return ResponseEntity.ok(favoriteRecipeService.getUserFavorites(userId, page, size));
    }

    @PostMapping("/{recipeId}/favorite")
    @PreAuthorize("hasRole('ROLE_USER')")
    public ResponseEntity<FavoriteRecipeDTO> addFavorite(
            JwtAuthenticationToken jwt,
            @PathVariable String recipeId,
            @Valid @RequestBody FavoriteRecipeAddRequest request) {
        
        String userId = jwt.getName();
        FavoriteRecipeDTO favorite = favoriteRecipeService.addFavorite(userId, recipeId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(favorite);
    }

    @GetMapping("/{recipeId}/favorite/check")
    @PreAuthorize("hasRole('ROLE_USER')")
    public ResponseEntity<Boolean> checkFavorite(
            JwtAuthenticationToken jwt,
            @PathVariable String recipeId) {
        
        String userId = jwt.getName();
        boolean isFavorite = favoriteRecipeService.isFavorite(userId, recipeId);
        return ResponseEntity.ok(isFavorite);
    }

    @DeleteMapping("/{recipeId}/favorite")
    @PreAuthorize("hasRole('ROLE_USER')")
    public ResponseEntity<Void> removeFavorite(
            JwtAuthenticationToken jwt,
            @PathVariable String recipeId) {
        
        String userId = jwt.getName();
        favoriteRecipeService.removeFavorite(userId, recipeId);
        return ResponseEntity.noContent().build();
    }
}
