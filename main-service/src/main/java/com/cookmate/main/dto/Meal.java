package com.cookmate.main.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Model representing a meal from TheMealDB API response.
 * Mapped from JSON response of search/lookup endpoints.
 */
public record Meal(
    /**
     * Unique meal ID from TheMealDB.
     */
    @JsonProperty("idMeal")
    String idMeal,

    /**
     * Meal name/title.
     */
    @JsonProperty("strMeal")
    String strMeal,

    /**
     * Alternate meal name.
     */
    @JsonProperty("strMealAlternate")
    String strMealAlternate,

    /**
     * Category of the meal.
     */
    @JsonProperty("strCategory")
    String strCategory,

    /**
     * Area/cuisine of the meal.
     */
    @JsonProperty("strArea")
    String strArea,

    /**
     * Instructions for preparing the meal.
     */
    @JsonProperty("strInstructions")
    String strInstructions,

    /**
     * URL to meal image.
     */
    @JsonProperty("strMealThumb")
    String strMealThumb,

    /**
     * Tags associated with the meal.
     */
    @JsonProperty("strTags")
    String strTags,

    /**
     * YouTube video URL for the meal.
     */
    @JsonProperty("strYoutube")
    String strYoutube,

    /**
     * Source URL for the meal.
     */
    @JsonProperty("strSource")
    String strSource,

    /**
     * Image source URL.
     */
    @JsonProperty("strImageSource")
    String strImageSource,

    /**
     * Creative Commons confirmation.
     */
    @JsonProperty("strCreativeCommonsConfirmed")
    String strCreativeCommonsConfirmed,

    /**
     * Date last modified.
     */
    @JsonProperty("dateModified")
    String dateModified,

    /**
     * Ingredient 1.
     */
    @JsonProperty("strIngredient1")
    String strIngredient1,

    /**
     * Measurement for ingredient 1.
     */
    @JsonProperty("strMeasure1")
    String strMeasure1,

    /**
     * Ingredient 2.
     */
    @JsonProperty("strIngredient2")
    String strIngredient2,

    /**
     * Measurement for ingredient 2.
     */
    @JsonProperty("strMeasure2")
    String strMeasure2,

    /**
     * Ingredient 3.
     */
    @JsonProperty("strIngredient3")
    String strIngredient3,

    /**
     * Measurement for ingredient 3.
     */
    @JsonProperty("strMeasure3")
    String strMeasure3,

    /**
     * Ingredient 4.
     */
    @JsonProperty("strIngredient4")
    String strIngredient4,

    /**
     * Measurement for ingredient 4.
     */
    @JsonProperty("strMeasure4")
    String strMeasure4,

    /**
     * Ingredient 5.
     */
    @JsonProperty("strIngredient5")
    String strIngredient5,

    /**
     * Measurement for ingredient 5.
     */
    @JsonProperty("strMeasure5")
    String strMeasure5,

    /**
     * Ingredient 6.
     */
    @JsonProperty("strIngredient6")
    String strIngredient6,

    /**
     * Measurement for ingredient 6.
     */
    @JsonProperty("strMeasure6")
    String strMeasure6,

    /**
     * Ingredient 7.
     */
    @JsonProperty("strIngredient7")
    String strIngredient7,

    /**
     * Measurement for ingredient 7.
     */
    @JsonProperty("strMeasure7")
    String strMeasure7,

    /**
     * Ingredient 8.
     */
    @JsonProperty("strIngredient8")
    String strIngredient8,

    /**
     * Measurement for ingredient 8.
     */
    @JsonProperty("strMeasure8")
    String strMeasure8,

    /**
     * Ingredient 9.
     */
    @JsonProperty("strIngredient9")
    String strIngredient9,

    /**
     * Measurement for ingredient 9.
     */
    @JsonProperty("strMeasure9")
    String strMeasure9,

    /**
     * Ingredient 10.
     */
    @JsonProperty("strIngredient10")
    String strIngredient10,

    /**
     * Measurement for ingredient 10.
     */
    @JsonProperty("strMeasure10")
    String strMeasure10,

    /**
     * Ingredient 11.
     */
    @JsonProperty("strIngredient11")
    String strIngredient11,

    /**
     * Measurement for ingredient 11.
     */
    @JsonProperty("strMeasure11")
    String strMeasure11,

    /**
     * Ingredient 12.
     */
    @JsonProperty("strIngredient12")
    String strIngredient12,

    /**
     * Measurement for ingredient 12.
     */
    @JsonProperty("strMeasure12")
    String strMeasure12,

    /**
     * Ingredient 13.
     */
    @JsonProperty("strIngredient13")
    String strIngredient13,

    /**
     * Measurement for ingredient 13.
     */
    @JsonProperty("strMeasure13")
    String strMeasure13,

    /**
     * Ingredient 14.
     */
    @JsonProperty("strIngredient14")
    String strIngredient14,

    /**
     * Measurement for ingredient 14.
     */
    @JsonProperty("strMeasure14")
    String strMeasure14,

    /**
     * Ingredient 15.
     */
    @JsonProperty("strIngredient15")
    String strIngredient15,

    /**
     * Measurement for ingredient 15.
     */
    @JsonProperty("strMeasure15")
    String strMeasure15,

    /**
     * Ingredient 16.
     */
    @JsonProperty("strIngredient16")
    String strIngredient16,

    /**
     * Measurement for ingredient 16.
     */
    @JsonProperty("strMeasure16")
    String strMeasure16,

    /**
     * Ingredient 17.
     */
    @JsonProperty("strIngredient17")
    String strIngredient17,

    /**
     * Measurement for ingredient 17.
     */
    @JsonProperty("strMeasure17")
    String strMeasure17,

    /**
     * Ingredient 18.
     */
    @JsonProperty("strIngredient18")
    String strIngredient18,

    /**
     * Measurement for ingredient 18.
     */
    @JsonProperty("strMeasure18")
    String strMeasure18,

    /**
     * Ingredient 19.
     */
    @JsonProperty("strIngredient19")
    String strIngredient19,

    /**
     * Measurement for ingredient 19.
     */
    @JsonProperty("strMeasure19")
    String strMeasure19,

    /**
     * Ingredient 20.
     */
    @JsonProperty("strIngredient20")
    String strIngredient20,

    /**
     * Measurement for ingredient 20.
     */
    @JsonProperty("strMeasure20")
    String strMeasure20
) {}
