package com.cookmate.mealplanner.dto;

import java.util.List;

public record ShoppingListRequest(List<String> mealIds) {}
