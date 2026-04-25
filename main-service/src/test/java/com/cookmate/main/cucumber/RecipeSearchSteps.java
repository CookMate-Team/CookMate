package com.cookmate.main.cucumber;

import com.cookmate.main.controller.RecipeSearchController;
import com.cookmate.main.dto.MealSearchResponse;
import com.cookmate.main.service.RecipeService;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

public class RecipeSearchSteps {

    private RecipeService recipeService;
    private MockMvc mockMvc;
    private MvcResult lastResult;

    @Before
    public void setUp() {
        recipeService = Mockito.mock(RecipeService.class);
        RecipeSearchController controller = new RecipeSearchController(recipeService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Given("the meal search by letter {string} returns an empty response")
    public void theMealSearchByLetterReturnsAnEmptyResponse(String letter) {
        when(recipeService.searchMealsByLetter(letter))
            .thenReturn(Mono.just(new MealSearchResponse(List.of())));
    }

    @Given("the meal lookup by id {string} returns an empty response")
    public void theMealLookupByIdReturnsAnEmptyResponse(String mealId) {
        when(recipeService.lookupMeal(mealId))
            .thenReturn(Mono.just(new MealSearchResponse(List.of())));
    }

    @When("I call GET {string} with query param {string} = {string}")
    public void iCallGetWithQueryParam(String path, String key, String value) throws Exception {
        MockHttpServletRequestBuilder requestBuilder = get(path).param(key, value);
        lastResult = performRequest(requestBuilder);
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
        assertTrue(body.contains("\"meals\":"));
    }

    @And("recipe service should be called to search by letter {string}")
    public void recipeServiceShouldBeCalledToSearchByLetter(String letter) {
        verify(recipeService).searchMealsByLetter(letter);
    }

    @And("recipe service should be called to lookup meal id {string}")
    public void recipeServiceShouldBeCalledToLookupMealId(String mealId) {
        verify(recipeService).lookupMeal(mealId);
    }

    private MvcResult performRequest(MockHttpServletRequestBuilder requestBuilder) throws Exception {
        MvcResult initial = mockMvc.perform(requestBuilder).andReturn();
        if (initial.getRequest().isAsyncStarted()) {
            return mockMvc.perform(asyncDispatch(initial)).andReturn();
        }
        return initial;
    }
}

