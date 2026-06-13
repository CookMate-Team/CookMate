package com.cookmate.mealplanner.service;

import com.cookmate.mealplanner.client.MainServiceClient;
import com.cookmate.mealplanner.dto.MealDetailListResponse;
import com.cookmate.mealplanner.dto.MealDetailResponse;
import com.cookmate.mealplanner.dto.ShoppingListItem;
import com.cookmate.mealplanner.dto.ShoppingListRequest;
import com.cookmate.mealplanner.dto.ShoppingListResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("ShoppingListService — unit tests")
class ShoppingListServiceTest {

    @Mock
    private MainServiceClient mainServiceClient;

    @InjectMocks
    private ShoppingListService shoppingListService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    // --- helpers ---

    private MealDetailResponse mealWith(String id, String name,
                                        String[] ingredients, String[] measures) {
        MealDetailResponse m = new MealDetailResponse();
        m.setIdMeal(id);
        m.setStrMeal(name);
        if (ingredients.length > 0)  m.setStrIngredient1(ingredients[0]);
        if (ingredients.length > 1)  m.setStrIngredient2(ingredients[1]);
        if (ingredients.length > 2)  m.setStrIngredient3(ingredients[2]);
        if (measures.length > 0)     m.setStrMeasure1(measures[0]);
        if (measures.length > 1)     m.setStrMeasure2(measures[1]);
        if (measures.length > 2)     m.setStrMeasure3(measures[2]);
        return m;
    }

    private MealDetailListResponse wrap(MealDetailResponse... meals) {
        return new MealDetailListResponse(List.of(meals));
    }

    // --- tests ---

    @Test
    @DisplayName("pusta lista mealIds zwraca pustą listę składników")
    void emptyMealIds_returnsEmptyItems() {
        ShoppingListResponse result = shoppingListService.buildShoppingList(new ShoppingListRequest(List.of()));
        assertThat(result.items()).isEmpty();
    }

    @Test
    @DisplayName("pojedynczy posiłek — wszystkie składniki trafiają na listę")
    void singleMeal_allIngredientsReturned() {
        MealDetailResponse meal = mealWith("1", "Pasta",
                new String[]{"Spaghetti", "Tomato Sauce", "Parmesan"},
                new String[]{"200g", "100ml", "50g"});
        when(mainServiceClient.lookupById("1")).thenReturn(wrap(meal));

        ShoppingListResponse result = shoppingListService.buildShoppingList(new ShoppingListRequest(List.of("1")));

        assertThat(result.items()).hasSize(3);
        assertThat(result.items()).extracting(ShoppingListItem::name)
                .containsExactlyInAnyOrder("Spaghetti", "Tomato Sauce", "Parmesan");
    }

    @Test
    @DisplayName("duplikat składnika z dwóch posiłków — jeden wpis z oboma przepisami i oboma miarami")
    void duplicateIngredient_mergesRecipesAndMeasures() {
        MealDetailResponse meal1 = mealWith("1", "Pasta",
                new String[]{"Garlic"}, new String[]{"2 cloves"});
        MealDetailResponse meal2 = mealWith("2", "Soup",
                new String[]{"Garlic"}, new String[]{"1 clove"});
        when(mainServiceClient.lookupById("1")).thenReturn(wrap(meal1));
        when(mainServiceClient.lookupById("2")).thenReturn(wrap(meal2));

        ShoppingListResponse result = shoppingListService.buildShoppingList(
                new ShoppingListRequest(List.of("1", "2")));

        assertThat(result.items()).hasSize(1);
        ShoppingListItem garlic = result.items().get(0);
        assertThat(garlic.name()).isEqualTo("Garlic");
        assertThat(garlic.measures()).containsExactly("2 cloves", "1 clove");
        assertThat(garlic.recipes()).containsExactlyInAnyOrder("Pasta", "Soup");
    }

    @Test
    @DisplayName("unikalny składnik — measures i recipes zawierają po jednym elemencie")
    void uniqueIngredient_singleMeasureAndRecipe() {
        MealDetailResponse meal = mealWith("1", "Pizza",
                new String[]{"Mozzarella"}, new String[]{"150g"});
        when(mainServiceClient.lookupById("1")).thenReturn(wrap(meal));

        ShoppingListResponse result = shoppingListService.buildShoppingList(
                new ShoppingListRequest(List.of("1")));

        assertThat(result.items()).hasSize(1);
        ShoppingListItem item = result.items().get(0);
        assertThat(item.measures()).containsExactly("150g");
        assertThat(item.recipes()).containsExactly("Pizza");
    }

    @Test
    @DisplayName("puste/null składniki są pomijane")
    void nullAndBlankIngredients_areSkipped() {
        MealDetailResponse meal = new MealDetailResponse();
        meal.setIdMeal("1");
        meal.setStrMeal("Stew");
        meal.setStrIngredient1("Beef");
        meal.setStrMeasure1("300g");
        meal.setStrIngredient2("");
        meal.setStrIngredient3(null);
        meal.setStrIngredient4("  ");
        when(mainServiceClient.lookupById("1")).thenReturn(wrap(meal));

        ShoppingListResponse result = shoppingListService.buildShoppingList(
                new ShoppingListRequest(List.of("1")));

        assertThat(result.items()).hasSize(1);
        assertThat(result.items().get(0).name()).isEqualTo("Beef");
    }

    @Test
    @DisplayName("lookupById jest wywoływany dla każdego mealId")
    void lookupById_calledForEachMealId() {
        MealDetailResponse meal1 = mealWith("1", "A", new String[]{"Salt"}, new String[]{"1 tsp"});
        MealDetailResponse meal2 = mealWith("2", "B", new String[]{"Pepper"}, new String[]{"1 tsp"});
        when(mainServiceClient.lookupById("1")).thenReturn(wrap(meal1));
        when(mainServiceClient.lookupById("2")).thenReturn(wrap(meal2));

        shoppingListService.buildShoppingList(new ShoppingListRequest(List.of("1", "2")));

        verify(mainServiceClient).lookupById("1");
        verify(mainServiceClient).lookupById("2");
    }

    @Test
    @DisplayName("null odpowiedź z klienta jest pomijana bez błędu")
    void nullClientResponse_isIgnoredGracefully() {
        when(mainServiceClient.lookupById("99")).thenReturn(null);

        ShoppingListResponse result = shoppingListService.buildShoppingList(
                new ShoppingListRequest(List.of("99")));

        assertThat(result.items()).isEmpty();
    }

    @Test
    @DisplayName("null lista posiłków w odpowiedzi jest pomijana bez błędu")
    void nullMealsInResponse_isIgnoredGracefully() {
        when(mainServiceClient.lookupById("99")).thenReturn(new MealDetailListResponse(null));

        ShoppingListResponse result = shoppingListService.buildShoppingList(
                new ShoppingListRequest(List.of("99")));

        assertThat(result.items()).isEmpty();
    }

    @Test
    @DisplayName("ten sam składnik dwa razy w jednym przepisie — przepis pojawia się tylko raz w recipes, obie miary w measures")
    void ingredientTwiceInOneMeal_recipeListedOnlyOnce_bothMeasuresCollected() {
        MealDetailResponse meal = new MealDetailResponse();
        meal.setIdMeal("1");
        meal.setStrMeal("Osso Buco alla Milanese");
        meal.setStrIngredient1("Garlic");
        meal.setStrMeasure1("2 cloves");
        meal.setStrIngredient2("Garlic");
        meal.setStrMeasure2("1 clove");
        when(mainServiceClient.lookupById("1")).thenReturn(wrap(meal));

        ShoppingListResponse result = shoppingListService.buildShoppingList(
                new ShoppingListRequest(List.of("1")));

        assertThat(result.items()).hasSize(1);
        ShoppingListItem item = result.items().get(0);
        assertThat(item.measures()).containsExactly("2 cloves", "1 clove");
        assertThat(item.recipes()).containsExactly("Osso Buco alla Milanese");
    }

    @Test
    @DisplayName("składnik z dwóch różnych posiłków — obie miary są zbierane w kolejności")
    void duplicateIngredient_collectsAllMeasuresInOrder() {
        MealDetailResponse meal1 = mealWith("1", "Soup", new String[]{"Salt"}, new String[]{"1 tsp"});
        MealDetailResponse meal2 = mealWith("2", "Stew", new String[]{"Salt"}, new String[]{"2 tsp"});
        when(mainServiceClient.lookupById("1")).thenReturn(wrap(meal1));
        when(mainServiceClient.lookupById("2")).thenReturn(wrap(meal2));

        ShoppingListResponse result = shoppingListService.buildShoppingList(
                new ShoppingListRequest(List.of("1", "2")));

        assertThat(result.items()).hasSize(1);
        assertThat(result.items().get(0).measures()).containsExactly("1 tsp", "2 tsp");
    }

    @Test
    @DisplayName("wszystkie 20 slotów składników jest odczytywanych")
    void allTwentyIngredientSlots_areRead() {
        MealDetailResponse meal = new MealDetailResponse();
        meal.setIdMeal("1");
        meal.setStrMeal("BigMeal");
        meal.setStrIngredient1("A1"); meal.setStrMeasure1("m1");
        meal.setStrIngredient2("A2"); meal.setStrMeasure2("m2");
        meal.setStrIngredient3("A3"); meal.setStrMeasure3("m3");
        meal.setStrIngredient4("A4"); meal.setStrMeasure4("m4");
        meal.setStrIngredient5("A5"); meal.setStrMeasure5("m5");
        meal.setStrIngredient6("A6"); meal.setStrMeasure6("m6");
        meal.setStrIngredient7("A7"); meal.setStrMeasure7("m7");
        meal.setStrIngredient8("A8"); meal.setStrMeasure8("m8");
        meal.setStrIngredient9("A9"); meal.setStrMeasure9("m9");
        meal.setStrIngredient10("A10"); meal.setStrMeasure10("m10");
        meal.setStrIngredient11("A11"); meal.setStrMeasure11("m11");
        meal.setStrIngredient12("A12"); meal.setStrMeasure12("m12");
        meal.setStrIngredient13("A13"); meal.setStrMeasure13("m13");
        meal.setStrIngredient14("A14"); meal.setStrMeasure14("m14");
        meal.setStrIngredient15("A15"); meal.setStrMeasure15("m15");
        meal.setStrIngredient16("A16"); meal.setStrMeasure16("m16");
        meal.setStrIngredient17("A17"); meal.setStrMeasure17("m17");
        meal.setStrIngredient18("A18"); meal.setStrMeasure18("m18");
        meal.setStrIngredient19("A19"); meal.setStrMeasure19("m19");
        meal.setStrIngredient20("A20"); meal.setStrMeasure20("m20");
        when(mainServiceClient.lookupById("1")).thenReturn(wrap(meal));

        ShoppingListResponse result = shoppingListService.buildShoppingList(
                new ShoppingListRequest(List.of("1")));

        assertThat(result.items()).hasSize(20);
    }
}
