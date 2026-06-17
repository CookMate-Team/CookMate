package com.cookmate.mealplanner.controller;

import com.cookmate.mealplanner.client.MainServiceClient;
import com.cookmate.mealplanner.dto.CategoryResponse;
import com.cookmate.mealplanner.dto.DayPlan;
import com.cookmate.mealplanner.dto.MealItem;
import com.cookmate.mealplanner.dto.MealSearchResponse;
import com.cookmate.mealplanner.dto.SavedShoppingListResponse;
import com.cookmate.mealplanner.dto.SavedWeeklyPlanResponse;
import com.cookmate.mealplanner.dto.ShoppingListItem;
import com.cookmate.mealplanner.service.ShoppingListPersistenceService;
import com.cookmate.mealplanner.service.WeeklyPlanPersistenceService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("MealPlanController — integration tests")
class MealPlanControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private MainServiceClient mainServiceClient;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @MockitoBean
    private WeeklyPlanPersistenceService weeklyPlanPersistenceService;

    @MockitoBean
    private ShoppingListPersistenceService shoppingListPersistenceService;

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

    private SavedWeeklyPlanResponse savedWeeklyPlanResponse() {
        UUID id = UUID.fromString("00000000-0000-0000-0000-000000000001");
        List<MealItem> meals = List.of(new MealItem("1", "Pasta", "https://thumb.jpg"));
        List<DayPlan> days = List.of(
                new DayPlan("Monday", meals),
                new DayPlan("Tuesday", List.of()),
                new DayPlan("Wednesday", List.of()),
                new DayPlan("Thursday", List.of()),
                new DayPlan("Friday", List.of()),
                new DayPlan("Saturday", List.of()),
                new DayPlan("Sunday", List.of())
        );
        return new SavedWeeklyPlanResponse(id, LocalDateTime.of(2026, 1, 10, 12, 0), 1, days);
    }

    private SavedShoppingListResponse savedShoppingListResponse() {
        UUID id = UUID.fromString("00000000-0000-0000-0000-000000000002");
        List<ShoppingListItem> items = List.of(
                new ShoppingListItem("Salt", List.of("1 tsp"), List.of("Pasta"))
        );
        return new SavedShoppingListResponse(id, LocalDateTime.of(2026, 1, 10, 12, 0), items);
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

    // --- POST /api/planner/weekly-plan/save ---

    @Test
    @DisplayName("POST /weekly-plan/save — zapisuje plan i zwraca 200 z id")
    void saveWeeklyPlan_validRequest_returns200WithId() throws Exception {
        when(weeklyPlanPersistenceService.save(anyString(), any())).thenReturn(savedWeeklyPlanResponse());

        List<DayPlan> days = List.of(new DayPlan("Monday", List.of(new MealItem("1", "Pasta", "https://t.jpg"))));
        String body = objectMapper.writeValueAsString(new com.cookmate.mealplanner.dto.WeeklyPlanResponse(days));

        mockMvc.perform(post("/api/planner/weekly-plan/save")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("00000000-0000-0000-0000-000000000001"))
                .andExpect(jsonPath("$.mealsPerDay").value(1))
                .andExpect(jsonPath("$.days", hasSize(7)));
    }

    @Test
    @DisplayName("POST /weekly-plan/save — przekazuje userId z JWT do serwisu")
    void saveWeeklyPlan_passesUserIdFromJwt() throws Exception {
        when(weeklyPlanPersistenceService.save(eq("test-user-id"), any()))
                .thenReturn(savedWeeklyPlanResponse());

        List<DayPlan> days = List.of(new DayPlan("Monday", List.of()));
        String body = objectMapper.writeValueAsString(new com.cookmate.mealplanner.dto.WeeklyPlanResponse(days));

        mockMvc.perform(post("/api/planner/weekly-plan/save")
                        .with(jwt().jwt(j -> j.subject("test-user-id"))
                                .authorities(new SimpleGrantedAuthority("ROLE_USER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /weekly-plan/save — brak JWT zwraca 401")
    void saveWeeklyPlan_noJwt_returns401() throws Exception {
        String body = objectMapper.writeValueAsString(
                new com.cookmate.mealplanner.dto.WeeklyPlanResponse(List.of()));

        mockMvc.perform(post("/api/planner/weekly-plan/save")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized());
    }

    // --- GET /api/planner/weekly-plan/history ---

    @Test
    @DisplayName("GET /weekly-plan/history — zwraca historię planów dla użytkownika")
    void getWeeklyPlanHistory_returns200WithHistory() throws Exception {
        when(weeklyPlanPersistenceService.getHistory(anyString()))
                .thenReturn(List.of(savedWeeklyPlanResponse()));

        mockMvc.perform(get("/api/planner/weekly-plan/history")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value("00000000-0000-0000-0000-000000000001"));
    }

    @Test
    @DisplayName("GET /weekly-plan/history — pusta historia zwraca pustą tablicę")
    void getWeeklyPlanHistory_emptyHistory_returnsEmptyArray() throws Exception {
        when(weeklyPlanPersistenceService.getHistory(anyString())).thenReturn(List.of());

        mockMvc.perform(get("/api/planner/weekly-plan/history")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @DisplayName("GET /weekly-plan/history — brak JWT zwraca 401")
    void getWeeklyPlanHistory_noJwt_returns401() throws Exception {
        mockMvc.perform(get("/api/planner/weekly-plan/history"))
                .andExpect(status().isUnauthorized());
    }

    // --- POST /api/planner/shopping-list/save ---

    @Test
    @DisplayName("POST /shopping-list/save — zapisuje listę i zwraca 200 z id")
    void saveShoppingList_validRequest_returns200WithId() throws Exception {
        when(shoppingListPersistenceService.save(anyString(), any())).thenReturn(savedShoppingListResponse());

        List<ShoppingListItem> items = List.of(
                new ShoppingListItem("Salt", List.of("1 tsp"), List.of("Pasta"))
        );
        String body = objectMapper.writeValueAsString(
                new com.cookmate.mealplanner.dto.ShoppingListResponse(items));

        mockMvc.perform(post("/api/planner/shopping-list/save")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("00000000-0000-0000-0000-000000000002"))
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].name").value("Salt"));
    }

    @Test
    @DisplayName("POST /shopping-list/save — przekazuje userId z JWT do serwisu")
    void saveShoppingList_passesUserIdFromJwt() throws Exception {
        when(shoppingListPersistenceService.save(eq("test-user-id"), any()))
                .thenReturn(savedShoppingListResponse());

        String body = objectMapper.writeValueAsString(
                new com.cookmate.mealplanner.dto.ShoppingListResponse(List.of()));

        mockMvc.perform(post("/api/planner/shopping-list/save")
                        .with(jwt().jwt(j -> j.subject("test-user-id"))
                                .authorities(new SimpleGrantedAuthority("ROLE_USER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /shopping-list/save — brak JWT zwraca 401")
    void saveShoppingList_noJwt_returns401() throws Exception {
        String body = objectMapper.writeValueAsString(
                new com.cookmate.mealplanner.dto.ShoppingListResponse(List.of()));

        mockMvc.perform(post("/api/planner/shopping-list/save")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized());
    }

    // --- GET /api/planner/shopping-list/history ---

    @Test
    @DisplayName("GET /shopping-list/history — zwraca historię list zakupów")
    void getShoppingListHistory_returns200WithHistory() throws Exception {
        when(shoppingListPersistenceService.getHistory(anyString()))
                .thenReturn(List.of(savedShoppingListResponse()));

        mockMvc.perform(get("/api/planner/shopping-list/history")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value("00000000-0000-0000-0000-000000000002"))
                .andExpect(jsonPath("$[0].items[0].name").value("Salt"));
    }

    @Test
    @DisplayName("GET /shopping-list/history — pusta historia zwraca pustą tablicę")
    void getShoppingListHistory_emptyHistory_returnsEmptyArray() throws Exception {
        when(shoppingListPersistenceService.getHistory(anyString())).thenReturn(List.of());

        mockMvc.perform(get("/api/planner/shopping-list/history")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @DisplayName("GET /shopping-list/history — brak JWT zwraca 401")
    void getShoppingListHistory_noJwt_returns401() throws Exception {
        mockMvc.perform(get("/api/planner/shopping-list/history"))
                .andExpect(status().isUnauthorized());
    }
}
