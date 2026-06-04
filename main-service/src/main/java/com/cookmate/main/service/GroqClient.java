package com.cookmate.main.service;

import com.cookmate.main.dto.LLMResponseDTO;
import com.cookmate.main.exception.ExternalServiceException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.List;

@Service
public class GroqClient {

    private static final Logger logger = LoggerFactory.getLogger(GroqClient.class);
    private static final String GROQ_API_URL = "https://api.groq.com/openai/v1/chat/completions";
    //    Zmien na model GPT-OSS20B albo 120B
    private static final String MODEL = "llama-3.1-8b-instant";
    private static final double TEMPERATURE = 0.7;

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;

    public GroqClient(RestClient.Builder restClientBuilder, ObjectMapper objectMapper,
                      @Value("${groq.api-key:}") String apiKey) {
        this.restClient = restClientBuilder.build();
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
    }

    public Mono<LLMResponseDTO> generateSteps(String recipeInstructions, String ingredients) {
        logger.info("Wysyłanie żądania do Groq LLM...");

        return Mono.fromCallable(() -> {
            String prompt = buildPrompt(recipeInstructions, ingredients);
            GroqChatRequest request = new GroqChatRequest(
                    MODEL,
                    List.of(new GroqMessage("user", prompt)),
                    TEMPERATURE,
                    new JsonResponseFormat("json_object")
            );

            GroqChatResponse response = restClient.post()
                    .uri(GROQ_API_URL)
                    .header("Authorization", "Bearer " + apiKey)
                    .body(request)
                    .retrieve()
                    .body(GroqChatResponse.class);

            return parseAndValidateResponse(response);
        })
        .retryWhen(Retry.backoff(5, Duration.ofSeconds(1))
            .doBeforeRetry(retrySignal -> logger.warn(
                    "Próba {} ponowienia wywołania Groq API z powodu błędu: {}",
                    retrySignal.totalRetries() + 1,
                    retrySignal.failure().getMessage()
            ))
        )
        .doOnError(error -> logger.error("Błąd krytyczny komunikacji z Groq po wyczerpaniu limitu prób: {}", error.getMessage()))
        .onErrorMap(error -> error instanceof ExternalServiceException ? error : new ExternalServiceException("Groq", error));
    }

    private String buildPrompt(String recipeInstructions, String ingredients) {
        return """
                You are an expert culinary assistant for the CookMate system.\s
                CookMate is a multi-functional cooking device (like a Thermomix) that combines a pot, frying pan, blender, and steamer into one machine.

                YOUR PRIMARY TASK:\s
                Transform the provided recipe into a "Guided Cooking" experience for ONE device.\s
                Even if the original recipe requires multiple pots, pans, or an oven, ADAPT it to be done primarily within the CookMate device.\s
                Use heating, stirring, and blending functions to simulate traditional cooking methods.

                CRITICAL INSTRUCTIONS:
                1. CONSOLIDATE & ADAPT (MAX 8-10 STEPS): Combine actions. For example, if a recipe says "fry chicken in a pan and boil pasta in a pot", transform it into: "First, fry the chicken in the device, remove it, then boil the pasta in the same bowl using the heating function."
                2. GROUP SIMILAR ACTIONS: Whenever possible, group identical tasks together. If multiple vegetables need preparation (e.g., onions, garlic, carrots), combine them into a single CHOP or CUT step. If multiple ingredients need to be added to the device, group them into a single pouring/adding step UNLESS the recipe strictly dictates a specific time delay between them.
                3. DETAILED DESCRIPTIONS: Write long, instructional descriptions. Explain how to set up the device for that specific stage.
                4. DOMINANT ACTIONS ONLY: Use ONLY ONE from: CUT, CHOP, STIR, MARINATE, BLEND, FRYING_PAN, POT, BAKE, WEIGH, GRILL, WAIT, POUR, MIX.
                5. PARAMETERS: Always include "temperature" and "speed" (0-10) in the parameters object to guide the device simulator.

                Each step MUST include:
                - "step_number": sequential integer.
                - "description": detailed instructions for the device user.
                - "main_ingredient": string or null.
                - "action": ONE of the allowed action types.
                - "parameters": JSON object e.g., {"temperature": 120, "speed": 3}.
                - "duration_minutes": integer.

                EXAMPLE OF ONE-DEVICE ADAPTATION WITH GROUPED ACTIONS:
                {
                  "steps": [
                    {
                      "step_number": 1,
                      "description": "Place the onions, garlic, and carrots into the CookMate bowl. Set the device to CHOP mode at speed 5 to finely dice all the vegetables together for 10 seconds. Then, pour in the oil and select FRYING_PAN mode at 120°C to sauté the vegetable mix until softened.",
                      "main_ingredient": "vegetables",
                      "action": "CHOP",
                      "parameters": {"temperature": 120, "speed": 5},
                      "duration_minutes": 5
                    },
                    {
                      "step_number": 2,
                      "description": "Attach the mixing butterfly. Add the chopped meat, spices, and tomato paste all at once to the sautéed vegetables. Set the temperature to 140°C and use STIR mode to brown the ingredients evenly.",
                      "main_ingredient": "meat",
                      "action": "STIR",
                      "parameters": {"temperature": 140, "speed": 2},
                      "duration_minutes": 10
                    },
                    {
                      "step_number": 3,
                      "description": "Pour the water, broth, and pasta directly into the bowl. Set the temperature to 100°C and let the device simmer in POT mode until the pasta absorbs the liquid and becomes tender.",
                      "main_ingredient": "pasta",
                      "action": "POT",
                      "parameters": {"temperature": 100, "speed": 1},
                      "duration_minutes": 12
                    }
                  ]
                }

                Ingredients list:
                %s

                Recipe to adapt for CookMate:
                %s
               """.formatted(ingredients, recipeInstructions);
    }

    private LLMResponseDTO parseAndValidateResponse(GroqChatResponse response) {
        try {
            String content = response.choices().get(0).message().content().trim();
            logger.debug("Odpowiedź z Groq otrzymana, parsowanie JSON... Rozmiar odpowiedzi: {} znaków", content.length());

            LLMResponseDTO parsed = objectMapper.readValue(content, LLMResponseDTO.class);
            if (parsed.steps() == null || parsed.steps().isEmpty()) {
                throw new IllegalStateException("LLM zwrócił pustą listę kroków.");
            }

            for (var step : parsed.steps()) {
                if (step.action() == null) {
                    throw new IllegalStateException("Krok nr " + step.stepNumber() + " nie zawiera obowiązkowego pola action (null).");
                }
                if (step.description() == null || step.description().isBlank()) {
                    throw new IllegalStateException("Krok nr " + step.stepNumber() + " nie zawiera opisu.");
                }
            }
            logger.info("Pomyślnie sparsowano {} kroków", parsed.steps().size());
            return parsed;
        } catch (Exception e) {
            logger.warn("Walidacja/Parsowanie LLM oblało test: {}. Zgłaszam do ponowienia...", e.getMessage());
            throw new RuntimeException("Błąd walidacji danych z LLM", e);
        }
    }

    // DTO records for Groq API communication
    public record GroqChatRequest(
            String model,
            List<GroqMessage> messages,
            double temperature,
            JsonResponseFormat response_format
    ) {}

    public record JsonResponseFormat(
            String type
    ) {}

    public record GroqMessage(
            String role,
            String content
    ) {}

    public record GroqChatResponse(
            List<GroqChoice> choices
    ) {}

    public record GroqChoice(
            GroqChatMessage message
    ) {}

    public record GroqChatMessage(
            String role,
            String content
    ) {}
}
