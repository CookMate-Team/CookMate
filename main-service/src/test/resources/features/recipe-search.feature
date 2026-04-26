Feature: Recipe discovery and search endpoints
  As an API client
  I want to search and lookup meals using the Discovery API
  So that I can consume TheMealDB-backed recipe data

  Scenario: Search by name returns 200 and meals array
    Given the meal search by name "Arrabiata" returns an empty response
    When I call GET "/api/v1/discovery/search" with query param "name" = "Arrabiata"
    Then the response status should be 200
    And the response should contain a meals array
    And meal db client should be called to search by name "Arrabiata"

  Scenario: Lookup by meal id returns 200 and meals array
    Given the meal lookup by id "52772" returns an empty response
    When I call GET "/api/v1/discovery/lookup/52772"
    Then the response status should be 200
    And the response should contain a meals array
    And meal db client should be called to lookup meal id "52772"

  Scenario: Filter by ingredient returns 200 and meals array
    When I call GET "/api/v1/discovery/filter/ingredient" with query param "i" = "chicken_breast"
    Then the response status should be 200
    And the response should contain a meals array

  Scenario: List categories returns 200
    When I call GET "/api/v1/discovery/categories"
    Then the response status should be 200

  Scenario: Search by name without required param returns 400
    When I call GET "/api/v1/discovery/search" without query params
    Then the response status should be 400

  Scenario: Lookup by meal id without path variable returns 404
    When I call GET "/api/v1/discovery/lookup/"
    Then the response status should be 404