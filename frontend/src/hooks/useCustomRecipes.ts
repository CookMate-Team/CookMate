import { useInfiniteQuery, useMutation, useQueryClient, useQuery } from '@tanstack/react-query';
import { fetchMyRecipes, createCustomRecipe, updateCustomRecipe, deleteCustomRecipe, fetchCustomRecipeDetailsRaw } from '../services/api';
import type { CustomRecipe, CustomRecipeListResponse } from '../services/api';
import { useAuth } from '../context/AuthContext';

export const useCustomRecipes = () => {
  const { isAuthenticated } = useAuth();
  return useInfiniteQuery<CustomRecipeListResponse, Error>({
    queryKey: ['custom-recipes'],
    queryFn: ({ pageParam = 0 }) => fetchMyRecipes(pageParam as number, 12),
    getNextPageParam: (lastPage) => {
      if (lastPage.pageNumber >= lastPage.totalPages - 1) return undefined;
      return lastPage.pageNumber + 1;
    },
    initialPageParam: 0,
    enabled: isAuthenticated,
  });
};

export const useCustomRecipeDetails = (id: string | undefined) => {
  const { isAuthenticated } = useAuth();
  return useQuery<CustomRecipe, Error>({
    queryKey: ['custom-recipe-raw-details', id],
    queryFn: () => fetchCustomRecipeDetailsRaw(id!),
    enabled: !!id && isAuthenticated,
  });
};

export const useCreateCustomRecipe = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (recipe: Omit<CustomRecipe, 'id' | 'createdAt' | 'userId' | 'isCustom'>) => createCustomRecipe(recipe),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['custom-recipes'] });
    },
  });
};

export const useUpdateCustomRecipe = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, recipe }: { id: string; recipe: Partial<CustomRecipe> }) => updateCustomRecipe(id, recipe),
    onSuccess: (data) => {
      queryClient.invalidateQueries({ queryKey: ['custom-recipes'] });
      queryClient.invalidateQueries({ queryKey: ['meal-details', data.id.toString()] });
      queryClient.invalidateQueries({ queryKey: ['custom-recipe-raw-details', data.id.toString()] });
    },
  });
};

export const useDeleteCustomRecipe = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => deleteCustomRecipe(id),
    onSuccess: (_, id) => {
      queryClient.invalidateQueries({ queryKey: ['custom-recipes'] });
      queryClient.invalidateQueries({ queryKey: ['meal-details', id] });
      queryClient.invalidateQueries({ queryKey: ['custom-recipe-raw-details', id] });
    },
  });
};
