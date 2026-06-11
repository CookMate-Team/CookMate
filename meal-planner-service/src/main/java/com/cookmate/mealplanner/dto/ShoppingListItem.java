package com.cookmate.mealplanner.dto;

import java.util.List;

public record ShoppingListItem(String name, List<String> measures, List<String> recipes) {}
