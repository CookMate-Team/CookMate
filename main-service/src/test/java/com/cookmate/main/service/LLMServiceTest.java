package com.cookmate.main.service;

import com.cookmate.main.dto.LLMResponseDTO;
import com.cookmate.main.exception.ExternalServiceException;
import com.cookmate.main.model.ActionType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import java.util.function.Function;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for {@link GroqClient} (LLM API service).
 *
 * <p>Because {@link GroqClient} uses Spring's synchronous {@link RestClient} internally,
 * we exercise it by:
 * <ol>
 *   <li>Constructing a {@link GroqClient} with a real {@link ObjectMapper} so that
 *       JSON parsing logic is tested end-to-end.</li>
 *   <li>Using a custom {@link RestClient} builder configured with a mock exchange
 *       function – no real network calls are made.</li>
 * </ol>
 * </p>
 *
 * <p>The {@code generateSteps} method wraps the synchronous {@link RestClient} call
 * inside {@code Mono.fromCallable()} and applies a retry policy. Tests call
 * {@code .block()} to drive the reactive pipeline synchronously.</p>
 */
class LLMServiceTest {

    private static final String DUMMY_API_KEY = "test-api-key";

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
    }

    // -----------------------------------------------------------------------
    // Helper – build a GroqClient that returns a fixed JSON body from RestClient
    // -----------------------------------------------------------------------

    /**
     * Creates a {@link GroqClient} whose underlying {@link RestClient} always responds
     * with the given JSON {@code body} and the given HTTP {@code status}.
     */
    private GroqClient clientReturning(HttpStatus status, String body) {
        // Use a RestClient.Builder with a custom exchange function (mock adapter)
        RestClient.Builder builder = RestClient.builder()
                .requestInterceptor((request, requestBody, execution) -> {
                    // Return a minimal mock response
                    return new org.springframework.http.client.ClientHttpResponse() {
                        @Override
                        public org.springframework.http.HttpStatusCode getStatusCode() {
                            return status;
                        }

                        @Override
                        public String getStatusText() {
                            return status.getReasonPhrase();
                        }

                        @Override
                        public void close() {}

                        @Override
                        public java.io.InputStream getBody() {
                            return new java.io.ByteArrayInputStream(body.getBytes());
                        }

                        @Override
                        public org.springframework.http.HttpHeaders getHeaders() {
                            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
                            headers.setContentType(MediaType.APPLICATION_JSON);
                            return headers;
                        }
                    };
                });

        return new GroqClient(builder, objectMapper, DUMMY_API_KEY);
    }

    // -----------------------------------------------------------------------
    // Test: generateStepsFromRecipe – success
    // -----------------------------------------------------------------------

    @Test
    void generateSteps_shouldReturnParsedLlmResponseOnSuccess() {
        GroqClient client = clientReturning(HttpStatus.OK, groqSuccessResponseJson());

        LLMResponseDTO result = client.generateSteps("Some recipe instructions").block();

        assertThat(result).isNotNull();
        assertThat(result.steps()).hasSize(2);

        var step1 = result.steps().get(0);
        assertThat(step1.stepNumber()).isEqualTo(1);
        assertThat(step1.description()).isEqualTo("Chop all vegetables finely.");
        assertThat(step1.action()).isEqualTo(ActionType.CHOP);
        assertThat(step1.mainIngredient()).isEqualTo("vegetables");
        assertThat(step1.duration()).isEqualTo(5);
        assertThat(step1.parameters()).containsEntry("temperature", 0);
        assertThat(step1.parameters()).containsEntry("speed", 5);

        var step2 = result.steps().get(1);
        assertThat(step2.stepNumber()).isEqualTo(2);
        assertThat(step2.action()).isEqualTo(ActionType.FRYING_PAN);
        assertThat(step2.parameters()).containsEntry("temperature", 180);
    }

    // -----------------------------------------------------------------------
    // Test: LLM response structure validation
    // -----------------------------------------------------------------------

    @Test
    void generateSteps_shouldVerifyStepNumberIsSequential() {
        GroqClient client = clientReturning(HttpStatus.OK, groqSuccessResponseJson());

        LLMResponseDTO result = client.generateSteps("Instructions").block();

        assertThat(result).isNotNull();
        for (int i = 0; i < result.steps().size(); i++) {
            assertThat(result.steps().get(i).stepNumber()).isEqualTo(i + 1);
        }
    }

    @Test
    void generateSteps_shouldVerifyAllRequiredFieldsArePresent() {
        GroqClient client = clientReturning(HttpStatus.OK, groqSuccessResponseJson());

        LLMResponseDTO result = client.generateSteps("Instructions").block();

        assertThat(result).isNotNull();
        result.steps().forEach(step -> {
            assertThat(step.stepNumber()).isPositive();
            assertThat(step.description()).isNotBlank();
            assertThat(step.action()).isNotNull();
            assertThat(step.parameters()).isNotNull();
        });
    }

    @Test
    void generateSteps_shouldVerifyActionTypeIsKnownEnum() {
        GroqClient client = clientReturning(HttpStatus.OK, groqSuccessResponseJson());

        LLMResponseDTO result = client.generateSteps("Instructions").block();

        assertThat(result).isNotNull();
        result.steps().forEach(step ->
                assertThat(step.action()).isIn((Object[]) ActionType.values())
        );
    }

    // -----------------------------------------------------------------------
    // Test: API error – 4xx/5xx responses
    // -----------------------------------------------------------------------

    @Test
    void generateSteps_shouldThrowExternalServiceExceptionOnHttpError() {
        // When RestClient gets a 401 it throws HttpClientErrorException which
        // the retry policy exhausts and the onErrorMap converts to ExternalServiceException.
        GroqClient client = clientReturning(HttpStatus.UNAUTHORIZED,
                "{\"error\":{\"message\":\"Invalid API key\"}}");

        assertThatThrownBy(() -> client.generateSteps("Instructions").block())
                .isInstanceOf(ExternalServiceException.class)
                .hasMessageContaining("Groq");
    }

    @Test
    void generateSteps_shouldThrowExternalServiceExceptionOnServerError() {
        GroqClient client = clientReturning(HttpStatus.SERVICE_UNAVAILABLE,
                "{\"error\":{\"message\":\"Service overloaded\"}}");

        assertThatThrownBy(() -> client.generateSteps("Instructions").block())
                .isInstanceOf(ExternalServiceException.class)
                .hasMessageContaining("Groq");
    }

    // -----------------------------------------------------------------------
    // Test: empty steps list – validation rejects response
    // -----------------------------------------------------------------------

    @Test
    void generateSteps_shouldThrowExternalServiceExceptionWhenStepsListIsEmpty() {
        GroqClient client = clientReturning(HttpStatus.OK, groqEmptyStepsResponseJson());

        // The retry policy will exhaust attempts and then onErrorMap kicks in
        assertThatThrownBy(() -> client.generateSteps("Instructions").block())
                .isInstanceOf(ExternalServiceException.class)
                .hasMessageContaining("Groq");
    }

    // -----------------------------------------------------------------------
    // Test: malformed JSON from LLM
    // -----------------------------------------------------------------------

    @Test
    void generateSteps_shouldThrowExternalServiceExceptionOnMalformedJson() {
        GroqClient client = clientReturning(HttpStatus.OK,
                buildGroqResponseWithContent("NOT_VALID_JSON{{{"));

        assertThatThrownBy(() -> client.generateSteps("Instructions").block())
                .isInstanceOf(ExternalServiceException.class)
                .hasMessageContaining("Groq");
    }

    // -----------------------------------------------------------------------
    // Test: step with null action – validation rejects response
    // -----------------------------------------------------------------------

    @Test
    void generateSteps_shouldThrowExternalServiceExceptionWhenStepHasNullAction() {
        String nullActionJson = buildGroqResponseWithContent("""
                {
                  "steps": [
                    {
                      "step_number": 1,
                      "description": "Some step",
                      "action": null,
                      "main_ingredient": "chicken",
                      "duration_minutes": 5,
                      "parameters": {"temperature": 100, "speed": 2}
                    }
                  ]
                }
                """);

        GroqClient client = clientReturning(HttpStatus.OK, nullActionJson);

        assertThatThrownBy(() -> client.generateSteps("Instructions").block())
                .isInstanceOf(ExternalServiceException.class)
                .hasMessageContaining("Groq");
    }

    // -----------------------------------------------------------------------
    // Test: step with blank description – validation rejects response
    // -----------------------------------------------------------------------

    @Test
    void generateSteps_shouldThrowExternalServiceExceptionWhenStepHasBlankDescription() {
        String blankDescJson = buildGroqResponseWithContent("""
                {
                  "steps": [
                    {
                      "step_number": 1,
                      "description": "   ",
                      "action": "CHOP",
                      "main_ingredient": "onion",
                      "duration_minutes": 3,
                      "parameters": {"temperature": 0, "speed": 5}
                    }
                  ]
                }
                """);

        GroqClient client = clientReturning(HttpStatus.OK, blankDescJson);

        assertThatThrownBy(() -> client.generateSteps("Instructions").block())
                .isInstanceOf(ExternalServiceException.class)
                .hasMessageContaining("Groq");
    }

    // -----------------------------------------------------------------------
    // JSON fixtures
    // -----------------------------------------------------------------------

    /**
     * Wraps the given {@code innerJson} in a Groq chat completion envelope.
     */
    private String buildGroqResponseWithContent(String innerJson) {
        // Escape the inner JSON string for embedding inside another JSON string
        String escaped = innerJson
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
        return """
                {
                  "choices": [
                    {
                      "message": {
                        "role": "assistant",
                        "content": "%s"
                      }
                    }
                  ]
                }
                """.formatted(escaped);
    }

    private String groqSuccessResponseJson() {
        String steps = """
                {
                  "steps": [
                    {
                      "step_number": 1,
                      "description": "Chop all vegetables finely.",
                      "action": "CHOP",
                      "main_ingredient": "vegetables",
                      "duration_minutes": 5,
                      "parameters": {"temperature": 0, "speed": 5}
                    },
                    {
                      "step_number": 2,
                      "description": "Fry the meat at 180 degrees until golden.",
                      "action": "FRYING_PAN",
                      "main_ingredient": "meat",
                      "duration_minutes": 15,
                      "parameters": {"temperature": 180, "speed": 3}
                    }
                  ]
                }
                """;
        return buildGroqResponseWithContent(steps);
    }

    private String groqEmptyStepsResponseJson() {
        return buildGroqResponseWithContent("{\"steps\": []}");
    }
}
