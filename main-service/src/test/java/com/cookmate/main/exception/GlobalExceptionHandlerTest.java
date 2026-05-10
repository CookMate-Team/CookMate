package com.cookmate.main.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
            .standaloneSetup(new TestErrorController())
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
    }

    @Test
    void shouldReturnUnifiedContractForUnhandledException() throws Exception {
        mockMvc.perform(get("/test-errors/runtime"))
            .andExpect(status().isInternalServerError())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.status").value(500))
            .andExpect(jsonPath("$.error").value("Internal Server Error"))
            .andExpect(jsonPath("$.code").value("INTERNAL_SERVER_ERROR"))
            .andExpect(jsonPath("$.message").value("An unexpected error occurred"))
            .andExpect(jsonPath("$.path").value("/test-errors/runtime"))
            .andExpect(jsonPath("$.traceId").isNotEmpty())
            .andExpect(jsonPath("$.details").isArray())
            .andExpect(jsonPath("$.details.length()").value(0));
    }

    @Test
    void shouldPreserveIncomingTraceIdHeader() throws Exception {
        mockMvc.perform(
                get("/test-errors/runtime")
                    .header("X-Trace-Id", "trace-id-from-client")
            )
            .andExpect(status().isInternalServerError())
            .andExpect(header().string("X-Trace-Id", "trace-id-from-client"))
            .andExpect(jsonPath("$.traceId").value("trace-id-from-client"));
    }

    @RestController
    static class TestErrorController {

        @GetMapping("/test-errors/runtime")
        String runtimeError() {
            throw new RuntimeException("boom");
        }
    }
}
