package com.cookmate.simulator.client;

import com.cookmate.simulator.dto.RecipeDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@FeignClient(name = "main-service")
public interface MainServiceClient {

    @GetMapping("/api/recipes")
    List<RecipeDto> getAllRecipes();

    @GetMapping("/api/recipes/{id}")
    RecipeDto getRecipeById(@PathVariable("id") Long id);
}
