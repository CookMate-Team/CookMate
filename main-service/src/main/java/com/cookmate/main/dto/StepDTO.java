package com.cookmate.main.dto;

import com.cookmate.main.model.ActionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StepDTO {

    private Long id;
    private Integer stepNumber;
    private String description;
    private ActionType action;
    private String parameters;
    private Integer duration;
    private String recipeId;
    private LocalDateTime createdAt;
}
