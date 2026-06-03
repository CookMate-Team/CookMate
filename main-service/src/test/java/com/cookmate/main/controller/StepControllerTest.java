package com.cookmate.main.controller;

import com.cookmate.main.model.ActionType;
import com.cookmate.main.model.Step;
import com.cookmate.main.repository.StepRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class StepControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private StepRepository stepRepository;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    void shouldReturnStepDtoForExistingStep() throws Exception {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("speed", "medium");
        
        Step savedStep = stepRepository.save(Step.builder()
            .stepNumber(3)
            .description("Mieszaj przez 2 minuty")
            .action(ActionType.MIX)
            .parameters(parameters)
            .durationMinutes(120)
            .recipeId("recipe-001")
            .createdAt(LocalDateTime.now())
            .build());

        mockMvc.perform(get("/api/steps/{stepId}", savedStep.getId())
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER")))
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.id").value(savedStep.getId()))
            .andExpect(jsonPath("$.stepNumber").value(3))
            .andExpect(jsonPath("$.description").value("Mieszaj przez 2 minuty"))
            .andExpect(jsonPath("$.action").value("MIX"))
            .andExpect(jsonPath("$.parameters").exists())
            .andExpect(jsonPath("$.parameters.speed").value("medium"))
            .andExpect(jsonPath("$.durationMinutes").value(120))
            .andExpect(jsonPath("$.recipeId").value("recipe-001"))
            .andExpect(jsonPath("$.createdAt").exists());
    }

    @Test
    void shouldReturn404ForUnknownStep() throws Exception {
        mockMvc.perform(get("/api/steps/{stepId}", 999999L)
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"))))
            .andExpect(status().isNotFound())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.timestamp").exists())
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.error").value("Not Found"))
            .andExpect(jsonPath("$.code").value("STEP_NOT_FOUND"))
            .andExpect(jsonPath("$.message").value("Step with id 999999 not found"))
            .andExpect(jsonPath("$.path").value("/api/steps/999999"))
            .andExpect(jsonPath("$.traceId").isNotEmpty())
            .andExpect(jsonPath("$.details").isArray())
            .andExpect(jsonPath("$.details.length()").value(0));
    }
}
