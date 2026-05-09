import type { MealSearchResponse } from '../types/recipe';

const API_BASE_URL = '/api';

export const fetchDiscoveryRecipes = async (query: string): Promise<MealSearchResponse> => {
  const response = await fetch(`${API_BASE_URL}/v1/discovery/search?name=${encodeURIComponent(query)}`);
  if (!response.ok) {
    throw new Error('Network response was not ok');
  }
  return response.json();
};

export const fetchMealDetails = async (id: string): Promise<MealSearchResponse> => {
  const response = await fetch(`${API_BASE_URL}/v1/discovery/lookup/${id}`);
  if (!response.ok) {
    throw new Error('Network response was not ok');
  }
  return response.json();
};
