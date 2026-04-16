package com.cookmate.main.dto;

import java.util.List;

/**
 * Response DTO for paginated list of recipes.
 * Contains recipes and pagination metadata.
 */
public record RecipeListResponse(
    /**
     * List of recipes in the current page.
     */
    List<RecipeDTO> recipes,

    /**
     * Total number of recipes across all pages.
     */
    Integer totalCount,

    /**
     * Current page number (1-based).
     */
    Integer pageNumber,

    /**
     * Number of items per page.
     */
    Integer pageSize,

    /**
     * Total number of pages available.
     */
    Integer totalPages
) {}
