package com.cookmate.mealplanner.controller;

import com.cookmate.mealplanner.client.MainServiceClient;
import com.cookmate.mealplanner.dto.CategoryResponse;
import com.cookmate.mealplanner.dto.MealSearchResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("MealPlanController — integration tests")
class MealPlanControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MainServiceClient mainServiceClient;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    // --- helpers ---

    private void mockSuccessfulPlan() {
        CategoryResponse categories = new CategoryResponse(List.of(
                new CategoryResponse.Category("1", "Beef"),
                new CategoryResponse.Category("2", "Chicken"),
                new CategoryResponse.Category("3", "Dessert"),
                new CategoryResponse.Category("4", "Lamb"),
                new CategoryResponse.Category("5", "Miscellaneous"),
                new CategoryResponse.Category("6", "Pasta"),
                new CategoryResponse.Category("7", "Pork")
        ));
        List<MealSearchResponse.FilteredMeal> meals = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            meals.add(new MealSearchResponse.FilteredMeal(
                    String.valueOf(i), "Meal " + i, "https://thumb" + i + ".jpg"
            ));
        }
        when(mainServiceClient.getCategories()).thenReturn(categories);
        when(mainServiceClient.getMealsByCategory(anyString())).thenReturn(new MealSearchResponse(meals));
    }

    // --- GET /api/planner/weekly-plan ---

    @Test
    @DisplayName("GET /weekly-plan — poprawny request zwraca 200 z 7 dniami")
    void getWeeklyPlan_validRequest_returns200WithSevenDays() throws Exception {
        mockSuccessfulPlan();

        mockMvc.perform(get("/api/planner/weekly-plan")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER")))
                        .param("mealsPerDay", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.days", hasSize(7)))
                .andExpect(jsonPath("$.days[0].day").value("Monday"))
                .andExpect(jsonPath("$.days[6].day").value("Sunday"))
                .andExpect(jsonPath("$.days[0].meals", hasSize(2)));
    }

    @Test
    @DisplayName("GET /weekly-plan — mealsPerDay=1 zwraca 200")
    void getWeeklyPlan_mealsPerDayOne_returns200() throws Exception {
        mockSuccessfulPlan();

        mockMvc.perform(get("/api/planner/weekly-plan")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER")))
                        .param("mealsPerDay", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.days", hasSize(7)));
    }

    @Test
    @DisplayName("GET /weekly-plan — mealsPerDay=5 zwraca 200")
    void getWeeklyPlan_mealsPerDayFive_returns200() throws Exception {
        mockSuccessfulPlan();

        mockMvc.perform(get("/api/planner/weekly-plan")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER")))
                        .param("mealsPerDay", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.days[0].meals", hasSize(5)));
    }

    @Test
    @DisplayName("GET /weekly-plan — mealsPerDay=0 zwraca 400")
    void getWeeklyPlan_mealsPerDayZero_returns400() throws Exception {
        mockMvc.perform(get("/api/planner/weekly-plan")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER")))
                        .param("mealsPerDay", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("GET /weekly-plan — mealsPerDay=6 zwraca 400")
    void getWeeklyPlan_mealsPerDaySix_returns400() throws Exception {
        mockMvc.perform(get("/api/planner/weekly-plan")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER")))
                        .param("mealsPerDay", "6"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("GET /weekly-plan — brak parametru zwraca 400")
    void getWeeklyPlan_missingParam_returns400() throws Exception {
        mockMvc.perform(get("/api/planner/weekly-plan")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /weekly-plan — brak tokenu JWT zwraca 401")
    void getWeeklyPlan_noJwt_returns401() throws Exception {
        mockMvc.perform(get("/api/planner/weekly-plan")
                        .param("mealsPerDay", "2"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /weekly-plan — main-service niedostępny zwraca 503")
    void getWeeklyPlan_mainServiceDown_returns503() throws Exception {
        when(mainServiceClient.getCategories()).thenThrow(new RuntimeException("Connection refused"));

        mockMvc.perform(get("/api/planner/weekly-plan")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER")))
                        .param("mealsPerDay", "2"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("MAIN_SERVICE_UNAVAILABLE"));
    }
}
