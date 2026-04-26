package com.cookmate.main.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record CategoryResponse(
        List<CategoryDTO> categories
) {
    public record CategoryDTO(
            @JsonProperty("idCategory") String id,
            @JsonProperty("strCategory") String name,
            @JsonProperty("strCategoryThumb") String thumbnail,
            @JsonProperty("strCategoryDescription") String description
    ) {}
}