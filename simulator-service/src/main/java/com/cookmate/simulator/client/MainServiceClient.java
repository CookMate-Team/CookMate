package com.cookmate.simulator.client;

import com.cookmate.simulator.dto.MainServiceStepDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@FeignClient(name = "main-service")
public interface MainServiceClient {

    @GetMapping("/api/recipes/{recipeId}/steps")
    List<MainServiceStepDto> getRecipeSteps(@PathVariable("recipeId") String recipeId);
}
