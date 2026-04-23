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
        assertEquals(entity.getId(), dto.getId());
        assertEquals(entity.getStepNumber(), dto.getStepNumber());
        assertEquals(entity.getDescription(), dto.getDescription());
        assertEquals(entity.getAction(), dto.getAction());
        assertEquals(entity.getRecipeId(), dto.getRecipeId());
        assertEquals(entity.getDuration(), dto.getDuration());
        assertEquals(entity.getParameters(), dto.getParameters());
        assertEquals(entity.getCreatedAt(), dto.getCreatedAt());
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
        assertEquals(dto.getStepNumber(), entity.getStepNumber());
        assertEquals(dto.getDescription(), entity.getDescription());
        assertEquals(dto.getAction(), entity.getAction());
        assertEquals(dto.getRecipeId(), entity.getRecipeId());
        assertEquals(dto.getDuration(), entity.getDuration());
        assertEquals(dto.getParameters(), entity.getParameters());

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