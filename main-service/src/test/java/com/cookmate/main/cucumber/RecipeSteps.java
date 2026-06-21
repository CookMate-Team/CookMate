package com.cookmate.main.cucumber;

import com.cookmate.main.controller.RecipeController;
import com.cookmate.main.exception.GlobalExceptionHandler;
import com.cookmate.main.exception.RecipeNotFoundException;
import com.cookmate.main.model.Recipe;
import com.cookmate.main.service.RecipeService;
import com.cookmate.main.service.StepService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.cucumber.java.Before;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

public class RecipeSteps {
    private RecipeService recipeService;
    private StepService stepService;
    private MockMvc mockMvc;
    private MvcResult lastResult;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Before
    public void setUp() {
        recipeService = Mockito.mock(RecipeService.class);
        stepService = Mockito.mock(StepService.class);

        when(stepService.getStepsByRecipeId(any())).thenReturn(List.of());

        RecipeController controller = new RecipeController(recipeService, stepService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Given("the recipe repository contains {int} recipes")
    public void theRepositoryContainsRecipes(int count) {
        var recipes = java.util.stream.IntStream.range(0, count)
                .mapToObj(i -> new Recipe("Recipe " + i, "Description " + i, "Ingredients " + i, "Instructions " + i, 30))
                .toList();

        var recipeDtos = recipes.stream()
                .map(r -> new com.cookmate.main.dto.RecipeDTO(
                        (long) recipes.indexOf(r) + 1,
                        r.getName(), r.getDescription(),
                        r.getIngredients(), r.getInstructions(),
                        r.getPreparationTimeMinutes(), null, 4,
                        null, false, null, java.util.List.of()))
                .toList();

        when(recipeService.findPaginated(0, 10))
                .thenReturn(new com.cookmate.main.dto.RecipeListResponse(recipeDtos, 0, 10, count, 1));
    }

    @Given("a recipe with id {long} exists in the repository")
    public void aRecipeWithIdExists(long id) {
        var recipe = new Recipe("Test Recipe", "A description", "flour, eggs", "Mix and bake", 60);
        when(recipeService.findByIdOrThrow(id)).thenReturn(recipe);
    }

    @Given("the recipe repository is empty")
    public void theRepositoryIsEmpty() {
        when(recipeService.findByIdOrThrow(anyLong()))
                .thenThrow(new RecipeNotFoundException(99999L));
        doThrow(new RecipeNotFoundException(99999L))
                .when(recipeService).deleteByIdOrThrow(anyLong());
        when(recipeService.findPaginated(0, 10))
                .thenReturn(new com.cookmate.main.dto.RecipeListResponse(List.of(), 0, 10, 0, 0));
    }

    @When("I send GET {string}")
    public void iCallGet(String path) throws Exception {
        lastResult = mockMvc.perform(get(path)
                        .contentType(MediaType.APPLICATION_JSON))
                .andReturn();
    }

    @When("I call POST {string} with body:")
    public void iCallPostWithBody(String path, String body) throws Exception {
        if (!body.contains("\"name\": \"\"")) {
            var recipe = new Recipe("Spaghetti Bolognese", "Classic Italian pasta dish",
                    "spaghetti, minced meat, tomato sauce", "Cook pasta, prepare sauce, combine", 45);
            when(recipeService.save(any())).thenReturn(recipe);
        }

        lastResult = mockMvc.perform(post(path)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andReturn();
    }

    @When("I call DELETE {string}")
    public void iCallDelete(String path) throws Exception {
        lastResult = mockMvc.perform(delete(path)
                        .contentType(MediaType.APPLICATION_JSON))
                .andReturn();
    }

    @Then("the recipe response status should be {int}")
    public void theResponseStatusShouldBe(int expectedStatus) {
        assertThat(lastResult.getResponse().getStatus())
                .as("Expected HTTP status %d", expectedStatus)
                .isEqualTo(expectedStatus);
    }

    @And("the response should contain a recipes array")
    public void theResponseShouldContainARecipesArray() throws Exception {
        var body = lastResult.getResponse().getContentAsString();
        JsonNode json = objectMapper.readTree(body);
        assertThat(json.has("recipes"))
                .as("Response should have 'recipes' field")
                .isTrue();
        assertThat(json.get("recipes").isArray())
                .as("'recipes' should be an array")
                .isTrue();
    }

    @And("the recipe response should contain field {string}")
    public void theResponseShouldContainField(String fieldName) throws Exception {
        var body = lastResult.getResponse().getContentAsString();
        JsonNode json = objectMapper.readTree(body);
        assertThat(json.has(fieldName))
                .as("Response should contain field '%s'", fieldName)
                .isTrue();
    }

    @And("the response should contain error code {string}")
    public void theResponseShouldContainErrorCode(String expectedCode) throws Exception {
        var body = lastResult.getResponse().getContentAsString();
        JsonNode json = objectMapper.readTree(body);
        assertThat(json.has("code"))
                .as("Response should have 'code' field")
                .isTrue();
        assertThat(json.get("code").asText())
                .as("Error code should be '%s'", expectedCode)
                .isEqualTo(expectedCode);
    }
}
