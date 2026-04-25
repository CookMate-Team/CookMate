package com.cookmate.main.service;

import com.cookmate.main.dto.MealSearchResponse;
import com.cookmate.main.dto.CategoryResponse;
import com.cookmate.main.dto.CommonListResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
public class MealDbClient {

    private static final String BASE_URL = "https://www.themealdb.com/api/json/v1/1";

    private final WebClient webClient;

    public MealDbClient(WebClient webClient) {
        this.webClient = webClient;
    }

    /**
     * Wyszukiwanie po nazwie (?s=)
     */
    public Mono<MealSearchResponse> searchByName(String name) {
        return fetch("/search.php?s=" + name, MealSearchResponse.class);
    }

    /**
     * Pobieranie po ID (?i=)
     */
    public Mono<MealSearchResponse> lookupById(String mealId) {
        return fetch("/lookup.php?i=" + mealId, MealSearchResponse.class);
    }

    /**
     * Filtrowanie po składniku (?i=)
     */
    public Mono<MealSearchResponse> filterByIngredient(String ingredient) {
        return fetch("/filter.php?i=" + ingredient, MealSearchResponse.class);
    }

    /**
     * Pełna lista kategorii z opisami (categories.php)
     */
    public Mono<CategoryResponse> listFullCategories() {
        return fetch("/categories.php", CategoryResponse.class);
    }

    /**
     * Listy słownikowe (list.php?c=list, a=list, i=list)
     */
    public Mono<CommonListResponse> listAllBy(String type) {
        return fetch("/list.php?" + type + "=list", CommonListResponse.class);
    }

    private <T> Mono<T> fetch(String endpoint, Class<T> responseType) {
        return webClient.get()
                .uri(BASE_URL + endpoint)
                .retrieve()
                .bodyToMono(responseType)
                .doOnError(error -> {
                    throw new RuntimeException("Błąd podczas wywołania API TheMealDB: " + error.getMessage(), error);
                });
    }
}