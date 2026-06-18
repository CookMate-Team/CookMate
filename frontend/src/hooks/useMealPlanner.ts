import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
  generateWeeklyPlan,
  saveWeeklyPlan,
  getWeeklyPlanHistory,
  deleteWeeklyPlan,
  generateShoppingList,
  saveShoppingList,
  getShoppingListHistory,
  generateShoppingListFromPlan,
} from '../services/mealPlannerApi';
import { useAuth } from '../context/AuthContext';

export const useWeeklyPlanHistory = () => {
  const { isAuthenticated } = useAuth();
  return useQuery({
    queryKey: ['weekly-plan-history'],
    queryFn: getWeeklyPlanHistory,
    enabled: isAuthenticated,
  });
};

export const useGenerateWeeklyPlan = () => {
  return useMutation({
    mutationFn: (mealsPerDay: number) => generateWeeklyPlan(mealsPerDay),
  });
};

export const useSaveWeeklyPlan = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: saveWeeklyPlan,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['weekly-plan-history'] });
    },
  });
};

export const useDeleteWeeklyPlan = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (weeklyPlanId: string) => deleteWeeklyPlan(weeklyPlanId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['weekly-plan-history'] });
    },
  });
};

export const useShoppingListHistory = () => {
  const { isAuthenticated } = useAuth();
  return useQuery({
    queryKey: ['shopping-list-history'],
    queryFn: getShoppingListHistory,
    enabled: isAuthenticated,
  });
};

export const useGenerateShoppingList = () => {
  return useMutation({
    mutationFn: (mealIds: string[]) => generateShoppingList(mealIds),
  });
};

export const useGenerateShoppingListFromPlan = () => {
  return useMutation({
    mutationFn: (weeklyPlanId: string) => generateShoppingListFromPlan(weeklyPlanId),
  });
};

export const useSaveShoppingList = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: saveShoppingList,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['shopping-list-history'] });
    },
  });
};
