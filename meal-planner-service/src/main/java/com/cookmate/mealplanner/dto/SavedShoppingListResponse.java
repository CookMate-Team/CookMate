package com.cookmate.mealplanner.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record SavedShoppingListResponse(UUID id, LocalDateTime createdAt, List<ShoppingListItem> items) {}
