package com.cookmate.main.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testy jednostkowe dla Step entity — weryfikacja builder pattern,
 * getterów/setterów, equals/hashCode, toString i obsługi null.
 */
@DisplayName("Step Entity")
class StepTest {

    // --- Builder pattern ---

    @Nested
    @DisplayName("Builder pattern")
    class BuilderTests {

        @Test
        @DisplayName("tworzy Step z wszystkimi polami")
        void builderCreatesStepWithAllFields() {
            LocalDateTime now = LocalDateTime.now();
            Map<String, Object> params = Map.of("temp", 180, "unit", "°C");

            Step step = Step.builder()
                    .id(1L)
                    .stepNumber(1)
                    .description("Pokrój cebulę")
                    .mainIngredient("cebula")
                    .action(ActionType.CUT)
                    .parameters(params)
                    .durationMinutes(10)
                    .recipeId("recipe-123")
                    .createdAt(now)
                    .updatedAt(now)
                    .build();

            assertEquals(1L, step.getId());
            assertEquals(1, step.getStepNumber());
            assertEquals("Pokrój cebulę", step.getDescription());
            assertEquals("cebula", step.getMainIngredient());
            assertEquals(ActionType.CUT, step.getAction());
            assertEquals(params, step.getParameters());
            assertEquals(10, step.getDurationMinutes());
            assertEquals("recipe-123", step.getRecipeId());
            assertEquals(now, step.getCreatedAt());
            assertEquals(now, step.getUpdatedAt());
        }

        @Test
        @DisplayName("tworzy Step z wymaganymi polami (bez optional)")
        void builderCreatesStepWithRequiredFieldsOnly() {
            Step step = Step.builder()
                    .stepNumber(1)
                    .description("Odważ mąkę")
                    .action(ActionType.WEIGH)
                    .recipeId("recipe-456")
                    .build();

            assertNull(step.getId());
            assertEquals(1, step.getStepNumber());
            assertEquals("Odważ mąkę", step.getDescription());
            assertEquals(ActionType.WEIGH, step.getAction());
            assertEquals("recipe-456", step.getRecipeId());
            assertNull(step.getMainIngredient());
            assertNull(step.getParameters());
            assertNull(step.getDurationMinutes());
            assertNull(step.getCreatedAt());
            assertNull(step.getUpdatedAt());
        }
    }

    // --- Gettery i settery ---

    @Nested
    @DisplayName("Gettery i settery")
    class GettersSettersTests {

        @Test
        @DisplayName("settery ustawiają wartości poprawnie")
        void settersWork() {
            Step step = new Step();
            LocalDateTime now = LocalDateTime.now();

            step.setId(42L);
            step.setStepNumber(3);
            step.setDescription("Podsmaż na oleju");
            step.setMainIngredient("olej");
            step.setAction(ActionType.FRYING_PAN);
            step.setParameters(Map.of("oil", "olive"));
            step.setDurationMinutes(15);
            step.setRecipeId("recipe-789");
            step.setCreatedAt(now);
            step.setUpdatedAt(now);

            assertEquals(42L, step.getId());
            assertEquals(3, step.getStepNumber());
            assertEquals("Podsmaż na oleju", step.getDescription());
            assertEquals("olej", step.getMainIngredient());
            assertEquals(ActionType.FRYING_PAN, step.getAction());
            assertEquals(Map.of("oil", "olive"), step.getParameters());
            assertEquals(15, step.getDurationMinutes());
            assertEquals("recipe-789", step.getRecipeId());
            assertEquals(now, step.getCreatedAt());
            assertEquals(now, step.getUpdatedAt());
        }

        @Test
        @DisplayName("settery akceptują null dla opcjonalnych pól")
        void settersAcceptNullForOptionalFields() {
            Step step = Step.builder()
                    .stepNumber(1)
                    .description("Test")
                    .action(ActionType.WAIT)
                    .recipeId("r-1")
                    .durationMinutes(10)
                    .mainIngredient("cebula")
                    .parameters(Map.of("key", "val"))
                    .build();

            step.setDurationMinutes(null);
            step.setMainIngredient(null);
            step.setParameters(null);

            assertNull(step.getDurationMinutes());
            assertNull(step.getMainIngredient());
            assertNull(step.getParameters());
        }
    }

    // --- Null handling ---

    @Nested
    @DisplayName("Null handling")
    class NullHandlingTests {

        @Test
        @DisplayName("no-arg constructor tworzy Step z nullami")
        void noArgConstructorCreatesStepWithNulls() {
            Step step = new Step();

            assertNull(step.getId());
            assertNull(step.getDescription());
            assertNull(step.getMainIngredient());
            assertNull(step.getAction());
            assertNull(step.getParameters());
            assertNull(step.getDurationMinutes());
            assertNull(step.getRecipeId());
            assertNull(step.getCreatedAt());
            assertNull(step.getUpdatedAt());
            // stepNumber to Integer (boxed) — domyślnie null
            assertNull(step.getStepNumber());
        }

        @Test
        @DisplayName("builder akceptuje null wartości")
        void builderAcceptsNullValues() {
            Step step = Step.builder()
                    .id(null)
                    .description(null)
                    .action(null)
                    .parameters(null)
                    .durationMinutes(null)
                    .recipeId(null)
                    .build();

            assertNull(step.getId());
            assertNull(step.getDescription());
            assertNull(step.getAction());
            assertNull(step.getParameters());
            assertNull(step.getDurationMinutes());
            assertNull(step.getRecipeId());
        }
    }

    // --- equals / hashCode ---

    @Nested
    @DisplayName("equals i hashCode")
    class EqualsHashCodeTests {

        @Test
        @DisplayName("dwa identyczne Step są equal")
        void identicalStepsAreEqual() {
            LocalDateTime now = LocalDateTime.now();
            Step step1 = Step.builder().id(1L).stepNumber(1).description("A").action(ActionType.CUT)
                    .recipeId("r-1").createdAt(now).build();
            Step step2 = Step.builder().id(1L).stepNumber(1).description("A").action(ActionType.CUT)
                    .recipeId("r-1").createdAt(now).build();

            assertEquals(step1, step2);
            assertEquals(step1.hashCode(), step2.hashCode());
        }

        @Test
        @DisplayName("Step nie jest equal do null")
        void stepNotEqualToNull() {
            Step step = Step.builder().id(1L).stepNumber(1).description("A")
                    .action(ActionType.CUT).recipeId("r-1").build();

            assertNotEquals(null, step);
        }

        @Test
        @DisplayName("różne Step nie są equal")
        void differentStepsAreNotEqual() {
            Step step1 = Step.builder().id(1L).stepNumber(1).description("A")
                    .action(ActionType.CUT).recipeId("r-1").build();
            Step step2 = Step.builder().id(2L).stepNumber(2).description("B")
                    .action(ActionType.MIX).recipeId("r-2").build();

            assertNotEquals(step1, step2);
        }

        @Test
        @DisplayName("Step jest equal do samego siebie")
        void stepEqualsItself() {
            Step step = Step.builder().id(1L).stepNumber(1).description("A")
                    .action(ActionType.CUT).recipeId("r-1").build();

            assertEquals(step, step);
        }
    }

    // --- toString ---

    @Nested
    @DisplayName("toString")
    class ToStringTests {

        @Test
        @DisplayName("toString generuje content z kluczowymi polami")
        void toStringContainsKeyFields() {
            Step step = Step.builder()
                    .id(5L)
                    .stepNumber(3)
                    .description("Gotuj w garnku")
                    .action(ActionType.POT)
                    .recipeId("recipe-abc")
                    .durationMinutes(20)
                    .build();

            String result = step.toString();

            assertNotNull(result);
            assertFalse(result.isEmpty());
            assertTrue(result.contains("5"), "toString powinien zawierać id");
            assertTrue(result.contains("3"), "toString powinien zawierać stepNumber");
            assertTrue(result.contains("Gotuj w garnku"), "toString powinien zawierać description");
            assertTrue(result.contains("POT"), "toString powinien zawierać action");
            assertTrue(result.contains("recipe-abc"), "toString powinien zawierać recipeId");
        }
    }

    // --- Optional fields ---

    @Nested
    @DisplayName("Optional fields (parameters, durationMinutes, mainIngredient)")
    class OptionalFieldsTests {

        @Test
        @DisplayName("parameters mogą być pustą mapą")
        void parametersCanBeEmptyMap() {
            Step step = Step.builder()
                    .stepNumber(1).description("A").action(ActionType.WAIT)
                    .recipeId("r-1").parameters(Map.of()).build();

            assertNotNull(step.getParameters());
            assertTrue(step.getParameters().isEmpty());
        }

        @Test
        @DisplayName("parameters mogą zawierać złożone wartości")
        void parametersCanContainComplexValues() {
            Map<String, Object> params = Map.of(
                    "temperature", 200,
                    "unit", "°C",
                    "preheated", true);

            Step step = Step.builder()
                    .stepNumber(1).description("Piecz").action(ActionType.BAKE)
                    .recipeId("r-1").parameters(params).build();

            assertEquals(200, step.getParameters().get("temperature"));
            assertEquals("°C", step.getParameters().get("unit"));
            assertEquals(true, step.getParameters().get("preheated"));
        }

        @Test
        @DisplayName("durationMinutes akceptuje zero")
        void durationAcceptsZero() {
            Step step = Step.builder()
                    .stepNumber(1).description("Natychmiastowe").action(ActionType.POUR)
                    .recipeId("r-1").durationMinutes(0).build();

            assertEquals(0, step.getDurationMinutes());
        }

        @Test
        @DisplayName("mainIngredient jest opcjonalny")
        void mainIngredientIsOptional() {
            Step withIngredient = Step.builder()
                    .stepNumber(1).description("A").action(ActionType.CUT)
                    .recipeId("r-1").mainIngredient("pomidor").build();
            Step withoutIngredient = Step.builder()
                    .stepNumber(1).description("A").action(ActionType.CUT)
                    .recipeId("r-1").build();

            assertEquals("pomidor", withIngredient.getMainIngredient());
            assertNull(withoutIngredient.getMainIngredient());
        }
    }

    // --- ActionType ---

    @Nested
    @DisplayName("ActionType enum")
    class ActionTypeTests {

        @Test
        @DisplayName("wszystkie ActionType mają displayName")
        void allActionTypesHaveDisplayName() {
            for (ActionType type : ActionType.values()) {
                assertNotNull(type.getDisplayName(), type.name() + " powinien mieć displayName");
                assertFalse(type.getDisplayName().isBlank(), type.name() + " displayName nie powinien być pusty");
            }
        }

        @Test
        @DisplayName("Step akceptuje każdy ActionType")
        void stepAcceptsAllActionTypes() {
            for (ActionType type : ActionType.values()) {
                Step step = Step.builder()
                        .stepNumber(1).description("test").action(type).recipeId("r-1").build();
                assertEquals(type, step.getAction());
            }
        }
    }
}
