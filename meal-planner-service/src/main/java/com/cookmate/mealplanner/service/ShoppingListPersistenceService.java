package com.cookmate.mealplanner.service;

import com.cookmate.mealplanner.dto.SavedShoppingListResponse;
import com.cookmate.mealplanner.dto.ShoppingListItem;
import com.cookmate.mealplanner.dto.ShoppingListResponse;
import com.cookmate.mealplanner.model.ShoppingList;
import com.cookmate.mealplanner.model.ShoppingListItemEntity;
import com.cookmate.mealplanner.repository.ShoppingListRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ShoppingListPersistenceService {

    private static final Logger logger = LoggerFactory.getLogger(ShoppingListPersistenceService.class);
    private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() {};

    private final ShoppingListRepository shoppingListRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public SavedShoppingListResponse save(String userId, ShoppingListResponse request) {
        logger.info("Saving shopping list for userId={}, items={}", userId, request.items().size());

        ShoppingList list = new ShoppingList();
        list.setUserId(userId);

        List<ShoppingListItemEntity> items = request.items().stream()
                .map(item -> toEntity(item, list))
                .toList();
        list.setItems(items);

        ShoppingList saved = shoppingListRepository.save(list);
        logger.info("Shopping list saved with id={} for userId={}", saved.getId(), userId);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<SavedShoppingListResponse> getHistory(String userId) {
        logger.info("Fetching shopping list history for userId={}", userId);
        List<ShoppingList> lists = shoppingListRepository.findByUserIdOrderByCreatedAtDesc(userId);
        logger.info("Found {} shopping lists for userId={}", lists.size(), userId);
        return lists.stream().map(this::toResponse).toList();
    }

    private ShoppingListItemEntity toEntity(ShoppingListItem item, ShoppingList list) {
        ShoppingListItemEntity entity = new ShoppingListItemEntity();
        entity.setShoppingList(list);
        entity.setIngredientName(item.name());
        entity.setMeasures(toJson(item.measures()));
        entity.setRecipeNames(toJson(item.recipes()));
        return entity;
    }

    private SavedShoppingListResponse toResponse(ShoppingList list) {
        List<ShoppingListItem> items = list.getItems().stream()
                .map(item -> new ShoppingListItem(
                        item.getIngredientName(),
                        fromJson(item.getMeasures()),
                        fromJson(item.getRecipeNames())
                ))
                .toList();
        return new SavedShoppingListResponse(list.getId(), list.getCreatedAt(), items);
    }

    private String toJson(List<String> values) {
        try {
            return objectMapper.writeValueAsString(values);
        } catch (JsonProcessingException e) {
            logger.error("Failed to serialize list to JSON", e);
            return "[]";
        }
    }

    private List<String> fromJson(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, STRING_LIST);
        } catch (JsonProcessingException e) {
            logger.error("Failed to deserialize JSON to list: {}", json, e);
            return List.of();
        }
    }
}
