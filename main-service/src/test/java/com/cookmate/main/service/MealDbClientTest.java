package com.cookmate.main.service;

import com.cookmate.main.dto.Meal;
import com.cookmate.main.dto.MealSearchResponse;
import com.cookmate.main.dto.CommonListResponse;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;

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
    void searchByName_shouldCallTheMealDbWithCorrectQuery() {
        MealSearchResponse expected = new MealSearchResponse(List.of());

        // Mockowanie fluent API WebClienta
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(MealSearchResponse.class)).thenReturn(Mono.just(expected));

        mealDbClient.searchByName("Arrabiata").block();

        ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
        verify(requestHeadersUriSpec).uri(urlCaptor.capture());

        // Weryfikacja czy URL zawiera poprawny parametr 's' zamiast 'f'
        assertTrue(urlCaptor.getValue().contains("/search.php?s=Arrabiata"));
    }

    @Test
    void lookupById_shouldMapFullMealDetailsRigorously() {
        // Używamy Twojego rygorystycznego JSONa
        MealDbClient realClient = createClientReturningJson(lookupResponseWithFullIngredients());

        MealSearchResponse response = realClient.lookupById("52772").block();

        assertNotNull(response);
        Meal meal = response.meals().getFirst();
        assertEquals("52772", meal.idMeal());

        // Rygorystyczne sprawdzenie granic mapowania (1 i 20)
        assertEquals("Ingredient1", meal.strIngredient1());
        assertEquals("Ingredient20", meal.strIngredient20());
        assertEquals("Measure20", meal.strMeasure20());
    }

    @Test
    void listAllBy_shouldCallCorrectType() {
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(CommonListResponse.class)).thenReturn(Mono.just(new CommonListResponse(List.of())));

        mealDbClient.listAllBy("a").block();

        ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
        verify(requestHeadersUriSpec).uri(urlCaptor.capture());
        assertTrue(urlCaptor.getValue().contains("/list.php?a=list"));
    }

//    @Test
//    void searchByName_shouldRejectBlankInput() {
//        when(webClient.get()).thenReturn(requestHeadersUriSpec);
//        when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersSpec);
//        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
//
//        assertThrows(IllegalArgumentException.class, () -> mealDbClient.searchByName(" ").block());
//    }

    @Test
    void shouldWrapErrorInRuntimeException() {
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);

        when(responseSpec.bodyToMono(any(Class.class)))
                .thenReturn(Mono.error(new RuntimeException("boom")));

        RuntimeException thrown = assertThrows(RuntimeException.class,
                () -> mealDbClient.listFullCategories().block());

        assertTrue(thrown.getMessage().contains("Error calling TheMealDB API"),
                "Wiadomość błędu powinna zawierać prefix: Error calling TheMealDB API");
        assertTrue(thrown.getMessage().contains("boom"),
                "Wiadomość powinna zawierać oryginalny powód błędu: boom");
    }

    // --- Metody Pomocnicze ---

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