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
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.cookmate.main.service.MealDbClient;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

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

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    void shouldReturnPaginatedRecipesWithMetadata() throws Exception {
        recipeRepository.save(new Recipe("Apple Pie", "A classic dessert", "apples, flour", "Mix and bake", 60));
        recipeRepository.save(new Recipe("Banana Bread", "Moist bread", "bananas, flour", "Mix and bake", 50));

        mockMvc.perform(get("/api/recipes")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER")))
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
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER")))
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
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER")))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void shouldReturn400WhenPageIsNegative() throws Exception {
        mockMvc.perform(get("/api/recipes")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER")))
                        .param("page", "-1")
                        .param("size", "10"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("REQUEST_VALIDATION_FAILED"))
                .andExpect(jsonPath("$.traceId").isNotEmpty());
    }

    @Test
    void shouldReturn400WhenSizeIsZero() throws Exception {
        mockMvc.perform(get("/api/recipes")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER")))
                        .param("page", "0")
                        .param("size", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("REQUEST_VALIDATION_FAILED"))
                .andExpect(jsonPath("$.traceId").isNotEmpty());
    }

    @Test
    void shouldReturn404ContractForUnknownRecipe() throws Exception {
        mockMvc.perform(get("/api/recipes/{id}", 999999L)
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.code").value("RECIPE_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Recipe with id 999999 not found"))
                .andExpect(jsonPath("$.path").value("/api/recipes/999999"))
                .andExpect(jsonPath("$.traceId").isNotEmpty())
                .andExpect(jsonPath("$.details").isArray());
    }

    @Test
    void shouldReturn404ContractForUnknownRecipeOnUpdate() throws Exception {
        String requestBody = """
                {
                  "name": "Updated Name",
                  "description": "Updated description",
                  "ingredients": "salt, pepper",
                  "instructions": "Mix everything",
                  "preparationTimeMinutes": 10
                }
                """;

        mockMvc.perform(put("/api/recipes/{id}", 999999L)
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value("RECIPE_NOT_FOUND"))
                .andExpect(jsonPath("$.path").value("/api/recipes/999999"))
                .andExpect(jsonPath("$.traceId").isNotEmpty());
    }

    @Test
    void shouldReturn404ContractForUnknownRecipeOnDelete() throws Exception {
        mockMvc.perform(delete("/api/recipes/{id}", 999999L)
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value("RECIPE_NOT_FOUND"))
                .andExpect(jsonPath("$.path").value("/api/recipes/999999"))
                .andExpect(jsonPath("$.traceId").isNotEmpty());
    }

    @Test
    void shouldReturn400ForMalformedRequestBody() throws Exception {
        mockMvc.perform(post("/api/recipes")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Soup\","))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value("REQUEST_BODY_INVALID"))
                .andExpect(jsonPath("$.path").value("/api/recipes"))
                .andExpect(jsonPath("$.traceId").isNotEmpty());
    }

    @Test
    void shouldReturn405WhenMethodIsNotSupported() throws Exception {
        mockMvc.perform(patch("/api/recipes")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER")))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value("METHOD_NOT_ALLOWED"))
                .andExpect(jsonPath("$.path").value("/api/recipes"))
                .andExpect(jsonPath("$.traceId").isNotEmpty());
    }
}
