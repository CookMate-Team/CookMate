package com.cookmate.main.repository;

import com.cookmate.main.model.ActionType;
import com.cookmate.main.model.Step;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
@DisplayName("StepRepository")
class StepRepositoryTest {

    @Autowired
    private StepRepository stepRepository;

    // --- save i findById ---

    @Test
    @DisplayName("save — zapisuje Step i generuje ID")
    void save_persistsStepAndGeneratesId() {
        // given
        Step step = Step.builder()
                .recipeId("recipe-save-1")
                .stepNumber(1)
                .description("Odważ składniki")
                .action(ActionType.WEIGH)
                .durationMinutes(5)
                .mainIngredient("mąka")
                .parameters(Map.of("weight", "500g"))
                .createdAt(LocalDateTime.now())
                .build();

        // when
        Step saved = stepRepository.save(step);

        // then
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getDescription()).isEqualTo("Odważ składniki");
        assertThat(saved.getAction()).isEqualTo(ActionType.WEIGH);
        assertThat(saved.getDurationMinutes()).isEqualTo(5);
        assertThat(saved.getMainIngredient()).isEqualTo("mąka");
    }

    @Test
    @DisplayName("findById — zwraca zapisany Step")
    void findById_returnsSavedStep() {
        // given
        Step saved = stepRepository.save(Step.builder()
                .recipeId("recipe-find-1")
                .stepNumber(1)
                .description("Pokrój warzywa")
                .action(ActionType.CUT)
                .createdAt(LocalDateTime.now())
                .build());

        // when
        Optional<Step> found = stepRepository.findById(saved.getId());

        // then
        assertThat(found).isPresent();
        assertThat(found.get().getDescription()).isEqualTo("Pokrój warzywa");
        assertThat(found.get().getRecipeId()).isEqualTo("recipe-find-1");
    }

    @Test
    @DisplayName("findById — zwraca empty dla nieistniejącego ID")
    void findById_returnsEmptyForNonExistentId() {
        Optional<Step> result = stepRepository.findById(99999L);
        assertThat(result).isEmpty();
    }

    // --- findByRecipeIdOrderByStepNumberAsc ---

    @Test
    @DisplayName("findByRecipeIdOrderByStepNumberAsc — sortuje po stepNumber")
    void shouldFindStepsByRecipeIdSortedByStepNumber() {
        // given
        String recipeId = "recipe-abc-123";

        Step step2 = Step.builder()
                .recipeId(recipeId)
                .stepNumber(2)
                .description("Mieszaj energicznie")
                .action(ActionType.MIX)
                .createdAt(LocalDateTime.now())
                .build();

        Step step1 = Step.builder()
                .recipeId(recipeId)
                .stepNumber(1)
                .description("Odważ mąkę")
                .action(ActionType.WEIGH)
                .createdAt(LocalDateTime.now())
                .build();

        stepRepository.save(step2);
        stepRepository.save(step1);

        // when
        List<Step> results = stepRepository.findByRecipeIdOrderByStepNumberAsc(recipeId);

        // then
        assertThat(results).hasSize(2);
        assertThat(results.get(0).getStepNumber()).isEqualTo(1);
        assertThat(results.get(1).getStepNumber()).isEqualTo(2);
        assertThat(results.get(0).getDescription()).isEqualTo("Odważ mąkę");
    }

    @Test
    @DisplayName("findByRecipeIdOrderByStepNumberAsc — nie miesza przepisów")
    void findByRecipeId_doesNotMixRecipes() {
        // given
        stepRepository.save(Step.builder().recipeId("r-A").stepNumber(1)
                .description("Krok A").action(ActionType.CUT).createdAt(LocalDateTime.now()).build());
        stepRepository.save(Step.builder().recipeId("r-B").stepNumber(1)
                .description("Krok B").action(ActionType.MIX).createdAt(LocalDateTime.now()).build());

        // when
        List<Step> resultsA = stepRepository.findByRecipeIdOrderByStepNumberAsc("r-A");
        List<Step> resultsB = stepRepository.findByRecipeIdOrderByStepNumberAsc("r-B");

        // then
        assertThat(resultsA).hasSize(1);
        assertThat(resultsA.get(0).getDescription()).isEqualTo("Krok A");
        assertThat(resultsB).hasSize(1);
        assertThat(resultsB.get(0).getDescription()).isEqualTo("Krok B");
    }

    // --- findByIdAndRecipeId ---

    @Test
    @DisplayName("findByIdAndRecipeId — zwraca krok gdy ID i recipeId pasują")
    void shouldFindByIdAndRecipeId() {
        // given
        String recipeId = "recipe-xyz";
        Step savedStep = stepRepository.save(Step.builder()
                .recipeId(recipeId)
                .stepNumber(1)
                .description("Czekaj")
                .action(ActionType.WAIT)
                .createdAt(LocalDateTime.now())
                .build());

        // when
        var result = stepRepository.findByIdAndRecipeId(savedStep.getId(), recipeId);

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getAction()).isEqualTo(ActionType.WAIT);
    }

    @Test
    @DisplayName("findByIdAndRecipeId — zwraca empty gdy recipeId nie pasuje")
    void findByIdAndRecipeId_returnsEmptyWhenRecipeIdMismatch() {
        // given
        Step saved = stepRepository.save(Step.builder()
                .recipeId("recipe-correct")
                .stepNumber(1)
                .description("Krok")
                .action(ActionType.STIR)
                .createdAt(LocalDateTime.now())
                .build());

        // when
        var result = stepRepository.findByIdAndRecipeId(saved.getId(), "recipe-wrong");

        // then
        assertThat(result).isEmpty();
    }

    // --- delete ---

    @Test
    @DisplayName("delete — usuwa Step z bazy")
    void delete_removesStep() {
        // given
        Step saved = stepRepository.save(Step.builder()
                .recipeId("recipe-del")
                .stepNumber(1)
                .description("Do usunięcia")
                .action(ActionType.WAIT)
                .createdAt(LocalDateTime.now())
                .build());
        Long id = saved.getId();

        // when
        stepRepository.delete(saved);
        stepRepository.flush();

        // then
        assertThat(stepRepository.findById(id)).isEmpty();
    }

    @Test
    @DisplayName("deleteById — usuwa Step po ID")
    void deleteById_removesStep() {
        // given
        Step saved = stepRepository.save(Step.builder()
                .recipeId("recipe-del2")
                .stepNumber(1)
                .description("Delete by ID")
                .action(ActionType.POUR)
                .createdAt(LocalDateTime.now())
                .build());
        Long id = saved.getId();

        // when
        stepRepository.deleteById(id);
        stepRepository.flush();

        // then
        assertThat(stepRepository.findById(id)).isEmpty();
    }

    // --- empty results ---

    @Test
    @DisplayName("findByRecipeId — zwraca pustą listę dla nieistniejącego przepisu")
    void findByRecipeId_returnsEmptyForNonExistentRecipe() {
        List<Step> results = stepRepository.findByRecipeIdOrderByStepNumberAsc("non-existent-recipe");
        assertThat(results).isEmpty();
    }

    @Test
    @DisplayName("findByIdAndRecipeId — zwraca empty dla nieistniejącego ID")
    void findByIdAndRecipeId_returnsEmptyForNonExistentId() {
        var result = stepRepository.findByIdAndRecipeId(99999L, "any-recipe");
        assertThat(result).isEmpty();
    }

    // --- parameters (JSON conversion) ---

    @Test
    @DisplayName("save/load — poprawnie serializuje i deserializuje parameters (JSON)")
    void parametersJsonConversion() {
        // given
        Map<String, Object> params = Map.of("temperature", 200, "unit", "°C", "preheated", true);
        Step saved = stepRepository.save(Step.builder()
                .recipeId("recipe-json")
                .stepNumber(1)
                .description("Piecz")
                .action(ActionType.BAKE)
                .parameters(params)
                .createdAt(LocalDateTime.now())
                .build());

        // when
        Step loaded = stepRepository.findById(saved.getId()).orElseThrow();

        // then
        assertThat(loaded.getParameters()).isNotNull();
        assertThat(loaded.getParameters().get("temperature")).isEqualTo(200);
        assertThat(loaded.getParameters().get("unit")).isEqualTo("°C");
        assertThat(loaded.getParameters().get("preheated")).isEqualTo(true);
    }
}