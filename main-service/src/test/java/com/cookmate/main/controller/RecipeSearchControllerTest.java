package com.cookmate.main.controller;

import com.cookmate.main.dto.MealSearchResponse;
import com.cookmate.main.service.RecipeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import reactor.core.publisher.Mono;

import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class RecipeSearchControllerTest {

    @Mock
    private RecipeService recipeService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        RecipeSearchController controller = new RecipeSearchController(recipeService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void searchByLetter_shouldReturn200AndResponseBody() throws Exception {
        when(recipeService.searchMealsByLetter("a"))
            .thenReturn(Mono.just(new MealSearchResponse(List.of())));

        MvcResult mvcResult = mockMvc.perform(get("/api/recipes/search/themealdb/letter")
                .param("letter", "a"))
            .andExpect(request().asyncStarted())
            .andReturn();

        mockMvc.perform(asyncDispatch(mvcResult))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.meals").isArray());

        verify(recipeService).searchMealsByLetter("a");
    }

    @Test
    void lookupMeal_shouldReturn200AndResponseBody() throws Exception {
        when(recipeService.lookupMeal("52772"))
            .thenReturn(Mono.just(new MealSearchResponse(List.of())));

        MvcResult mvcResult = mockMvc.perform(get("/api/recipes/search/themealdb/meal")
                .param("mealId", "52772"))
            .andExpect(request().asyncStarted())
            .andReturn();

        mockMvc.perform(asyncDispatch(mvcResult))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.meals").isArray());

        verify(recipeService).lookupMeal("52772");
    }

    @Test
    void searchByLetter_withoutLetterParam_shouldReturn400() throws Exception {
        mockMvc.perform(get("/api/recipes/search/themealdb/letter"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void lookupMeal_withoutMealIdParam_shouldReturn400() throws Exception {
        mockMvc.perform(get("/api/recipes/search/themealdb/meal"))
            .andExpect(status().isBadRequest());
    }
}

