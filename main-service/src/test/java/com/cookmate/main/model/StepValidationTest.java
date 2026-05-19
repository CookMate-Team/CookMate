package com.cookmate.main.model;

import com.cookmate.main.dto.StepDTO;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testy Bean Validation dla StepDTO — weryfikacja @NotBlank, @NotNull,
 * @Positive, @PositiveOrZero na polach DTO.
 */
@DisplayName("StepDTO — Bean Validation")
class StepValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }
    }

    // --- Valid DTO ---

    @Nested
    @DisplayName("Poprawne dane")
    class ValidDataTests {

        @Test
        @DisplayName("poprawny StepDTO nie generuje violation")
        void validStepDTO_noViolations() {
            StepDTO dto = StepDTO.builder()
                    .stepNumber(1)
                    .description("Pokrój cebulę")
                    .action(ActionType.CUT)
                    .recipeId("recipe-123")
                    .durationMinutes(10)
                    .parameters(Map.of("size", "small"))
                    .build();

            Set<ConstraintViolation<StepDTO>> violations = validator.validate(dto);

            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("poprawny StepDTO z minimalnymi polami")
        void validStepDTO_minimalFields() {
            StepDTO dto = StepDTO.builder()
                    .stepNumber(1)
                    .description("Krok")
                    .action(ActionType.WAIT)
                    .recipeId("r-1")
                    .build();

            Set<ConstraintViolation<StepDTO>> violations = validator.validate(dto);

            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("durationMinutes = 0 jest akceptowalne (@PositiveOrZero)")
        void zeroDuration_isValid() {
            StepDTO dto = StepDTO.builder()
                    .stepNumber(1)
                    .description("Natychmiastowy krok")
                    .action(ActionType.POUR)
                    .recipeId("r-1")
                    .durationMinutes(0)
                    .build();

            Set<ConstraintViolation<StepDTO>> violations = validator.validate(dto);

            assertThat(violations).isEmpty();
        }
    }

    // --- stepNumber validation ---

    @Nested
    @DisplayName("stepNumber validation")
    class StepNumberValidation {

        @Test
        @DisplayName("null stepNumber → violation @NotNull")
        void nullStepNumber_violation() {
            StepDTO dto = StepDTO.builder()
                    .stepNumber(null)
                    .description("Krok")
                    .action(ActionType.CUT)
                    .recipeId("r-1")
                    .build();

            Set<ConstraintViolation<StepDTO>> violations = validator.validate(dto);

            assertThat(violations).isNotEmpty();
            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("stepNumber"));
        }

        @Test
        @DisplayName("stepNumber = 0 → violation @Positive")
        void zeroStepNumber_violation() {
            StepDTO dto = StepDTO.builder()
                    .stepNumber(0)
                    .description("Krok")
                    .action(ActionType.CUT)
                    .recipeId("r-1")
                    .build();

            Set<ConstraintViolation<StepDTO>> violations = validator.validate(dto);

            assertThat(violations).isNotEmpty();
            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("stepNumber"));
        }

        @Test
        @DisplayName("stepNumber = -1 → violation @Positive")
        void negativeStepNumber_violation() {
            StepDTO dto = StepDTO.builder()
                    .stepNumber(-1)
                    .description("Krok")
                    .action(ActionType.CUT)
                    .recipeId("r-1")
                    .build();

            Set<ConstraintViolation<StepDTO>> violations = validator.validate(dto);

            assertThat(violations).isNotEmpty();
            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("stepNumber"));
        }
    }

    // --- description validation ---

    @Nested
    @DisplayName("description validation")
    class DescriptionValidation {

        @Test
        @DisplayName("null description → violation @NotBlank")
        void nullDescription_violation() {
            StepDTO dto = StepDTO.builder()
                    .stepNumber(1)
                    .description(null)
                    .action(ActionType.CUT)
                    .recipeId("r-1")
                    .build();

            Set<ConstraintViolation<StepDTO>> violations = validator.validate(dto);

            assertThat(violations).isNotEmpty();
            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("description"));
        }

        @Test
        @DisplayName("pusty description → violation @NotBlank")
        void emptyDescription_violation() {
            StepDTO dto = StepDTO.builder()
                    .stepNumber(1)
                    .description("")
                    .action(ActionType.CUT)
                    .recipeId("r-1")
                    .build();

            Set<ConstraintViolation<StepDTO>> violations = validator.validate(dto);

            assertThat(violations).isNotEmpty();
            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("description"));
        }

        @Test
        @DisplayName("biały description (spacje) → violation @NotBlank")
        void blankDescription_violation() {
            StepDTO dto = StepDTO.builder()
                    .stepNumber(1)
                    .description("   ")
                    .action(ActionType.CUT)
                    .recipeId("r-1")
                    .build();

            Set<ConstraintViolation<StepDTO>> violations = validator.validate(dto);

            assertThat(violations).isNotEmpty();
            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("description"));
        }
    }

    // --- action validation ---

    @Nested
    @DisplayName("action validation")
    class ActionValidation {

        @Test
        @DisplayName("null action → violation @NotNull")
        void nullAction_violation() {
            StepDTO dto = StepDTO.builder()
                    .stepNumber(1)
                    .description("Krok")
                    .action(null)
                    .recipeId("r-1")
                    .build();

            Set<ConstraintViolation<StepDTO>> violations = validator.validate(dto);

            assertThat(violations).isNotEmpty();
            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("action"));
        }
    }

    // --- recipeId validation ---

    @Nested
    @DisplayName("recipeId validation")
    class RecipeIdValidation {

        @Test
        @DisplayName("null recipeId → violation @NotBlank")
        void nullRecipeId_violation() {
            StepDTO dto = StepDTO.builder()
                    .stepNumber(1)
                    .description("Krok")
                    .action(ActionType.CUT)
                    .recipeId(null)
                    .build();

            Set<ConstraintViolation<StepDTO>> violations = validator.validate(dto);

            assertThat(violations).isNotEmpty();
            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("recipeId"));
        }

        @Test
        @DisplayName("pusty recipeId → violation @NotBlank")
        void emptyRecipeId_violation() {
            StepDTO dto = StepDTO.builder()
                    .stepNumber(1)
                    .description("Krok")
                    .action(ActionType.CUT)
                    .recipeId("")
                    .build();

            Set<ConstraintViolation<StepDTO>> violations = validator.validate(dto);

            assertThat(violations).isNotEmpty();
            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("recipeId"));
        }
    }

    // --- durationMinutes validation ---

    @Nested
    @DisplayName("durationMinutes validation")
    class DurationValidation {

        @Test
        @DisplayName("null durationMinutes jest akceptowalne (opcjonalne)")
        void nullDuration_noViolation() {
            StepDTO dto = StepDTO.builder()
                    .stepNumber(1)
                    .description("Krok")
                    .action(ActionType.WAIT)
                    .recipeId("r-1")
                    .durationMinutes(null)
                    .build();

            Set<ConstraintViolation<StepDTO>> violations = validator.validate(dto);

            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("durationMinutes = -1 → violation @PositiveOrZero")
        void negativeDuration_violation() {
            StepDTO dto = StepDTO.builder()
                    .stepNumber(1)
                    .description("Krok")
                    .action(ActionType.WAIT)
                    .recipeId("r-1")
                    .durationMinutes(-1)
                    .build();

            Set<ConstraintViolation<StepDTO>> violations = validator.validate(dto);

            assertThat(violations).isNotEmpty();
            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("durationMinutes"));
        }
    }

    // --- multiple violations ---

    @Nested
    @DisplayName("Multiple violations")
    class MultipleViolations {

        @Test
        @DisplayName("wiele niepoprawnych pól → wiele violations")
        void multipleInvalidFields_multipleViolations() {
            StepDTO dto = StepDTO.builder()
                    .stepNumber(null)
                    .description(null)
                    .action(null)
                    .recipeId(null)
                    .durationMinutes(-5)
                    .build();

            Set<ConstraintViolation<StepDTO>> violations = validator.validate(dto);

            // stepNumber(NotNull), description(NotBlank), action(NotNull),
            // recipeId(NotBlank), durationMinutes(PositiveOrZero)
            assertThat(violations).hasSizeGreaterThanOrEqualTo(5);
        }
    }
}
