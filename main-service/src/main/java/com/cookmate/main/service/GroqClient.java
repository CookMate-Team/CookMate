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
import reactor.core.scheduler.Schedulers;
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
        logger.info("Rozpoczęcie 3-etapowego generowania kroków (Normalizer -> Planner -> Serializer)...");

        return callNormalizer(recipeInstructions, ingredients)
                .flatMap(this::callPlanner)
                .flatMap(plannedSteps -> callSerializer(plannedSteps, ingredients))
                .retryWhen(Retry.backoff(5, Duration.ofSeconds(1))
                        .doBeforeRetry(retrySignal -> logger.warn(
                                "Próba {} ponowienia całego potoku Groq z powodu błędu: {}",
                                retrySignal.totalRetries() + 1,
                                retrySignal.failure().getMessage()
                        ))
                )
                .doOnError(error -> logger.error("Błąd krytyczny komunikacji z Groq po wyczerpaniu limitu prób: {}", error.getMessage()))
                .onErrorMap(error -> error instanceof ExternalServiceException ? error : new ExternalServiceException("Groq", error));
    }

    private Mono<String> callNormalizer(String recipeInstructions, String ingredients) {
        return Mono.fromCallable(() -> {
            logger.info("[Etap 1/3] Uruchamianie Normalizatora...");
            String prompt = buildNormalizerPrompt(recipeInstructions, ingredients);
            GroqChatRequest request = new GroqChatRequest(
                    MODEL,
                    List.of(new GroqMessage("user", prompt)),
                    TEMPERATURE,
                    null
            );

            GroqChatResponse response = restClient.post()
                    .uri(GROQ_API_URL)
                    .header("Authorization", "Bearer " + apiKey)
                    .body(request)
                    .retrieve()
                    .body(GroqChatResponse.class);

            if (response == null || response.choices() == null || response.choices().isEmpty()) {
                throw new IllegalStateException("Otrzymano pustą odpowiedź z Normalizatora.");
            }

            String content = response.choices().get(0).message().content();
            logger.debug("[Etap 1/3] Normalizator zakończył działanie.");
            return content;
        }).subscribeOn(Schedulers.boundedElastic());
    }

    private Mono<String> callPlanner(String normalizedSteps) {
        return Mono.fromCallable(() -> {
            logger.info("[Etap 2/3] Uruchamianie Plannera...");
            String prompt = buildPlannerPrompt(normalizedSteps);
            GroqChatRequest request = new GroqChatRequest(
                    MODEL,
                    List.of(new GroqMessage("user", prompt)),
                    TEMPERATURE,
                    null
            );

            GroqChatResponse response = restClient.post()
                    .uri(GROQ_API_URL)
                    .header("Authorization", "Bearer " + apiKey)
                    .body(request)
                    .retrieve()
                    .body(GroqChatResponse.class);

            if (response == null || response.choices() == null || response.choices().isEmpty()) {
                throw new IllegalStateException("Otrzymano pustą odpowiedź z Plannera.");
            }

            String content = response.choices().get(0).message().content();
            logger.debug("[Etap 2/3] Planner zakończył działanie.");
            return content;
        }).subscribeOn(Schedulers.boundedElastic());
    }

    private Mono<LLMResponseDTO> callSerializer(String plannedSteps, String ingredients) {
        return Mono.fromCallable(() -> {
            logger.info("[Etap 3/3] Uruchamianie Serializatora...");
            String prompt = buildSerializerPrompt(plannedSteps, ingredients);
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
        }).subscribeOn(Schedulers.boundedElastic());
    }

    private String buildNormalizerPrompt(String recipeInstructions, String ingredients) {
        return """
                You are a professional culinary normalizer. Your task is to analyze the provided recipe instructions and ingredients list, then extract and standardize all raw kitchen activities sequentially.

                CRITICAL INSTRUCTIONS:
                - Do not adapt anything to any cooking device yet.
                - List all actions in a simple, standardized, chronological order.
                - Be concise but do not omit any step from the original recipe.
                - Include names of ingredients in the actions.
                - Identify every ingredient addition, preparation (chopping, cutting, blending), heating, boiling, frying, baking, waiting, and mixing.

                EXAMPLE:
                ---
                Ingredients list:
                - spaghetti (400g)
                - olive oil (2 tbsp)
                - onion (1)
                - garlic (2 cloves)
                - minced beef (500g)
                - canned tomatoes (400g)
                - salt (1 tsp)

                Recipe instructions:
                Finely chop the onion and garlic. Heat olive oil in a large skillet over medium-high heat. Add onion and garlic, cooking until soft. Add minced beef, breaking it up with a spoon, and cook until browned. Pour in canned tomatoes and simmer for 15 minutes. Meanwhile, boil water in a large pot with salt. Add spaghetti and cook until al dente. Drain the pasta and serve with the meat sauce mixed in.
                
                Normalized steps:
                1. Chop 1 onion and 2 cloves of garlic.
                2. Heat 2 tbsp of olive oil in a skillet.
                3. Sauté chopped onion and garlic in the skillet.
                4. Add 500g of minced beef to the skillet and cook until browned.
                5. Add 400g of canned tomatoes to the skillet.
                6. Simmer the tomato and meat mixture for 15 minutes.
                7. Boil water in a separate large pot with 1 tsp of salt.
                8. Add 400g of spaghetti to the boiling water.
                9. Cook spaghetti until al dente.
                10. Drain the cooked spaghetti.
                11. Combine the cooked spaghetti with the simmered meat sauce.
                ---

                Now, normalize the following recipe:

                Ingredients list:
                %s

                Recipe instructions to normalize:
                %s
                """.formatted(ingredients, recipeInstructions);
    }

    private String buildPlannerPrompt(String normalizedSteps) {
        return """
                You are an expert culinary planner for the CookMate system.
                CookMate is a multi-functional cooking device (like a Thermomix) that combines a pot, frying pan, blender, and steamer into one bowl.

                YOUR TASK:
                Take the provided list of raw kitchen activities and plan them to be executed primarily within ONE CookMate device.
                Even if the original recipe requires multiple pots, pans, or an oven, adapt it to be done sequentially or concurrently in the CookMate device using its heating, stirring, and blending functions.

                CRITICAL PLANNING RULES:
                1. CONSOLIDATE & ADAPT (MAX 8-10 STEPS): Group similar tasks. For example, if multiple vegetables need preparation (e.g., onions, garlic, carrots), combine them into a single CHOP step.
                2. SINGLE BOWL FLOW: Adapt steps to run in a single bowl. E.g., fry meat in the bowl, remove it, boil pasta in the same bowl, then mix them together.
                3. SEQUENCE: List the planned steps in a clear chronological order. Write clear, detailed instructional descriptions for each step, describing which attachments to use (e.g. blade, mixing butterfly) and what device modes are simulated.

                EXAMPLE:
                ---
                Raw kitchen activities to adapt:
                1. Chop 1 onion and 2 cloves of garlic.
                2. Heat 2 tbsp of olive oil in a skillet.
                3. Sauté chopped onion and garlic in the skillet.
                4. Add 500g of minced beef to the skillet and cook until browned.
                5. Add 400g of canned tomatoes to the skillet.
                6. Simmer the tomato and meat mixture for 15 minutes.
                7. Boil water in a separate large pot with 1 tsp of salt.
                8. Add 400g of spaghetti to the boiling water.
                9. Cook spaghetti until al dente.
                10. Drain the cooked spaghetti.
                11. Combine the cooked spaghetti with the simmered meat sauce.

                Planned CookMate steps:
                1. Add 1 onion (halved) and 2 garlic cloves to the CookMate bowl. Chop them at high speed.
                2. Add 2 tbsp of olive oil to the bowl. Sauté the chopped vegetables using the frying function.
                3. Add 500g of minced beef to the bowl. Stir and brown the meat alongside the vegetables.
                4. Pour 400g of canned tomatoes into the bowl. Simmer the sauce with gentle stirring.
                5. Transfer the prepared meat sauce to a separate bowl and rinse the CookMate bowl.
                6. Pour 1.5 liters of water and 1 tsp of salt into the clean CookMate bowl. Heat until boiling.
                7. Add 400g of spaghetti to the boiling water through the lid. Cook with gentle stirring.
                8. Drain the spaghetti using the steamer basket or a strainer.
                9. Return the drained spaghetti to the CookMate bowl, pour in the reserved meat sauce, and mix at low speed to combine.
                ---

                Now, plan the CookMate steps for the following raw kitchen activities:

                Raw kitchen activities to adapt:
                %s
                """.formatted(normalizedSteps);
    }

    private String buildSerializerPrompt(String plannedSteps, String ingredients) {
        return """
                You are an expert technical serializer for the CookMate system.
                Your task is to take the planned cooking steps and format them strictly into a structured JSON object matching the schema below.

                CRITICAL SERIALIZATION RULES:
                1. DOMINANT ACTIONS ONLY: Use ONLY ONE from: CUT, CHOP, STIR, MARINATE, BLEND, FRY, POT, BAKE, WEIGH, GRILL, WAIT, POUR, MIX.
                2. PARAMETERS: Always include "temperature" (in Celsius, use 0 if no heat is applied) and "speed" (0-10, use 0 if no rotation/mixing is needed) in the parameters object to guide the device simulator.
                3. WEIGH AND MEASURE INGREDIENTS: When planning steps that involve adding ingredients, use the WEIGH action for dry/solid ingredients and the POUR action for liquid ingredients. You MUST extract the precise quantity or weight (e.g., grams, milliliters, pieces) from the "Ingredients list" provided below and explicitly mention it in the step "description" (e.g., "Weigh 500g of chicken breast using the built-in scale"). Do not just say "Add chicken", say "Weigh and add 500g of chicken".
                4. DURATION: Specify "duration_minutes" as an integer.

                JSON Output Format:
                Each step MUST include:
                - "step_number": sequential integer.
                - "description": detailed instructions for the device user.
                - "main_ingredient": string or null.
                - "action": ONE of the allowed action types.
                - "parameters": JSON object e.g., {"temperature": 120, "speed": 3}.
                - "duration_minutes": integer.

                The root JSON structure must look like this:
                {
                  "steps": [
                    {
                      "step_number": 1,
                      "description": "...",
                      "main_ingredient": "...",
                      "action": "...",
                      "parameters": {"temperature": 120, "speed": 3},
                      "duration_minutes": 5
                    }
                  ]
                }

                EXAMPLE:
                ---
                Ingredients list:
                - spaghetti (400g)
                - olive oil (2 tbsp)
                - onion (1)
                - garlic (2 cloves)
                - minced beef (500g)
                - canned tomatoes (400g)
                - salt (1 tsp)

                Planned steps:
                1. Add 1 onion (halved) and 2 garlic cloves to the CookMate bowl. Chop them at high speed.
                2. Add 2 tbsp of olive oil to the bowl. Sauté the chopped vegetables using the frying function.
                3. Add 500g of minced beef to the bowl. Stir and brown the meat alongside the vegetables.
                4. Pour 400g of canned tomatoes into the bowl. Simmer the sauce with gentle stirring.
                5. Transfer the prepared meat sauce to a separate bowl and rinse the CookMate bowl.
                6. Pour 1.5 liters of water and 1 tsp of salt into the clean CookMate bowl. Heat until boiling.
                7. Add 400g of spaghetti to the boiling water through the lid. Cook with gentle stirring.
                8. Drain the spaghetti using the steamer basket or a strainer.
                9. Return the drained spaghetti to the CookMate bowl, pour in the reserved meat sauce, and mix at low speed to combine.

                JSON Output:
                {
                  "steps": [
                    {
                      "step_number": 1,
                      "description": "Place 1 onion (halved) and 2 garlic cloves into the CookMate bowl.",
                      "main_ingredient": "onion",
                      "action": "WEIGH",
                      "parameters": {"temperature": 0, "speed": 0},
                      "duration_minutes": 1
                    },
                    {
                      "step_number": 2,
                      "description": "Chop the onion and garlic inside the bowl by setting the device speed to 6 for 10 seconds.",
                      "main_ingredient": "onion",
                      "action": "CHOP",
                      "parameters": {"temperature": 0, "speed": 6},
                      "duration_minutes": 1
                    },
                    {
                      "step_number": 3,
                      "description": "Add 2 tbsp of olive oil to the bowl and sauté the chopped vegetables by setting the temperature to 120°C and speed to 1.",
                      "main_ingredient": "olive oil",
                      "action": "FRY",
                      "parameters": {"temperature": 120, "speed": 1},
                      "duration_minutes": 3
                    },
                    {
                      "step_number": 4,
                      "description": "Add 500g of minced beef to the bowl. Set the device to sauté mode at 120°C and speed 2 with the mixing blade to brown the meat.",
                      "main_ingredient": "minced beef",
                      "action": "FRY",
                      "parameters": {"temperature": 120, "speed": 2},
                      "duration_minutes": 8
                    },
                    {
                      "step_number": 5,
                      "description": "Pour 400g of canned tomatoes into the bowl. Simmer the mixture at 100°C and speed 1 to reduce and combine the sauce.",
                      "main_ingredient": "canned tomatoes",
                      "action": "POT",
                      "parameters": {"temperature": 100, "speed": 1},
                      "duration_minutes": 15
                    },
                    {
                      "step_number": 6,
                      "description": "Rinse the CookMate bowl, then pour 1.5 liters of water and add 1 tsp of salt. Heat the water until boiling by setting the temperature to 100°C and speed to 0.",
                      "main_ingredient": "water",
                      "action": "POUR",
                      "parameters": {"temperature": 100, "speed": 0},
                      "duration_minutes": 10
                    },
                    {
                      "step_number": 7,
                      "description": "Add 400g of spaghetti to the boiling water through the lid. Cook the pasta by setting the temperature to 100°C and speed to 1.",
                      "main_ingredient": "spaghetti",
                      "action": "POT",
                      "parameters": {"temperature": 100, "speed": 1},
                      "duration_minutes": 10
                    },
                    {
                      "step_number": 8,
                      "description": "Drain the cooked spaghetti using the steamer basket and set it aside.",
                      "main_ingredient": "spaghetti",
                      "action": "WAIT",
                      "parameters": {"temperature": 0, "speed": 0},
                      "duration_minutes": 2
                    },
                    {
                      "step_number": 9,
                      "description": "Return the drained spaghetti to the CookMate bowl, pour in the reserved meat sauce, and mix at speed 2 to combine them evenly.",
                      "main_ingredient": "spaghetti",
                      "action": "MIX",
                      "parameters": {"temperature": 80, "speed": 2},
                      "duration_minutes": 2
                    }
                  ]
                }
                ---

                Now, serialize the following planned steps:

                Ingredients list:
                %s

                Planned steps to serialize:
                %s
                """.formatted(ingredients, plannedSteps);
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
