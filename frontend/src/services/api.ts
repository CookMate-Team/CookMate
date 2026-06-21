import type { Meal, MealSearchResponse, RecipeStep } from '../types/recipe';
import { authFetch } from './authFetch';

const API_BASE_URL = '/api';

export const fetchDiscoveryRecipes = async (query: string): Promise<MealSearchResponse> => {
  const response = await authFetch(`${API_BASE_URL}/v1/discovery/search?name=${encodeURIComponent(query)}`);
  if (!response.ok) {
    throw new Error('Network response was not ok');
  }
  return response.json();
};

export const fetchMealDetails = async (id: string): Promise<MealSearchResponse> => {
  const isDiscovery = /^\d{5}$/.test(id);
  if (isDiscovery) {
    const response = await authFetch(`${API_BASE_URL}/v1/discovery/lookup/${id}`);
    if (response.ok) {
      return response.json();
    }
  }
  return fetchCustomRecipeDetails(id);
};

export const fetchRecipeSteps = async (recipeId: string): Promise<RecipeStep[]> => {
  const response = await authFetch(`${API_BASE_URL}/recipes/${recipeId}/steps`);
  if (!response.ok) {
    if (response.status === 404) {
      return [];
    }
    throw new Error('Network response was not ok');
  }
  return response.json();
};

export interface CustomRecipe {
  id: number;
  name: string;
  description: string;
  ingredients: string;
  instructions: string;
  preparationTimeMinutes: number;
  defaultPortions?: number;
  createdAt?: string;
  userId?: string;
  isCustom?: boolean;
  custom?: boolean;
  imageUrl?: string;
}

export interface CustomRecipeListResponse {
  recipes: CustomRecipe[];
  totalCount: number;
  pageNumber: number;
  pageSize: number;
  totalPages: number;
}

export const fetchMyRecipes = async (page: number = 0, size: number = 10): Promise<CustomRecipeListResponse> => {
  const response = await authFetch(`${API_BASE_URL}/recipes/my?page=${page}&size=${size}`);
  if (!response.ok) {
    throw new Error('Failed to fetch custom recipes');
  }
  return response.json();
};

export const fetchCustomRecipeDetailsRaw = async (id: string): Promise<CustomRecipe> => {
  const response = await authFetch(`${API_BASE_URL}/recipes/${id}`);
  if (!response.ok) {
    throw new Error('Failed to fetch custom recipe details');
  }
  return response.json();
};

export const createCustomRecipe = async (recipe: Omit<CustomRecipe, 'id' | 'createdAt' | 'userId' | 'isCustom'>): Promise<CustomRecipe> => {
  const response = await authFetch(`${API_BASE_URL}/recipes`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(recipe),
  });
  if (!response.ok) {
    throw new Error('Failed to create recipe');
  }
  return response.json();
};

export const updateCustomRecipe = async (id: string, recipe: Partial<CustomRecipe>): Promise<CustomRecipe> => {
  const response = await authFetch(`${API_BASE_URL}/recipes/${id}`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(recipe),
  });
  if (!response.ok) {
    throw new Error('Failed to update recipe');
  }
  return response.json();
};

export const deleteCustomRecipe = async (id: string): Promise<void> => {
  const response = await authFetch(`${API_BASE_URL}/recipes/${id}`, {
    method: 'DELETE',
  });
  if (!response.ok) {
    throw new Error('Failed to delete recipe');
  }
};

// Helper to parse ingredients string from "flour (200g)\nsugar (100g)" to list
function parseIngredientsString(ingredientsStr: string) {
  if (!ingredientsStr) return [];
  const lines = ingredientsStr.split(/[\n,]+/);
  return lines
    .map(line => {
      const trimmed = line.trim();
      if (!trimmed) return null;
      const match = trimmed.match(/^([^(]+)(?:\(([^)]+)\))?$/);
      if (match) {
        return {
          name: match[1].trim(),
          measure: match[2] ? match[2].trim() : 'to taste'
        };
      }
      return {
        name: trimmed,
        measure: 'to taste'
      };
    })
    .filter(Boolean) as Array<{ name: string; measure: string }>;
}

// Map custom recipe structure to fake TheMealDB format for ExpandedRecipeCard
export function mapRecipeToMealResponse(recipe: CustomRecipe) {
  const meal: Meal = {
    idMeal: recipe.id.toString(),
    strMeal: recipe.name,
    strCategory: (recipe.isCustom || recipe.custom) ? 'Custom' : 'Local',
    strArea: 'My Kitchen',
    strInstructions: recipe.instructions,
    strMealThumb: recipe.imageUrl || 'https://images.unsplash.com/photo-1546069901-ba9599a7e63c?ixlib=rb-4.0.3&auto=format&fit=crop&w=500&q=60', // Placeholder or custom image
    strTags: null,
    strYoutube: undefined,
    preparationTimeMinutes: recipe.preparationTimeMinutes,
    defaultPortions: recipe.defaultPortions || 4,
  };

  const ingredientsList = parseIngredientsString(recipe.ingredients);
  for (let i = 0; i < 20; i++) {
    if (i < ingredientsList.length) {
      meal[`strIngredient${i + 1}`] = ingredientsList[i].name;
      meal[`strMeasure${i + 1}`] = ingredientsList[i].measure;
    } else {
      meal[`strIngredient${i + 1}`] = '';
      meal[`strMeasure${i + 1}`] = '';
    }
  }

  return { meals: [meal] };
}

export const fetchCustomRecipeDetails = async (id: string): Promise<MealSearchResponse> => {
  const recipe = await fetchCustomRecipeDetailsRaw(id);
  return mapRecipeToMealResponse(recipe);
};

