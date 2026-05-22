Feature: Recipe management endpoints
  As an API client
  I want to create, retrieve and delete recipes
  So that I can manage the recipe collection in CookMate

  Scenario: Get all recipes returns 200 and recipes array
    Given the recipe repository contains 2 recipes
    When I call GET "/api/recipes"
    Then the response status should be 200
    And the response should contain a recipes array

  Scenario: Get recipe by existing ID returns 200
    Given a recipe with id 1 exists in the repository
    When I call GET "/api/recipes/1"
    Then the response status should be 200
    And the response should contain field "name"

  Scenario: Get recipe by non-existing ID returns 404
    Given the recipe repository is empty
    When I call GET "/api/recipes/99999"
    Then the response status should be 404
    And the response should contain error code "RECIPE_NOT_FOUND"

  Scenario: Create a valid recipe returns 201
    When I call POST "/api/recipes" with body:
      """
      {
        "name": "Spaghetti Bolognese",
        "description": "Classic Italian pasta dish",
        "ingredients": "spaghetti, minced meat, tomato sauce",
        "instructions": "Cook pasta, prepare sauce, combine",
        "preparationTimeMinutes": 45
      }
      """
    Then the response status should be 201
    And the response should contain field "name"

  Scenario: Create recipe with blank name returns 400
    When I call POST "/api/recipes" with body:
      """
      {
        "name": "",
        "ingredients": "some ingredients",
        "preparationTimeMinutes": 10
      }
      """
    Then the response status should be 400

  Scenario: Delete non-existing recipe returns 404
    Given the recipe repository is empty
    When I call DELETE "/api/recipes/99999"
    Then the response status should be 404
    And the response should contain error code "RECIPE_NOT_FOUND"
