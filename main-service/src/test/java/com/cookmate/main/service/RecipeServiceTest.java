package com.cookmate.main.service;

import com.cookmate.main.dto.Meal;
import com.cookmate.main.dto.MealSearchResponse;
import com.cookmate.main.dto.RecipeListResponse;
import com.cookmate.main.model.Recipe;
import com.cookmate.main.repository.RecipeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecipeServiceTest {

    @Mock
    private RecipeRepository recipeRepository;
    @Mock
    private MealDbClient mealDbClient;

    private RecipeService recipeService;

    @BeforeEach
    void setUp() {
        recipeService = new RecipeService(recipeRepository, mealDbClient);
    }

    @Test
    void searchMealsByLetter_shouldDelegateToMealDbClient() {
        MealSearchResponse expected = new MealSearchResponse(List.of());
        when(mealDbClient.searchByLetter("a")).thenReturn(Mono.just(expected));

        MealSearchResponse actual = recipeService.searchMealsByLetter("a").block();

        assertEquals(expected, actual);
        verify(mealDbClient).searchByLetter("a");
    }

    @Test
    void lookupMeal_shouldDelegateToMealDbClient() {
        MealSearchResponse expected = new MealSearchResponse(List.of());
        when(mealDbClient.lookupById("52772")).thenReturn(Mono.just(expected));

        MealSearchResponse actual = recipeService.lookupMeal("52772").block();

        assertEquals(expected, actual);
        verify(mealDbClient).lookupById("52772");
    }

    @Test
    void findPaginated_shouldMapRecipesAndMetadata() {
        Recipe recipe = new Recipe("Pasta", "desc", "ingredient", "instruction", 20);
        recipe.setId(10L);
        recipe.setCreatedAt(LocalDateTime.now());

        when(recipeRepository.findAll(eq(PageRequest.of(0, 10))))
            .thenReturn(new PageImpl<>(List.of(recipe), PageRequest.of(0, 10), 1));

        RecipeListResponse response = recipeService.findPaginated(0, 10);

        assertEquals(1, response.recipes().size());
        assertEquals(1, response.totalCount());
        assertEquals(0, response.pageNumber());
        assertEquals(10, response.pageSize());
        assertEquals(1, response.totalPages());
        assertEquals("Pasta", response.recipes().get(0).name());
    }

    @Test
    void syncMealFromTheMealDB_shouldSaveMappedRecipeAndSkipBlankIngredients() {
        Meal meal = buildMealForSync();
        when(recipeRepository.save(any(Recipe.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Recipe saved = recipeService.syncMealFromTheMealDB(meal);

        assertNotNull(saved);
        assertEquals("Teriyaki Chicken Casserole", saved.getName());
        assertEquals("Category: Chicken | Area: Japanese", saved.getDescription());
        assertEquals("Chicken (200g), Salt", saved.getIngredients());
        assertEquals("Cook instructions", saved.getInstructions());

        ArgumentCaptor<Recipe> captor = ArgumentCaptor.forClass(Recipe.class);
        verify(recipeRepository).save(captor.capture());
        assertEquals("Chicken (200g), Salt", captor.getValue().getIngredients());
    }

    private Meal buildMealForSync() {
        return new Meal(
            "52772",
            "Teriyaki Chicken Casserole",
            null,
            "Chicken",
            "Japanese",
            "Cook instructions",
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            "Chicken",
            "200g",
            " ",
            "1 tsp",
            "Salt",
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null
        );
    }
}

