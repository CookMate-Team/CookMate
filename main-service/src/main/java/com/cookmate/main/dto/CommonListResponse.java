package com.cookmate.main.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record CommonListResponse(
        List<Item> meals
) {
    public record Item(
            @JsonProperty("strCategory") String categoryName,
            @JsonProperty("strArea") String areaName,
            @JsonProperty("strIngredient") String ingredientName
    ) {}
}