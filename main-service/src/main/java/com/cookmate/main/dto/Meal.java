package com.cookmate.main.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Model representing a meal from TheMealDB API response.
 * Mapped from JSON response of search/lookup endpoints.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class Meal {
    /**
     * Unique meal ID from TheMealDB.
     */
    @JsonProperty("idMeal")
    private String idMeal;

    /**
     * Meal name/title.
     */
    @JsonProperty("strMeal")
    private String strMeal;

    /**
     * Alternate meal name.
     */
    @JsonProperty("strMealAlternate")
    private String strMealAlternate;

    /**
     * Category of the meal.
     */
    @JsonProperty("strCategory")
    private String strCategory;

    /**
     * Area/cuisine of the meal.
     */
    @JsonProperty("strArea")
    private String strArea;

    /**
     * Instructions for preparing the meal.
     */
    @JsonProperty("strInstructions")
    private String strInstructions;

    /**
     * URL to meal image.
     */
    @JsonProperty("strMealThumb")
    private String strMealThumb;

    /**
     * Tags associated with the meal.
     */
    @JsonProperty("strTags")
    private String strTags;

    /**
     * YouTube video URL for the meal.
     */
    @JsonProperty("strYoutube")
    private String strYoutube;

    /**
     * Source URL for the meal.
     */
    @JsonProperty("strSource")
    private String strSource;

    /**
     * Image source URL.
     */
    @JsonProperty("strImageSource")
    private String strImageSource;

    /**
     * Creative Commons confirmation.
     */
    @JsonProperty("strCreativeCommonsConfirmed")
    private String strCreativeCommonsConfirmed;

    /**
     * Date last modified.
     */
    @JsonProperty("dateModified")
    private String dateModified;

    /**
     * Ingredient 1.
     */
    @JsonProperty("strIngredient1")
    private String strIngredient1;

    /**
     * Measurement for ingredient 1.
     */
    @JsonProperty("strMeasure1")
    private String strMeasure1;

    /**
     * Ingredient 2.
     */
    @JsonProperty("strIngredient2")
    private String strIngredient2;

    /**
     * Measurement for ingredient 2.
     */
    @JsonProperty("strMeasure2")
    private String strMeasure2;

    /**
     * Ingredient 3.
     */
    @JsonProperty("strIngredient3")
    private String strIngredient3;

    /**
     * Measurement for ingredient 3.
     */
    @JsonProperty("strMeasure3")
    private String strMeasure3;

    /**
     * Ingredient 4.
     */
    @JsonProperty("strIngredient4")
    private String strIngredient4;

    /**
     * Measurement for ingredient 4.
     */
    @JsonProperty("strMeasure4")
    private String strMeasure4;

    /**
     * Ingredient 5.
     */
    @JsonProperty("strIngredient5")
    private String strIngredient5;

    /**
     * Measurement for ingredient 5.
     */
    @JsonProperty("strMeasure5")
    private String strMeasure5;

    /**
     * Ingredient 6.
     */
    @JsonProperty("strIngredient6")
    private String strIngredient6;

    /**
     * Measurement for ingredient 6.
     */
    @JsonProperty("strMeasure6")
    private String strMeasure6;

    /**
     * Ingredient 7.
     */
    @JsonProperty("strIngredient7")
    private String strIngredient7;

    /**
     * Measurement for ingredient 7.
     */
    @JsonProperty("strMeasure7")
    private String strMeasure7;

    /**
     * Ingredient 8.
     */
    @JsonProperty("strIngredient8")
    private String strIngredient8;

    /**
     * Measurement for ingredient 8.
     */
    @JsonProperty("strMeasure8")
    private String strMeasure8;

    /**
     * Ingredient 9.
     */
    @JsonProperty("strIngredient9")
    private String strIngredient9;

    /**
     * Measurement for ingredient 9.
     */
    @JsonProperty("strMeasure9")
    private String strMeasure9;

    /**
     * Ingredient 10.
     */
    @JsonProperty("strIngredient10")
    private String strIngredient10;

    /**
     * Measurement for ingredient 10.
     */
    @JsonProperty("strMeasure10")
    private String strMeasure10;

    /**
     * Ingredient 11.
     */
    @JsonProperty("strIngredient11")
    private String strIngredient11;

    /**
     * Measurement for ingredient 11.
     */
    @JsonProperty("strMeasure11")
    private String strMeasure11;

    /**
     * Ingredient 12.
     */
    @JsonProperty("strIngredient12")
    private String strIngredient12;

    /**
     * Measurement for ingredient 12.
     */
    @JsonProperty("strMeasure12")
    private String strMeasure12;

    /**
     * Ingredient 13.
     */
    @JsonProperty("strIngredient13")
    private String strIngredient13;

    /**
     * Measurement for ingredient 13.
     */
    @JsonProperty("strMeasure13")
    private String strMeasure13;

    /**
     * Ingredient 14.
     */
    @JsonProperty("strIngredient14")
    private String strIngredient14;

    /**
     * Measurement for ingredient 14.
     */
    @JsonProperty("strMeasure14")
    private String strMeasure14;

    /**
     * Ingredient 15.
     */
    @JsonProperty("strIngredient15")
    private String strIngredient15;

    /**
     * Measurement for ingredient 15.
     */
    @JsonProperty("strMeasure15")
    private String strMeasure15;

    /**
     * Ingredient 16.
     */
    @JsonProperty("strIngredient16")
    private String strIngredient16;

    /**
     * Measurement for ingredient 16.
     */
    @JsonProperty("strMeasure16")
    private String strMeasure16;

    /**
     * Ingredient 17.
     */
    @JsonProperty("strIngredient17")
    private String strIngredient17;

    /**
     * Measurement for ingredient 17.
     */
    @JsonProperty("strMeasure17")
    private String strMeasure17;

    /**
     * Ingredient 18.
     */
    @JsonProperty("strIngredient18")
    private String strIngredient18;

    /**
     * Measurement for ingredient 18.
     */
    @JsonProperty("strMeasure18")
    private String strMeasure18;

    /**
     * Ingredient 19.
     */
    @JsonProperty("strIngredient19")
    private String strIngredient19;

    /**
     * Measurement for ingredient 19.
     */
    @JsonProperty("strMeasure19")
    private String strMeasure19;

    /**
     * Ingredient 20.
     */
    @JsonProperty("strIngredient20")
    private String strIngredient20;

    /**
     * Measurement for ingredient 20.
     */
    @JsonProperty("strMeasure20")
    private String strMeasure20;

    /**
     * Calculated execution time based on LLM steps.
     * Populated by backend before returning to frontend.
     */
    @JsonProperty("preparationTimeMinutes")
    private Integer preparationTimeMinutes;
}
