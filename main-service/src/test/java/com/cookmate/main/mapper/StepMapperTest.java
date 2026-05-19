package com.cookmate.main.mapper;

import com.cookmate.main.dto.StepDTO;
import com.cookmate.main.model.ActionType;
import com.cookmate.main.model.Step;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("StepMapper")
class StepMapperTest {

    @Autowired
    private StepMapper stepMapper;

    // --- toDTO ---

    @Test
    @DisplayName("toDTO — mapuje wszystkie pola entity → DTO")
    void shouldMapStepToDTO() {
        // given
        LocalDateTime now = LocalDateTime.now();
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("size", "small");

        Step entity = Step.builder()
                .id(1L)
                .stepNumber(1)
                .description("Krojenie cebuli")
                .action(ActionType.CHOP)
                .recipeId("recipe-123")
                .durationMinutes(60)
                .parameters(parameters)
                .createdAt(now)
                .build();

        // when
        StepDTO dto = stepMapper.toDTO(entity);

        // then
        assertNotNull(dto);
        // Zmiana: używamy nazw pól bezpośrednio (składnia rekordu)
        assertEquals(entity.getId(), dto.id());
        assertEquals(entity.getStepNumber(), dto.stepNumber());
        assertEquals(entity.getDescription(), dto.description());
        assertEquals(entity.getAction(), dto.action());
        assertEquals(entity.getRecipeId(), dto.recipeId());
        assertEquals(entity.getDurationMinutes(), dto.durationMinutes());
        assertEquals(entity.getParameters(), dto.parameters());
        assertEquals(entity.getCreatedAt(), dto.createdAt());
    }

    @Test
    @DisplayName("toDTO — obsługuje opcjonalne pola jako null")
    void toDTO_handlesNullOptionalFields() {
        // given
        Step entity = Step.builder()
                .id(2L)
                .stepNumber(1)
                .description("Prosty krok")
                .action(ActionType.WAIT)
                .recipeId("recipe-simple")
                .parameters(null)
                .durationMinutes(null)
                .createdAt(LocalDateTime.now())
                .build();

        // when
        StepDTO dto = stepMapper.toDTO(entity);

        // then
        assertNotNull(dto);
        assertNull(dto.parameters());
        assertNull(dto.durationMinutes());
    }

    // --- toEntity ---

    @Test
    @DisplayName("toEntity — mapuje DTO → entity (bez createdAt)")
    void shouldMapDTOToEntity() {
        // given
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("temp", 180);

        StepDTO dto = StepDTO.builder()
                .stepNumber(2)
                .description("Smażenie na patelni")
                .action(ActionType.FRYING_PAN)
                .recipeId("recipe-456")
                .durationMinutes(300)
                .parameters(parameters)
                .build();

        // when
        Step entity = stepMapper.toEntity(dto);

        // then
        assertNotNull(entity);
        // Przy mapowaniu DTO (Record) -> Entity (Lombok) nadal używamy get... dla encji
        assertEquals(dto.stepNumber(), entity.getStepNumber());
        assertEquals(dto.description(), entity.getDescription());
        assertEquals(dto.action(), entity.getAction());
        assertEquals(dto.recipeId(), entity.getRecipeId());
        assertEquals(dto.durationMinutes(), entity.getDurationMinutes());
        assertEquals(dto.parameters(), entity.getParameters());

        assertNull(entity.getId());
        assertNull(entity.getCreatedAt());
    }

    // --- null handling ---

    @Test
    @DisplayName("toDTO/toEntity — null input zwraca null")
    void shouldReturnNullWhenSourceIsNull() {
        // when
        StepDTO dto = stepMapper.toDTO(null);
        Step entity = stepMapper.toEntity(null);

        // then
        assertNull(dto);
        assertNull(entity);
    }

    // --- bidirectional mapping ---

    @Test
    @DisplayName("bidirectional — entity → DTO → entity zachowuje wartości")
    void bidirectionalMapping_entityToDtoToEntity() {
        // given
        Map<String, Object> params = new HashMap<>();
        params.put("temperature", 200);
        params.put("unit", "°C");

        Step original = Step.builder()
                .id(10L)
                .stepNumber(3)
                .description("Piecz ciasto")
                .action(ActionType.BAKE)
                .recipeId("recipe-bake")
                .durationMinutes(45)
                .parameters(params)
                .createdAt(LocalDateTime.of(2026, 5, 19, 12, 0))
                .build();

        // when
        StepDTO dto = stepMapper.toDTO(original);
        Step reconstructed = stepMapper.toEntity(dto);

        // then — pola mapowane powinny być identyczne
        assertEquals(original.getStepNumber(), reconstructed.getStepNumber());
        assertEquals(original.getDescription(), reconstructed.getDescription());
        assertEquals(original.getAction(), reconstructed.getAction());
        assertEquals(original.getRecipeId(), reconstructed.getRecipeId());
        assertEquals(original.getDurationMinutes(), reconstructed.getDurationMinutes());
        assertEquals(original.getParameters(), reconstructed.getParameters());

        // createdAt jest ignorowane przy toEntity (@Mapping target="createdAt",
        // ignore=true)
        assertNull(reconstructed.getCreatedAt());
        // id nie jest kopiowane przy toEntity (brak mappingu)
        assertEquals(original.getId(), dto.id()); // DTO ma id
    }

    @Test
    @DisplayName("bidirectional — DTO → entity → DTO zachowuje wartości")
    void bidirectionalMapping_dtoToEntityToDto() {
        // given
        StepDTO original = StepDTO.builder()
                .stepNumber(5)
                .description("Grilluj mięso")
                .action(ActionType.GRILL)
                .recipeId("recipe-grill")
                .durationMinutes(30)
                .parameters(Map.of("heat", "high"))
                .build();

        // when
        Step entity = stepMapper.toEntity(original);
        StepDTO reconstructed = stepMapper.toDTO(entity);

        // then
        assertEquals(original.stepNumber(), reconstructed.stepNumber());
        assertEquals(original.description(), reconstructed.description());
        assertEquals(original.action(), reconstructed.action());
        assertEquals(original.recipeId(), reconstructed.recipeId());
        assertEquals(original.durationMinutes(), reconstructed.durationMinutes());
        assertEquals(original.parameters(), reconstructed.parameters());
    }

    // --- all ActionTypes ---

    @Test
    @DisplayName("toDTO — poprawnie mapuje każdy ActionType")
    void toDTO_mapsAllActionTypes() {
        for (ActionType action : ActionType.values()) {
            Step entity = Step.builder()
                    .stepNumber(1).description("test").action(action).recipeId("r-1").build();

            StepDTO dto = stepMapper.toDTO(entity);

            assertNotNull(dto);
            assertEquals(action, dto.action(), "ActionType " + action + " powinien być poprawnie mapowany");
        }
    }
}