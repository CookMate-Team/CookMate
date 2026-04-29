package com.cookmate.main.controller;

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
        // Tworzymy Meal z 53 argumentami (użyj metody pomocniczej z poprzednich odpowiedzi)
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

    private Meal createTestMeal(String id, String name) {
        Object[] args = new Object[53];
        return new Meal(id, name, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null,
                null, null, null);
    }
}