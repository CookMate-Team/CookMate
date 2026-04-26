package com.cookmate.main.cucumber;

import com.cookmate.main.controller.DiscoveryController;
import com.cookmate.main.dto.CategoryResponse;
import com.cookmate.main.dto.CommonListResponse;
import com.cookmate.main.dto.MealSearchResponse;
import com.cookmate.main.service.MealDbClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.cucumber.java.Before;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.mockito.Mockito;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import reactor.core.publisher.Mono;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

public class DiscoverySteps {

    private MealDbClient mealDbClient;
    private MockMvc mockMvc;
    private MvcResult lastResult;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Before
    public void setUp() {
        mealDbClient = Mockito.mock(MealDbClient.class);

        // Domyślne zachowania dla endpointów pomocniczych, aby uniknąć NPE/AssertionError
        when(mealDbClient.listFullCategories())
                .thenReturn(Mono.just(new CategoryResponse(List.of())));
        when(mealDbClient.listAllBy(anyString()))
                .thenReturn(Mono.just(new CommonListResponse(List.of())));
        when(mealDbClient.filterByIngredient(anyString()))
                .thenReturn(Mono.just(new MealSearchResponse(List.of())));

        DiscoveryController controller = new DiscoveryController(mealDbClient);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Given("the meal search by name {string} returns an empty response")
    public void theMealSearchByNameReturnsAnEmptyResponse(String name) {
        // Zwracamy pustą listę - nie musimy tworzyć obiektu Meal z 53 argumentami
        when(mealDbClient.searchByName(name))
                .thenReturn(Mono.just(new MealSearchResponse(List.of())));
    }

    @Given("the meal lookup by id {string} returns an empty response")
    public void theMealLookupByIdReturnsAnEmptyResponse(String mealId) {
        when(mealDbClient.lookupById(mealId))
                .thenReturn(Mono.just(new MealSearchResponse(List.of())));
    }

    @Given("the meal filter by ingredient {string} returns a valid response")
    public void theMealFilterReturnsResponse(String ingredient) {
        when(mealDbClient.filterByIngredient(ingredient))
                .thenReturn(Mono.just(new MealSearchResponse(List.of())));
    }

    @When("I call GET {string} with query param {string} = {string}")
    public void iCallGetWithQueryParam(String path, String key, String value) throws Exception {
        MockHttpServletRequestBuilder requestBuilder = get(path).param(key, value);
        lastResult = performRequest(requestBuilder);
    }

    @When("I call GET {string}")
    public void iCallGetWithPath(String path) throws Exception {
        lastResult = performRequest(get(path));
    }

    @When("I call GET {string} without query params")
    public void iCallGetWithoutQueryParams(String path) throws Exception {
        lastResult = performRequest(get(path));
    }

    @Then("the response status should be {int}")
    public void theResponseStatusShouldBe(int statusCode) {
        assertEquals(statusCode, lastResult.getResponse().getStatus());
    }

    @And("the response should contain a meals array")
    public void theResponseShouldContainAMealsArray() throws Exception {
        String body = lastResult.getResponse().getContentAsString();
        JsonNode responseJson = objectMapper.readTree(body);
        // Sprawdzamy czy pole "meals" istnieje w JSON - to naprawia błąd z logów
        assertTrue(responseJson.has("meals"), "Response should have 'meals' field");
        assertTrue(responseJson.get("meals").isArray(), "'meals' should be an array");
    }

    @And("meal db client should be called to search by name {string}")
    public void mealDbClientShouldBeCalledToSearchByName(String name) {
        verify(mealDbClient).searchByName(name);
    }

    @And("meal db client should be called to lookup meal id {string}")
    public void mealDbClientShouldBeCalledToLookupMealId(String mealId) {
        verify(mealDbClient).lookupById(mealId);
    }

    private MvcResult performRequest(MockHttpServletRequestBuilder requestBuilder) throws Exception {
        MvcResult initial = mockMvc.perform(requestBuilder).andReturn();
        if (initial.getRequest().isAsyncStarted()) {
            return mockMvc.perform(asyncDispatch(initial)).andReturn();
        }
        return initial;
    }
}