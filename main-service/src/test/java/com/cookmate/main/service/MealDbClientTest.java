package com.cookmate.main.service;

import com.cookmate.main.dto.Meal;
import com.cookmate.main.dto.MealSearchResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MealDbClientTest {

    @Mock
    private WebClient webClient;
    @Mock
    @SuppressWarnings("rawtypes")
    private WebClient.RequestHeadersUriSpec requestHeadersUriSpec;
    @Mock
    @SuppressWarnings("rawtypes")
    private WebClient.RequestHeadersSpec requestHeadersSpec;
    @Mock
    private WebClient.ResponseSpec responseSpec;

    private MealDbClient mealDbClient;

    @BeforeEach
    void setUp() {
        mealDbClient = new MealDbClient(webClient);
    }

    @Test
    void searchByLetter_shouldCallTheMealDbAndReturnResponse() {
        MealSearchResponse expected = new MealSearchResponse(List.of());

        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(MealSearchResponse.class)).thenReturn(Mono.just(expected));

        MealSearchResponse actual = mealDbClient.searchByLetter("A").block();

        ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
        verify(requestHeadersUriSpec).uri(urlCaptor.capture());
        assertTrue(urlCaptor.getValue().contains("/search.php?f=a"));
        assertEquals(expected, actual);
    }

    @Test
    void lookupById_shouldCallTheMealDbAndReturnResponse() {
        MealSearchResponse expected = new MealSearchResponse(List.of());

        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(MealSearchResponse.class)).thenReturn(Mono.just(expected));

        MealSearchResponse actual = mealDbClient.lookupById("52772").block();

        ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
        verify(requestHeadersUriSpec).uri(urlCaptor.capture());
        assertTrue(urlCaptor.getValue().contains("/lookup.php?i=52772"));
        assertEquals(expected, actual);
    }

    @Test
    void searchByLetter_shouldMapJsonToMealRecord() {
        String searchResponseJson = """
            {
              "meals": [
                {
                  "idMeal": "53049",
                  "strMeal": "Apam balik",
                  "strCategory": "Dessert",
                  "strArea": "Malaysian",
                  "strInstructions": "Mix all ingredients",
                  "strIngredient1": "Milk",
                  "strMeasure1": "200ml"
                }
              ]
            }
            """;

        MealDbClient realClient = createClientReturningJson(searchResponseJson);

        MealSearchResponse response = realClient.searchByLetter("a").block();

        assertNotNull(response);
        assertNotNull(response.meals());
        assertEquals(1, response.meals().size());
        Meal meal = response.meals().getFirst();
        assertEquals("53049", meal.idMeal());
        assertEquals("Apam balik", meal.strMeal());
        assertEquals("Dessert", meal.strCategory());
        assertEquals("Malaysian", meal.strArea());
        assertEquals("Milk", meal.strIngredient1());
        assertEquals("200ml", meal.strMeasure1());
    }

    @Test
    void lookupById_shouldMapFullMealDetailsIncludingAllIngredients() {
        MealDbClient realClient = createClientReturningJson(lookupResponseWithFullIngredients());

        MealSearchResponse response = realClient.lookupById("52772").block();

        assertNotNull(response);
        assertNotNull(response.meals());
        assertEquals(1, response.meals().size());
        Meal meal = response.meals().getFirst();
        assertEquals("52772", meal.idMeal());
        assertEquals("Teriyaki Chicken Casserole", meal.strMeal());
        assertAllIngredientsWereMapped(meal);
    }

    @Test
    void searchByLetter_shouldRejectInvalidLetter() {
        assertThrows(IllegalArgumentException.class, () -> mealDbClient.searchByLetter("ab").block());
    }

    @Test
    void lookupById_shouldRejectBlankMealId() {
        assertThrows(IllegalArgumentException.class, () -> mealDbClient.lookupById(" ").block());
    }

    @Test
    void searchByLetter_shouldWrapDownstreamError() {
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(MealSearchResponse.class)).thenReturn(Mono.error(new RuntimeException("boom")));

        RuntimeException thrown = assertThrows(RuntimeException.class, () -> mealDbClient.searchByLetter("a").block());

        assertTrue(thrown.getMessage().contains("Error calling TheMealDB API"));
    }

    private MealDbClient createClientReturningJson(String body) {
        ExchangeFunction exchangeFunction = request -> Mono.just(
            ClientResponse.create(HttpStatus.OK)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .body(body)
                .build()
        );
        WebClient localWebClient = WebClient.builder().exchangeFunction(exchangeFunction).build();
        return new MealDbClient(localWebClient);
    }

    private void assertAllIngredientsWereMapped(Meal meal) {
        assertEquals("Ingredient1", meal.strIngredient1());
        assertEquals("Ingredient2", meal.strIngredient2());
        assertEquals("Ingredient3", meal.strIngredient3());
        assertEquals("Ingredient4", meal.strIngredient4());
        assertEquals("Ingredient5", meal.strIngredient5());
        assertEquals("Ingredient6", meal.strIngredient6());
        assertEquals("Ingredient7", meal.strIngredient7());
        assertEquals("Ingredient8", meal.strIngredient8());
        assertEquals("Ingredient9", meal.strIngredient9());
        assertEquals("Ingredient10", meal.strIngredient10());
        assertEquals("Ingredient11", meal.strIngredient11());
        assertEquals("Ingredient12", meal.strIngredient12());
        assertEquals("Ingredient13", meal.strIngredient13());
        assertEquals("Ingredient14", meal.strIngredient14());
        assertEquals("Ingredient15", meal.strIngredient15());
        assertEquals("Ingredient16", meal.strIngredient16());
        assertEquals("Ingredient17", meal.strIngredient17());
        assertEquals("Ingredient18", meal.strIngredient18());
        assertEquals("Ingredient19", meal.strIngredient19());
        assertEquals("Ingredient20", meal.strIngredient20());
    }

    private String lookupResponseWithFullIngredients() {
        return """
            {
              "meals": [
                {
                  "idMeal": "52772",
                  "strMeal": "Teriyaki Chicken Casserole",
                  "strCategory": "Chicken",
                  "strArea": "Japanese",
                  "strInstructions": "Cook instructions",
                  "strIngredient1": "Ingredient1",
                  "strMeasure1": "Measure1",
                  "strIngredient2": "Ingredient2",
                  "strMeasure2": "Measure2",
                  "strIngredient3": "Ingredient3",
                  "strMeasure3": "Measure3",
                  "strIngredient4": "Ingredient4",
                  "strMeasure4": "Measure4",
                  "strIngredient5": "Ingredient5",
                  "strMeasure5": "Measure5",
                  "strIngredient6": "Ingredient6",
                  "strMeasure6": "Measure6",
                  "strIngredient7": "Ingredient7",
                  "strMeasure7": "Measure7",
                  "strIngredient8": "Ingredient8",
                  "strMeasure8": "Measure8",
                  "strIngredient9": "Ingredient9",
                  "strMeasure9": "Measure9",
                  "strIngredient10": "Ingredient10",
                  "strMeasure10": "Measure10",
                  "strIngredient11": "Ingredient11",
                  "strMeasure11": "Measure11",
                  "strIngredient12": "Ingredient12",
                  "strMeasure12": "Measure12",
                  "strIngredient13": "Ingredient13",
                  "strMeasure13": "Measure13",
                  "strIngredient14": "Ingredient14",
                  "strMeasure14": "Measure14",
                  "strIngredient15": "Ingredient15",
                  "strMeasure15": "Measure15",
                  "strIngredient16": "Ingredient16",
                  "strMeasure16": "Measure16",
                  "strIngredient17": "Ingredient17",
                  "strMeasure17": "Measure17",
                  "strIngredient18": "Ingredient18",
                  "strMeasure18": "Measure18",
                  "strIngredient19": "Ingredient19",
                  "strMeasure19": "Measure19",
                  "strIngredient20": "Ingredient20",
                  "strMeasure20": "Measure20"
                }
              ]
            }
            """;
    }
}

