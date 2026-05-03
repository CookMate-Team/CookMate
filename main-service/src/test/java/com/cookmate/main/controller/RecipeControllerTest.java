package com.cookmate.main.controller;

import com.cookmate.main.model.ActionType;
import com.cookmate.main.model.Recipe;
import com.cookmate.main.model.Step;
import com.cookmate.main.repository.RecipeRepository;
import com.cookmate.main.repository.StepRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.cookmate.main.service.MealDbClient;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class RecipeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RecipeRepository recipeRepository;

    @Autowired
    private StepRepository stepRepository;

    @MockitoBean
    private MealDbClient mealDbClient;

    @Test
    void shouldReturnPaginatedRecipesWithMetadata() throws Exception {
        recipeRepository.save(new Recipe("Apple Pie", "A classic dessert", "apples, flour", "Mix and bake", 60));
        recipeRepository.save(new Recipe("Banana Bread", "Moist bread", "bananas, flour", "Mix and bake", 50));

        mockMvc.perform(get("/api/recipes")
                        .param("page", "0")
                        .param("size", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.recipes").isArray())
                .andExpect(jsonPath("$.pageNumber").value(0))
                .andExpect(jsonPath("$.pageSize").value(10))
                .andExpect(jsonPath("$.totalCount").isNumber())
                .andExpect(jsonPath("$.totalPages").isNumber());
    }

    @Test
    void shouldReturnStepsForRecipeSortedAscending() throws Exception {
        String recipeId = "test-recipe-abc";
        stepRepository.save(Step.builder()
                .stepNumber(3).description("Third step").action(ActionType.MIX)
                .recipeId(recipeId).durationMinutes(5).createdAt(LocalDateTime.now()).build());
        stepRepository.save(Step.builder()
                .stepNumber(1).description("First step").action(ActionType.CHOP)
                .recipeId(recipeId).durationMinutes(2).createdAt(LocalDateTime.now()).build());
        stepRepository.save(Step.builder()
                .stepNumber(2).description("Second step").action(ActionType.STIR)
                .recipeId(recipeId).durationMinutes(3).createdAt(LocalDateTime.now()).build());

        mockMvc.perform(get("/api/recipes/{recipeId}/steps", recipeId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].stepNumber").value(1))
                .andExpect(jsonPath("$[1].stepNumber").value(2))
                .andExpect(jsonPath("$[2].stepNumber").value(3));
    }

    @Test
    void shouldReturnEmptyListWhenNoStepsForRecipe() throws Exception {
        mockMvc.perform(get("/api/recipes/{recipeId}/steps", "recipe-with-no-steps")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void shouldReturn400WhenPageIsNegative() throws Exception {
        mockMvc.perform(get("/api/recipes")
                        .param("page", "-1")
                        .param("size", "10"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn400WhenSizeIsZero() throws Exception {
        mockMvc.perform(get("/api/recipes")
                        .param("page", "0")
                        .param("size", "0"))
                .andExpect(status().isBadRequest());
    }
}
