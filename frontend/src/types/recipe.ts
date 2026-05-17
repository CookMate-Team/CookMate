export interface Meal {
  idMeal: string;
  strMeal: string;
  strCategory: string;
  strArea: string;
  strMealThumb: string;
  strInstructions: string;
  strYoutube?: string;
  [key: string]: any; // for ingredients and other fields
}

export interface MealSearchResponse {
  meals: Meal[] | null;
}

export interface RecipeDTO {
  id: number;
  name: string;
  description: string;
  ingredients: string;
  instructions: string;
  preparationTimeMinutes: number;
  createdAt: string;
}

export interface RecipeListResponse {
  recipes: RecipeDTO[];
  totalCount: number;
  pageNumber: number;
  pageSize: number;
  totalPages: number;
}

export interface RecipeStep {
  id: number;
  stepNumber: number;
  description: string;
  action: string;
  parameters?: string;
  durationMinutes?: number;
  recipeId: string;
}
