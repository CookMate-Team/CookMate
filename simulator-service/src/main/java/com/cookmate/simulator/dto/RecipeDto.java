package com.cookmate.simulator.dto;

import java.time.LocalDateTime;

public record RecipeDto(Long id, String name, String description, String ingredients, String instructions, Integer preparationTimeMinutes, LocalDateTime createdAt) {}
