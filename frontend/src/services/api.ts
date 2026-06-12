import type { MealSearchResponse, RecipeStep } from '../types/recipe';
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
  const response = await authFetch(`${API_BASE_URL}/v1/discovery/lookup/${id}`);
  if (!response.ok) {
    throw new Error('Network response was not ok');
  }
  return response.json();
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

