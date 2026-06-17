package com.cookmate.mealplanner.service;

import com.cookmate.mealplanner.dto.DayPlan;
import com.cookmate.mealplanner.dto.MealItem;
import com.cookmate.mealplanner.dto.SavedWeeklyPlanResponse;
import com.cookmate.mealplanner.dto.WeeklyPlanResponse;
import com.cookmate.mealplanner.model.WeeklyPlan;
import com.cookmate.mealplanner.model.WeeklyPlanMeal;
import com.cookmate.mealplanner.repository.WeeklyPlanRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("WeeklyPlanPersistenceService — unit tests")
class WeeklyPlanPersistenceServiceTest {

    @Mock
    private WeeklyPlanRepository weeklyPlanRepository;

    private WeeklyPlanPersistenceService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new WeeklyPlanPersistenceService(weeklyPlanRepository);
        when(weeklyPlanRepository.save(any(WeeklyPlan.class))).thenAnswer(invocation -> {
            WeeklyPlan plan = invocation.getArgument(0);
            plan.setId(UUID.fromString("00000000-0000-0000-0000-000000000001"));
            return plan;
        });
    }

    // --- helpers ---

    private WeeklyPlanResponse twoMealPlan() {
        List<MealItem> meals = List.of(
                new MealItem("1", "Pasta", "https://thumb1.jpg"),
                new MealItem("2", "Soup", "https://thumb2.jpg")
        );
        List<DayPlan> days = List.of(
                new DayPlan("Monday", meals),
                new DayPlan("Tuesday", meals),
                new DayPlan("Wednesday", meals),
                new DayPlan("Thursday", meals),
                new DayPlan("Friday", meals),
                new DayPlan("Saturday", meals),
                new DayPlan("Sunday", meals)
        );
        return new WeeklyPlanResponse(days);
    }

    private WeeklyPlan savedPlan(UUID id, String userId, int mealsPerDay) {
        WeeklyPlan plan = new WeeklyPlan();
        plan.setId(id);
        plan.setUserId(userId);
        plan.setMealsPerDay(mealsPerDay);
        plan.setCreatedAt(LocalDateTime.of(2026, 1, 10, 12, 0));

        WeeklyPlanMeal meal = new WeeklyPlanMeal();
        meal.setWeeklyPlan(plan);
        meal.setDayName("Monday");
        meal.setMealId("1");
        meal.setMealName("Pasta");
        meal.setThumbnailUrl("https://thumb1.jpg");
        plan.setMeals(List.of(meal));
        return plan;
    }

    // --- save ---

    @Test
    @DisplayName("save — wywołuje repository.save z odpowiednimi danymi")
    void save_callsRepository() {
        service.save("user-1", twoMealPlan());
        verify(weeklyPlanRepository).save(any(WeeklyPlan.class));
    }

    @Test
    @DisplayName("save — zwraca odpowiedź z wygenerowanym id")
    void save_returnsResponseWithId() {
        SavedWeeklyPlanResponse response = service.save("user-1", twoMealPlan());
        assertThat(response.id()).isNotNull();
    }

    @Test
    @DisplayName("save — mealsPerDay jest obliczany z największej liczby posiłków w dniu")
    void save_computesMealsPerDay() {
        SavedWeeklyPlanResponse response = service.save("user-1", twoMealPlan());
        assertThat(response.mealsPerDay()).isEqualTo(2);
    }

    @Test
    @DisplayName("save — plan z pustymi dniami zapisuje mealsPerDay=0")
    void save_emptyDays_mealsPerDayIsZero() {
        WeeklyPlanResponse empty = new WeeklyPlanResponse(List.of(
                new DayPlan("Monday", List.of())
        ));
        SavedWeeklyPlanResponse response = service.save("user-1", empty);
        assertThat(response.mealsPerDay()).isEqualTo(0);
    }

    @Test
    @DisplayName("save — zwrócona odpowiedź zawiera 7 dni w kolejności")
    void save_responseContainsSevenDaysInOrder() {
        SavedWeeklyPlanResponse response = service.save("user-1", twoMealPlan());
        assertThat(response.days()).extracting("day")
                .containsExactly("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday");
    }

    @Test
    @DisplayName("save — posiłki są prawidłowo mapowane na dni")
    void save_mealsAreMappedToCorrectDays() {
        SavedWeeklyPlanResponse response = service.save("user-1", twoMealPlan());
        assertThat(response.days().get(0).meals()).hasSize(2);
        assertThat(response.days().get(0).meals().get(0).name()).isEqualTo("Pasta");
    }

    // --- getHistory ---

    @Test
    @DisplayName("getHistory — zwraca zmapowane plany dla użytkownika")
    void getHistory_returnsMappedPlansForUser() {
        UUID id = UUID.randomUUID();
        when(weeklyPlanRepository.findByUserIdOrderByCreatedAtDesc("user-1"))
                .thenReturn(List.of(savedPlan(id, "user-1", 1)));

        List<SavedWeeklyPlanResponse> history = service.getHistory("user-1");

        assertThat(history).hasSize(1);
        assertThat(history.get(0).id()).isEqualTo(id);
        assertThat(history.get(0).mealsPerDay()).isEqualTo(1);
    }

    @Test
    @DisplayName("getHistory — pusta historia zwraca pustą listę")
    void getHistory_emptyHistory_returnsEmptyList() {
        when(weeklyPlanRepository.findByUserIdOrderByCreatedAtDesc("user-1"))
                .thenReturn(List.of());

        List<SavedWeeklyPlanResponse> history = service.getHistory("user-1");

        assertThat(history).isEmpty();
    }

    @Test
    @DisplayName("getHistory — posiłki są prawidłowo rekonstruowane z encji")
    void getHistory_mealsAreReconstructedFromEntities() {
        when(weeklyPlanRepository.findByUserIdOrderByCreatedAtDesc("user-1"))
                .thenReturn(List.of(savedPlan(UUID.randomUUID(), "user-1", 1)));

        List<SavedWeeklyPlanResponse> history = service.getHistory("user-1");

        DayPlan monday = history.get(0).days().stream()
                .filter(d -> "Monday".equals(d.day()))
                .findFirst()
                .orElseThrow();
        assertThat(monday.meals()).hasSize(1);
        assertThat(monday.meals().get(0).name()).isEqualTo("Pasta");
    }

    @Test
    @DisplayName("getHistory — dni są zawsze zwracane w kolejności poniedziałek–niedziela")
    void getHistory_daysAlwaysInWeekOrder() {
        when(weeklyPlanRepository.findByUserIdOrderByCreatedAtDesc("user-1"))
                .thenReturn(List.of(savedPlan(UUID.randomUUID(), "user-1", 1)));

        List<SavedWeeklyPlanResponse> history = service.getHistory("user-1");

        assertThat(history.get(0).days()).extracting("day")
                .containsExactly("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday");
    }
}
