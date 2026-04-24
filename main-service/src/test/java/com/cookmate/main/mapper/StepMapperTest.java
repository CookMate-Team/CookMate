package com.cookmate.main.mapper;

import com.cookmate.main.dto.StepDTO;
import com.cookmate.main.model.ActionType;
import com.cookmate.main.model.Step;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class StepMapperTest {

    @Autowired
    private StepMapper stepMapper;

    @Test
    void shouldMapStepToDTO() {
        // given
        LocalDateTime now = LocalDateTime.now();
        Step entity = Step.builder()
                .id(1L)
                .stepNumber(1)
                .description("Krojenie cebuli")
                .action(ActionType.CHOP)
                .recipeId("recipe-123")
                .duration(60)
                .parameters("{\"size\": \"small\"}")
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
        assertEquals(entity.getDuration(), dto.duration());
        assertEquals(entity.getParameters(), dto.parameters());
        assertEquals(entity.getCreatedAt(), dto.createdAt());
    }

    @Test
    void shouldMapDTOToEntity() {
        // given
        StepDTO dto = StepDTO.builder()
                .stepNumber(2)
                .description("Smażenie na patelni")
                .action(ActionType.FRYING_PAN)
                .recipeId("recipe-456")
                .duration(300)
                .parameters("{\"temp\": 180}")
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
        assertEquals(dto.duration(), entity.getDuration());
        assertEquals(dto.parameters(), entity.getParameters());

        assertNull(entity.getId());
        assertNull(entity.getCreatedAt());
    }

    @Test
    void shouldReturnNullWhenSourceIsNull() {
        // when
        StepDTO dto = stepMapper.toDTO(null);
        Step entity = stepMapper.toEntity(null);

        // then
        assertNull(dto);
        assertNull(entity);
    }
}