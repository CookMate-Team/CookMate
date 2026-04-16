package com.cookmate.main.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Response model from TheMealDB API search/lookup endpoints.
 * Contains list of meals returned by the search.
 */
public record MealSearchResponse(
    /**
     * List of meals returned by the search.
     */
    List<Meal> meals
) {}
