package com.cookmate.mealplanner.service;

import com.cookmate.mealplanner.dto.SavedShoppingListResponse;
import com.cookmate.mealplanner.dto.ShoppingListItem;
import com.cookmate.mealplanner.dto.ShoppingListResponse;
import com.cookmate.mealplanner.model.ShoppingList;
import com.cookmate.mealplanner.model.ShoppingListItemEntity;
import com.cookmate.mealplanner.repository.ShoppingListRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("ShoppingListPersistenceService — unit tests")
class ShoppingListPersistenceServiceTest {

    @Mock
    private ShoppingListRepository shoppingListRepository;

    private ShoppingListPersistenceService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new ShoppingListPersistenceService(shoppingListRepository, new ObjectMapper());
        when(shoppingListRepository.save(any(ShoppingList.class))).thenAnswer(invocation -> {
            ShoppingList list = invocation.getArgument(0);
            list.setId(UUID.fromString("00000000-0000-0000-0000-000000000001"));
            return list;
        });
    }

    // --- helpers ---

    private ShoppingListResponse twoItemResponse() {
        return new ShoppingListResponse(List.of(
                new ShoppingListItem("Salt", List.of("1 tsp", "2 tsp"), List.of("Pasta", "Soup")),
                new ShoppingListItem("Garlic", List.of("2 cloves"), List.of("Pasta"))
        ));
    }

    private ShoppingList savedList(UUID id, String userId) {
        ShoppingList list = new ShoppingList();
        list.setId(id);
        list.setUserId(userId);
        list.setCreatedAt(LocalDateTime.of(2026, 1, 10, 12, 0));

        ShoppingListItemEntity item = new ShoppingListItemEntity();
        item.setShoppingList(list);
        item.setIngredientName("Salt");
        item.setMeasures("[\"1 tsp\",\"2 tsp\"]");
        item.setRecipeNames("[\"Pasta\",\"Soup\"]");
        list.setItems(List.of(item));
        return list;
    }

    // --- save ---

    @Test
    @DisplayName("save — wywołuje repository.save")
    void save_callsRepository() {
        service.save("user-1", twoItemResponse());
        verify(shoppingListRepository).save(any(ShoppingList.class));
    }

    @Test
    @DisplayName("save — zwraca odpowiedź z wygenerowanym id")
    void save_returnsResponseWithId() {
        SavedShoppingListResponse response = service.save("user-1", twoItemResponse());
        assertThat(response.id()).isNotNull();
    }

    @Test
    @DisplayName("save — zachowuje poprawną liczbę elementów")
    void save_preservesItemCount() {
        SavedShoppingListResponse response = service.save("user-1", twoItemResponse());
        assertThat(response.items()).hasSize(2);
    }

    @Test
    @DisplayName("save — miary są serializowane do JSON i poprawnie deserializowane w odpowiedzi")
    void save_measuresAreSerializedAndDeserializedCorrectly() {
        SavedShoppingListResponse response = service.save("user-1", twoItemResponse());

        ShoppingListItem salt = response.items().stream()
                .filter(i -> "Salt".equals(i.name()))
                .findFirst()
                .orElseThrow();
        assertThat(salt.measures()).containsExactly("1 tsp", "2 tsp");
        assertThat(salt.recipes()).containsExactly("Pasta", "Soup");
    }

    @Test
    @DisplayName("save — nazwy przepisów są serializowane do JSON")
    void save_recipeNamesAreSerializedCorrectly() {
        SavedShoppingListResponse response = service.save("user-1", twoItemResponse());

        ShoppingListItem garlic = response.items().stream()
                .filter(i -> "Garlic".equals(i.name()))
                .findFirst()
                .orElseThrow();
        assertThat(garlic.recipes()).containsExactly("Pasta");
    }

    @Test
    @DisplayName("save — pusta lista elementów zapisuje pustą listę zakupów")
    void save_emptyItems_savesEmptyList() {
        ShoppingListResponse empty = new ShoppingListResponse(List.of());
        SavedShoppingListResponse response = service.save("user-1", empty);
        assertThat(response.items()).isEmpty();
    }

    // --- getHistory ---

    @Test
    @DisplayName("getHistory — zwraca zmapowane listy dla użytkownika")
    void getHistory_returnsMappedListsForUser() {
        UUID id = UUID.randomUUID();
        when(shoppingListRepository.findByUserIdOrderByCreatedAtDesc("user-1"))
                .thenReturn(List.of(savedList(id, "user-1")));

        List<SavedShoppingListResponse> history = service.getHistory("user-1");

        assertThat(history).hasSize(1);
        assertThat(history.get(0).id()).isEqualTo(id);
    }

    @Test
    @DisplayName("getHistory — JSON miary są deserializowane z powrotem do listy")
    void getHistory_measuresAreDeserializedFromJson() {
        when(shoppingListRepository.findByUserIdOrderByCreatedAtDesc("user-1"))
                .thenReturn(List.of(savedList(UUID.randomUUID(), "user-1")));

        List<SavedShoppingListResponse> history = service.getHistory("user-1");

        ShoppingListItem salt = history.get(0).items().get(0);
        assertThat(salt.name()).isEqualTo("Salt");
        assertThat(salt.measures()).containsExactly("1 tsp", "2 tsp");
        assertThat(salt.recipes()).containsExactly("Pasta", "Soup");
    }

    @Test
    @DisplayName("getHistory — pusta historia zwraca pustą listę")
    void getHistory_emptyHistory_returnsEmptyList() {
        when(shoppingListRepository.findByUserIdOrderByCreatedAtDesc("user-1"))
                .thenReturn(List.of());

        List<SavedShoppingListResponse> history = service.getHistory("user-1");

        assertThat(history).isEmpty();
    }

    @Test
    @DisplayName("getHistory — null/pusty JSON measures deserializuje się do pustej listy")
    void getHistory_nullMeasuresJson_returnsEmptyList() {
        ShoppingList list = savedList(UUID.randomUUID(), "user-1");
        list.getItems().get(0).setMeasures(null);
        when(shoppingListRepository.findByUserIdOrderByCreatedAtDesc("user-1"))
                .thenReturn(List.of(list));

        List<SavedShoppingListResponse> history = service.getHistory("user-1");

        assertThat(history.get(0).items().get(0).measures()).isEmpty();
    }
}
