package com.cookmate.main.controller;

import com.cookmate.main.dto.CategoryResponse;
import com.cookmate.main.dto.CommonListResponse;
import com.cookmate.main.dto.Meal;
import com.cookmate.main.dto.MealSearchResponse;
import com.cookmate.main.service.MealDbClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import reactor.core.publisher.Mono;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.mockito.Mockito.when;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DiscoveryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean // Nowy standard Spring 4
    private MealDbClient mealDbClient;

    @Test
    void shouldReturnMealsWhenSearchingByName() throws Exception {
        // Meal record has 53 parameters; use helper to keep test declarations concise
        Meal meal = createTestMeal("52771", "Arrabiata");
        MealSearchResponse response = new MealSearchResponse(List.of(meal));

        when(mealDbClient.searchByName("Arrabiata")).thenReturn(Mono.just(response));

        // Obsługa asynchronicznego Mono w MockMvc
        MvcResult mvcResult = mockMvc.perform(get("/api/v1/discovery/search")
                        .param("name", "Arrabiata"))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.meals[0].strMeal").value("Arrabiata"));
    }

    @Test
    void shouldReturnCategoriesFromDiscoveryEndpoint() throws Exception {
        CategoryResponse.CategoryDTO category = new CategoryResponse.CategoryDTO("1", "Chicken", null, "Chicken dishes");
        CategoryResponse categoryResponse = new CategoryResponse(List.of(category));

        when(mealDbClient.listFullCategories()).thenReturn(Mono.just(categoryResponse));

        MvcResult mvcResult = mockMvc.perform(get("/api/v1/discovery/categories"))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.categories[0].strCategory").value("Chicken"));
    }

    @Test
    void shouldReturnListByTypeFromDiscoveryEndpoint() throws Exception {
        CommonListResponse.Item item = new CommonListResponse.Item("Chicken", null, null);
        CommonListResponse listResponse = new CommonListResponse(List.of(item));

        when(mealDbClient.listAllBy("c")).thenReturn(Mono.just(listResponse));

        MvcResult mvcResult = mockMvc.perform(get("/api/v1/discovery/list")
                        .param("type", "c"))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.meals[0].strCategory").value("Chicken"));
    }

    private Meal createTestMeal(String id, String name) {
        return new Meal(id, name, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null,
                null, null, null);
    }
}