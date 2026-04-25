package com.cookmate.main.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DiscoveryControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldReturnMealsWhenSearchingByName() throws Exception {
        mockMvc.perform(get("/api/v1/discovery/search")
                        .param("name", "Arrabiata")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                // Sprawdzamy czy struktura Recordu jest poprawna w JSON
                .andExpect(jsonPath("$.meals", notNullValue()))
                .andExpect(jsonPath("$.meals[0].strMeal", containsStringIgnoringCase("Arrabiata")));
    }

    @Test
    void shouldReturnCategories() throws Exception {
        mockMvc.perform(get("/api/v1/discovery/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categories", hasSize(greaterThan(0))))
                .andExpect(jsonPath("$.categories[0].strCategory", notNullValue()));
    }

    @Test
    void shouldReturnAreasList() throws Exception {
        mockMvc.perform(get("/api/v1/discovery/list")
                        .param("type", "a"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.meals", hasSize(greaterThan(0))))
                // Sprawdzamy czy na liście jest np. kuchnia włoska lub polska
                .andExpect(jsonPath("$.meals[*].strArea", hasItem("Italian")));
    }
}