package com.cookmate.main.service;

import com.cookmate.main.dto.MealSearchResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/**
 * Client for communicating with TheMealDB Recipe API.
 * Handles external API calls for meal searching and retrieval.
 * 
 * API Documentation: https://www.themealdb.com/api.php
 */
@Service
public class MealDbClient {

    private static final String BASE_URL = "https://www.themealdb.com/api/json/v1/1";
    private static final String SEARCH_BY_LETTER = "/search.php";
    private static final String LOOKUP_BY_ID = "/lookup.php";

    private final WebClient webClient;

    /**
     * Construct MealDbClient with WebClient.
     *
     * @param webClient configured WebClient instance
     */
    public MealDbClient(WebClient webClient) {
        this.webClient = webClient;
    }

    /**
     * Search for meals by first letter.
     * 
     * API: GET /search.php?f={letter}
     * Returns all meals whose name begins with the given letter.
     *
     * @param letter first letter to search for (a-z)
     * @return Mono containing search response with meals list
     */
    public Mono<MealSearchResponse> searchByLetter(String letter) {
        if (letter == null || letter.length() != 1) {
            return Mono.error(new IllegalArgumentException("Letter must be a single character"));
        }

        String url = BASE_URL + SEARCH_BY_LETTER + "?f=" + letter.toLowerCase();

        return webClient.get()
            .uri(url)
            .retrieve()
            .bodyToMono(MealSearchResponse.class)
            .doOnError(error -> {
                throw new RuntimeException("Error calling TheMealDB API: " + error.getMessage(), error);
            });
    }

    /**
     * Lookup full meal details by meal ID.
     * 
     * API: GET /lookup.php?i={id}
     * Returns complete meal information including all ingredients and measurements.
     *
     * @param mealId meal ID from TheMealDB
     * @return Mono containing search response with meal details
     */
    public Mono<MealSearchResponse> lookupById(String mealId) {
        if (mealId == null || mealId.isBlank()) {
            return Mono.error(new IllegalArgumentException("Meal ID cannot be blank"));
        }

        String url = BASE_URL + LOOKUP_BY_ID + "?i=" + mealId;

        return webClient.get()
            .uri(url)
            .retrieve()
            .bodyToMono(MealSearchResponse.class)
            .doOnError(error -> {
                throw new RuntimeException("Error calling TheMealDB API: " + error.getMessage(), error);
            });
    }
}
