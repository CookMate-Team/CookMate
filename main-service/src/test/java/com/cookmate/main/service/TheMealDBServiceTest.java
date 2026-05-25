package com.cookmate.main.service;

import com.cookmate.main.dto.Meal;
import com.cookmate.main.dto.MealSearchResponse;
import com.cookmate.main.dto.CommonListResponse;
import com.cookmate.main.dto.CategoryResponse;
import com.cookmate.main.exception.ExternalServiceException;
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

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link MealDbClient} (TheMealDB service client).
 *
 * <p>Two testing strategies are used:
 * <ul>
 *   <li><b>Mockito mocks</b> of the WebClient fluent API – for verifying the correct
 *       URL is constructed and that the error-wrapping behaviour works.</li>
 *   <li><b>Real {@code ExchangeFunction} stubs</b> – for end-to-end JSON deserialization
 *       assertions without hitting the network.</li>
 * </ul>
 * </p>
 */
@ExtendWith(MockitoExtension.class)
class TheMealDBServiceTest {

    // --- Mockito mocks of WebClient fluent API ---
    @Mock private WebClient webClient;
    @SuppressWarnings("rawtypes")
    @Mock private WebClient.RequestHeadersUriSpec requestHeadersUriSpec;
    @SuppressWarnings("rawtypes")
    @Mock private WebClient.RequestHeadersSpec requestHeadersSpec;
    @Mock private WebClient.ResponseSpec responseSpec;

    private MealDbClient mealDbClient;

    @BeforeEach
    void setUp() {
        mealDbClient = new MealDbClient(webClient);
    }

    // -----------------------------------------------------------------------
    // getMealById (lookupById) – success
    // -----------------------------------------------------------------------

    @Test
    void lookupById_shouldReturnMealOnSuccess() {
        MealDbClient realClient = createClientReturningJson(mealLookupJson());

        MealSearchResponse response = realClient.lookupById("52772").block();

        assertThat(response).isNotNull();
        assertThat(response.meals()).hasSize(1);
        Meal meal = response.meals().get(0);
        assertThat(meal.idMeal()).isEqualTo("52772");
        assertThat(meal.strMeal()).isEqualTo("Teriyaki Chicken Casserole");
        assertThat(meal.strInstructions()).isEqualTo("Mix soy sauce and marinate the chicken.");
        assertThat(meal.strIngredient1()).isEqualTo("Soy Sauce");
        assertThat(meal.strMeasure1()).isEqualTo("3 tablespoons");
    }

    @Test
    void lookupById_shouldCallCorrectApiUrl() {
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(MealSearchResponse.class))
                .thenReturn(Mono.just(new MealSearchResponse(List.of())));

        mealDbClient.lookupById("52772").block();

        ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
        verify(requestHeadersUriSpec).uri(urlCaptor.capture());
        assertThat(urlCaptor.getValue()).contains("/lookup.php?i=52772");
    }

    // -----------------------------------------------------------------------
    // getMealById – not found (null meals list)
    // -----------------------------------------------------------------------

    @Test
    void lookupById_shouldReturnNullMealsListWhenNotFound() {
        // TheMealDB returns {"meals": null} when no meal is found
        MealDbClient realClient = createClientReturningJson("{\"meals\": null}");

        MealSearchResponse response = realClient.lookupById("99999").block();

        assertThat(response).isNotNull();
        assertThat(response.meals()).isNull();
    }

    // -----------------------------------------------------------------------
    // searchByName – success
    // -----------------------------------------------------------------------

    @Test
    void searchByName_shouldCallCorrectApiUrl() {
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(MealSearchResponse.class))
                .thenReturn(Mono.just(new MealSearchResponse(List.of())));

        mealDbClient.searchByName("Chicken").block();

        ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
        verify(requestHeadersUriSpec).uri(urlCaptor.capture());
        assertThat(urlCaptor.getValue()).contains("/search.php?s=Chicken");
    }

    @Test
    void searchByName_shouldReturnMealList() {
        MealDbClient realClient = createClientReturningJson(searchResponseJson());

        MealSearchResponse response = realClient.searchByName("Apam").block();

        assertThat(response).isNotNull();
        assertThat(response.meals()).hasSize(1);
        assertThat(response.meals().get(0).strMeal()).isEqualTo("Apam balik");
        assertThat(response.meals().get(0).strArea()).isEqualTo("Malaysian");
    }

    // -----------------------------------------------------------------------
    // filterByIngredient
    // -----------------------------------------------------------------------

    @Test
    void filterByIngredient_shouldCallCorrectApiUrl() {
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(MealSearchResponse.class))
                .thenReturn(Mono.just(new MealSearchResponse(List.of())));

        mealDbClient.filterByIngredient("chicken_breast").block();

        ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
        verify(requestHeadersUriSpec).uri(urlCaptor.capture());
        assertThat(urlCaptor.getValue()).contains("/filter.php?i=chicken_breast");
    }

    // -----------------------------------------------------------------------
    // listAllBy
    // -----------------------------------------------------------------------

    @Test
    void listAllBy_shouldCallCorrectApiUrlForCategories() {
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(CommonListResponse.class))
                .thenReturn(Mono.just(new CommonListResponse(List.of())));

        mealDbClient.listAllBy("c").block();

        ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
        verify(requestHeadersUriSpec).uri(urlCaptor.capture());
        assertThat(urlCaptor.getValue()).contains("/list.php?c=list");
    }

    // -----------------------------------------------------------------------
    // API error handling – wraps exception in ExternalServiceException
    // -----------------------------------------------------------------------

    @Test
    void lookupById_shouldWrapNetworkErrorInExternalServiceException() {
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(MealSearchResponse.class))
                .thenReturn(Mono.error(new RuntimeException("Connection refused")));

        assertThatThrownBy(() -> mealDbClient.lookupById("52772").block())
                .isInstanceOf(ExternalServiceException.class)
                .hasMessageContaining("TheMealDB");
    }

    @Test
    void lookupById_shouldPreserveOriginalCauseInExternalServiceException() {
        RuntimeException root = new RuntimeException("socket timeout");

        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(MealSearchResponse.class))
                .thenReturn(Mono.error(root));

        ExternalServiceException thrown = (ExternalServiceException) catchThrowable(
                () -> mealDbClient.lookupById("52772").block());

        assertThat(thrown).isNotNull();
        assertThat(thrown.getCause()).isEqualTo(root);
        assertThat(thrown.getMessage()).contains("TheMealDB");
    }

    @Test
    void listFullCategories_shouldWrapErrorInExternalServiceException() {
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(CategoryResponse.class))
                .thenReturn(Mono.error(new RuntimeException("503")));

        assertThatThrownBy(() -> mealDbClient.listFullCategories().block())
                .isInstanceOf(ExternalServiceException.class)
                .hasMessageContaining("TheMealDB");
    }

    // -----------------------------------------------------------------------
    // Full ingredient mapping (boundary test – fields 1 and 20)
    // -----------------------------------------------------------------------

    @Test
    void lookupById_shouldMapAllTwentyIngredientAndMeasureFields() {
        MealDbClient realClient = createClientReturningJson(fullIngredientJson());

        MealSearchResponse response = realClient.lookupById("52772").block();

        assertThat(response).isNotNull();
        Meal meal = response.meals().get(0);
        assertThat(meal.strIngredient1()).isEqualTo("Ingredient1");
        assertThat(meal.strMeasure1()).isEqualTo("Measure1");
        assertThat(meal.strIngredient20()).isEqualTo("Ingredient20");
        assertThat(meal.strMeasure20()).isEqualTo("Measure20");
    }

    // -----------------------------------------------------------------------
    // Helper – creates a real MealDbClient backed by an ExchangeFunction stub
    // -----------------------------------------------------------------------

    private MealDbClient createClientReturningJson(String body) {
        ExchangeFunction exchangeFunction = request -> Mono.just(
                ClientResponse.create(HttpStatus.OK)
                        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .body(body)
                        .build()
        );
        WebClient stubWebClient = WebClient.builder().exchangeFunction(exchangeFunction).build();
        return new MealDbClient(stubWebClient);
    }

    // -----------------------------------------------------------------------
    // JSON fixtures
    // -----------------------------------------------------------------------

    private String mealLookupJson() {
        return """
            {
              "meals": [
                {
                  "idMeal": "52772",
                  "strMeal": "Teriyaki Chicken Casserole",
                  "strCategory": "Chicken",
                  "strArea": "Japanese",
                  "strInstructions": "Mix soy sauce and marinate the chicken.",
                  "strIngredient1": "Soy Sauce",
                  "strMeasure1": "3 tablespoons"
                }
              ]
            }
            """;
    }

    private String searchResponseJson() {
        return """
            {
              "meals": [
                {
                  "idMeal": "53049",
                  "strMeal": "Apam balik",
                  "strCategory": "Dessert",
                  "strArea": "Malaysian",
                  "strInstructions": "Mix all ingredients and cook.",
                  "strIngredient1": "Milk",
                  "strMeasure1": "200ml"
                }
              ]
            }
            """;
    }

    private String fullIngredientJson() {
        return """
            {
              "meals": [
                {
                  "idMeal": "52772",
                  "strMeal": "Teriyaki Chicken Casserole",
                  "strCategory": "Chicken",
                  "strArea": "Japanese",
                  "strInstructions": "Cook instructions",
                  "strIngredient1": "Ingredient1",  "strMeasure1": "Measure1",
                  "strIngredient2": "Ingredient2",  "strMeasure2": "Measure2",
                  "strIngredient3": "Ingredient3",  "strMeasure3": "Measure3",
                  "strIngredient4": "Ingredient4",  "strMeasure4": "Measure4",
                  "strIngredient5": "Ingredient5",  "strMeasure5": "Measure5",
                  "strIngredient6": "Ingredient6",  "strMeasure6": "Measure6",
                  "strIngredient7": "Ingredient7",  "strMeasure7": "Measure7",
                  "strIngredient8": "Ingredient8",  "strMeasure8": "Measure8",
                  "strIngredient9": "Ingredient9",  "strMeasure9": "Measure9",
                  "strIngredient10": "Ingredient10","strMeasure10": "Measure10",
                  "strIngredient11": "Ingredient11","strMeasure11": "Measure11",
                  "strIngredient12": "Ingredient12","strMeasure12": "Measure12",
                  "strIngredient13": "Ingredient13","strMeasure13": "Measure13",
                  "strIngredient14": "Ingredient14","strMeasure14": "Measure14",
                  "strIngredient15": "Ingredient15","strMeasure15": "Measure15",
                  "strIngredient16": "Ingredient16","strMeasure16": "Measure16",
                  "strIngredient17": "Ingredient17","strMeasure17": "Measure17",
                  "strIngredient18": "Ingredient18","strMeasure18": "Measure18",
                  "strIngredient19": "Ingredient19","strMeasure19": "Measure19",
                  "strIngredient20": "Ingredient20","strMeasure20": "Measure20"
                }
              ]
            }
            """;
    }
}
