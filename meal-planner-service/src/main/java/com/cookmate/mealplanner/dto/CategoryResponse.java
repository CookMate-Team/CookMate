package com.cookmate.mealplanner.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record CategoryResponse(
        @JsonProperty("categories") List<Category> categories
) {
    public record Category(
            @JsonProperty("idCategory") String id,
            @JsonProperty("strCategory") String name
    ) {}
}
