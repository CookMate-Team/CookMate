import { useQuery } from '@tanstack/react-query';
import { fetchRecipeSteps } from '../services/api';

export const useRecipeSteps = (recipeId: string | null, isAuthenticated = false) => {
  return useQuery({
    queryKey: ['recipe-steps', recipeId],
    queryFn: () => fetchRecipeSteps(recipeId!),
    enabled: !!recipeId && isAuthenticated,
  });
};
