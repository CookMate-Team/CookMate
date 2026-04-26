Feature: Recipe search endpoints
  As an API client
  I want to search and lookup meals
  So that I can consume TheMealDB-backed recipe data

  Scenario: Search by letter returns 200 and meals array
    Given the meal search by letter "a" returns an empty response
    When I call GET "/api/recipes/search/themealdb/letter" with query param "letter" = "a"
    Then the response status should be 200
    And the response should contain a meals array
    And recipe service should be called to search by letter "a"

  Scenario: Lookup by meal id returns 200 and meals array
    Given the meal lookup by id "52772" returns an empty response
    When I call GET "/api/recipes/search/themealdb/meal" with query param "mealId" = "52772"
    Then the response status should be 200
    And the response should contain a meals array
    And recipe service should be called to lookup meal id "52772"

  Scenario: Search by letter without required param returns 400
    When I call GET "/api/recipes/search/themealdb/letter" without query params
    Then the response status should be 400

  Scenario: Lookup by meal id without required param returns 400
    When I call GET "/api/recipes/search/themealdb/meal" without query params
    Then the response status should be 400

