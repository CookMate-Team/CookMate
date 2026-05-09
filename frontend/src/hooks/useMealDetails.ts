import { useQuery } from '@tanstack/react-query';
import { fetchMealDetails } from '../services/api';

export const useMealDetails = (id: string | null) => {
  return useQuery({
    queryKey: ['meal-details', id],
    queryFn: () => fetchMealDetails(id!),
    enabled: !!id,
  });
};
