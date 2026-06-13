import { authFetch } from './authFetch';

const API_BASE_URL = '/api';

export interface FavoriteRecipeAddRequest {
  recipeTitle: string;
  imageUrl?: string;
}

export interface FavoriteRecipeDTO {
  id: number;
  recipeId: string;
  recipeTitle: string;
  imageUrl?: string;
  addedAt: string;
}

export interface PaginatedFavorites {
  content: FavoriteRecipeDTO[];
  pageable: any;
  last: boolean;
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  numberOfElements: number;
  empty: boolean;
}

export const fetchFavorites = async (page: number = 0, size: number = 10): Promise<PaginatedFavorites> => {
  const response = await authFetch(`${API_BASE_URL}/recipes/favorites?page=${page}&size=${size}`);
  if (!response.ok) {
    throw new Error('Failed to fetch favorites');
  }
  return response.json();
};

export const addFavorite = async (recipeId: string, request: FavoriteRecipeAddRequest): Promise<FavoriteRecipeDTO> => {
  const response = await authFetch(`${API_BASE_URL}/recipes/${recipeId}/favorite`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(request),
  });
  if (!response.ok) {
    throw new Error('Failed to add favorite');
  }
  return response.json();
};

export const removeFavorite = async (recipeId: string): Promise<void> => {
  const response = await authFetch(`${API_BASE_URL}/recipes/${recipeId}/favorite`, {
    method: 'DELETE',
  });
  if (!response.ok) {
    throw new Error('Failed to remove favorite');
  }
};

export const checkFavorite = async (recipeId: string): Promise<boolean> => {
  const response = await authFetch(`${API_BASE_URL}/recipes/${recipeId}/favorite/check`);
  if (!response.ok) {
    if (response.status === 401 || response.status === 403) return false;
    throw new Error('Failed to check favorite');
  }
  return response.json();
};
