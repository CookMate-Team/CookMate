package com.cookmate.main.repository;

import com.cookmate.main.model.ActionType;
import com.cookmate.main.model.Step;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
class StepRepositoryTest {

    @Autowired
    private StepRepository stepRepository;

    @Test
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
        List<Step> results = stepRepository.findByRecipeIdOrderByStepNumber(recipeId);

        // then
        assertThat(results).hasSize(2);
        assertThat(results.get(0).getStepNumber()).isEqualTo(1);
        assertThat(results.get(1).getStepNumber()).isEqualTo(2);
        assertThat(results.get(0).getDescription()).isEqualTo("Odważ mąkę");
    }

    @Test
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
}