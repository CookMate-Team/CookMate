package com.cookmate.main.integration;

import com.cookmate.main.controller.DiscoveryController;
import com.cookmate.main.service.MealDbClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.context.WebApplicationContext;
import reactor.core.publisher.Mono;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@Import(DiscoveryIntegrationTest.StubMealDbApiConfig.class)
class DiscoveryIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    @Autowired
    private DiscoveryController discoveryController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    void shouldReturnCorrectSearchResponseFromStub() throws Exception {
        MvcResult result = performRequest(
                get("/api/v1/discovery/search").param("name", "Apam")
        );

        assertEquals(200, result.getResponse().getStatus());
        String body = result.getResponse().getContentAsString();

        assertTrue(body.contains("\"idMeal\":\"53049\""));
        assertTrue(body.contains("\"strMeal\":\"Apam balik\""));
        assertTrue(body.contains("\"strArea\":\"Malaysian\""));
    }

    @Test
    void shouldReturnFullIngredientsResponseFromStub() throws Exception {
        MvcResult result = performRequest(
                get("/api/v1/discovery/lookup/52772")
        );

        assertEquals(200, result.getResponse().getStatus());
        String body = result.getResponse().getContentAsString();

        // Rygorystyczna weryfikacja pierwszego i ostatniego elementu z Twojego JSONa
        assertTrue(body.contains("\"idMeal\":\"52772\""));
        assertTrue(body.contains("\"strIngredient1\":\"Ingredient1\""));
        assertTrue(body.contains("\"strIngredient20\":\"Ingredient20\""));
        assertTrue(body.contains("\"strMeasure20\":\"Measure20\""));
    }

    private MvcResult performRequest(MockHttpServletRequestBuilder requestBuilder) throws Exception {
        MvcResult initial = mockMvc.perform(requestBuilder).andReturn();
        if (initial.getRequest().isAsyncStarted()) {
            return mockMvc.perform(asyncDispatch(initial)).andReturn();
        }
        return initial;
    }

    @TestConfiguration
    static class StubMealDbApiConfig {

        @Bean
        @Primary
        MealDbClient mealDbClient() {
            ExchangeFunction exchangeFunction = request -> {
                String url = request.url().toString();

                if (url.contains("/search.php?s=Apam")) {
                    return Mono.just(jsonResponse(searchResponseJson()));
                }
                if (url.contains("/lookup.php?i=52772")) {
                    return Mono.just(jsonResponse(lookupResponseWithFullIngredients()));
                }

                return Mono.just(ClientResponse.create(HttpStatus.NOT_FOUND).build());
            };

            WebClient stubWebClient = WebClient.builder()
                    .exchangeFunction(exchangeFunction)
                    .build();

            return new MealDbClient(stubWebClient);
        }

        private static ClientResponse jsonResponse(String body) {
            return ClientResponse.create(HttpStatus.OK)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .body(body)
                    .build();
        }

        private static String searchResponseJson() {
            return """
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
        }

        private static String lookupResponseWithFullIngredients() {
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
}