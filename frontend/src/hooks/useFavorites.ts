import { useInfiniteQuery, useMutation, useQueryClient, useQuery } from '@tanstack/react-query';
import { fetchFavorites, addFavorite, removeFavorite, checkFavorite } from '../services/favoritesApi';
import type { FavoriteRecipeAddRequest, PaginatedFavorites } from '../services/favoritesApi';
import { useAuth } from '../context/AuthContext';
export const useFavorites = () => {
  const { isAuthenticated } = useAuth();
  return useInfiniteQuery<PaginatedFavorites, Error>({
    queryKey: ['favorites'],
    queryFn: ({ pageParam = 0 }) => fetchFavorites(pageParam as number, 12),
    getNextPageParam: (lastPage) => {
      if (lastPage.last) return undefined;
      return lastPage.number + 1;
    },
    initialPageParam: 0,
    enabled: isAuthenticated,
  });
};

export const useAddFavorite = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ recipeId, request }: { recipeId: string, request: FavoriteRecipeAddRequest }) => addFavorite(recipeId, request),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['favorites'] });
      queryClient.invalidateQueries({ queryKey: ['favoriteCheck'] });
    },
  });
};

export const useRemoveFavorite = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (recipeId: string) => removeFavorite(recipeId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['favorites'] });
      queryClient.invalidateQueries({ queryKey: ['favoriteCheck'] });
    },
  });
};

export const useCheckFavorite = (recipeId: string | null) => {
  const { isAuthenticated } = useAuth();
  return useQuery({
    queryKey: ['favoriteCheck', recipeId],
    queryFn: () => recipeId ? checkFavorite(recipeId) : Promise.resolve(false),
    enabled: !!recipeId && isAuthenticated,
    retry: false,
  });
};
