package com.cookmate.main.mapper;

import com.cookmate.main.dto.StepDTO;
import com.cookmate.main.model.ActionType;
import com.cookmate.main.model.Step;
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
class StepMapperTest {

    @Autowired
    private StepMapper stepMapper;

    @Test
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