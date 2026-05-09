import { useQuery } from '@tanstack/react-query';
import { fetchDiscoveryRecipes } from '../services/api';

export const useDiscoveryRecipes = (query: string) => {
  return useQuery({
    queryKey: ['discovery-recipes', query],
    queryFn: () => fetchDiscoveryRecipes(query),
    enabled: !!query.trim(),
  });
};
