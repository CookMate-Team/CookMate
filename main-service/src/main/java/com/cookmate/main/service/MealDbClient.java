package com.cookmate.main.service;

import com.cookmate.main.dto.CategoryResponse;
import com.cookmate.main.dto.CommonListResponse;
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

    private final WebClient webClient;

    /**
     * Construct MealDbClient with WebClient.
     *
     * @param webClient configured WebClient instance
     */
    public MealDbClient(WebClient webClient) {
        this.webClient = webClient;
    }


    // 1. Search meal by name
    public Mono<MealSearchResponse> searchByName(String name) {
        return fetch("/search.php?s=" + name, MealSearchResponse.class);
    }

    // 2. Lookup full meal details by id
    public Mono<MealSearchResponse> lookupById(String mealId) {
        return fetch("/lookup.php?i=" + mealId, MealSearchResponse.class);
    }

    // 3. List all meal categories (with details)
    public Mono<CategoryResponse> listFullCategories() {
        return fetch("/categories.php", CategoryResponse.class);
    }

    // 4. Filter by main ingredient
    public Mono<MealSearchResponse> filterByIngredient(String ingredient) {
        return fetch("/filter.php?i=" + ingredient, MealSearchResponse.class);
    }

    // 5. List all Categories, Area, Ingredients (Dictionaries)
    public Mono<CommonListResponse> listAllBy(String type) {
        // type: "c" for category, "a" for area, "i" for ingredients
        return fetch("/list.php?" + type + "=list", CommonListResponse.class);
    }

    private <T> Mono<T> fetch(String endpoint, Class<T> responseType) {
        return webClient.get()
                .uri(BASE_URL + endpoint)
                .retrieve()
                .bodyToMono(responseType)
                .doOnError(error -> {
                    throw new RuntimeException("Error calling TheMealDB API: " + error.getMessage(), error);
                });
    }
}
