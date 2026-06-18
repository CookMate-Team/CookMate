import { authFetch } from './authFetch';
import type {
  WeeklyPlanResponse,
  SavedWeeklyPlanResponse,
  ShoppingListResponse,
  SavedShoppingListResponse,
} from '../types/mealPlanner';

const API_BASE_URL = '/api/planner';

export const generateWeeklyPlan = async (mealsPerDay: number): Promise<WeeklyPlanResponse> => {
  const response = await authFetch(`${API_BASE_URL}/weekly-plan?mealsPerDay=${mealsPerDay}`);
  if (!response.ok) {
    const errorBody = await response.text();
    throw new Error(errorBody || 'Failed to generate weekly plan');
  }
  return response.json();
};

export const saveWeeklyPlan = async (plan: WeeklyPlanResponse): Promise<SavedWeeklyPlanResponse> => {
  const response = await authFetch(`${API_BASE_URL}/weekly-plan/save`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(plan),
  });
  if (!response.ok) {
    const errorBody = await response.text();
    throw new Error(errorBody || 'Failed to save weekly plan');
  }
  return response.json();
};

export const getWeeklyPlanHistory = async (): Promise<SavedWeeklyPlanResponse[]> => {
  const response = await authFetch(`${API_BASE_URL}/weekly-plan/history`);
  if (!response.ok) {
    const errorBody = await response.text();
    throw new Error(errorBody || 'Failed to fetch weekly plan history');
  }
  return response.json();
};

export const generateShoppingList = async (mealIds: string[]): Promise<ShoppingListResponse> => {
  const response = await authFetch(`${API_BASE_URL}/shopping-list`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ mealIds }),
  });
  if (!response.ok) {
    const errorBody = await response.text();
    throw new Error(errorBody || 'Failed to generate shopping list');
  }
  return response.json();
};

export const saveShoppingList = async (list: ShoppingListResponse): Promise<SavedShoppingListResponse> => {
  const response = await authFetch(`${API_BASE_URL}/shopping-list/save`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(list),
  });
  if (!response.ok) {
    const errorBody = await response.text();
    throw new Error(errorBody || 'Failed to save shopping list');
  }
  return response.json();
};

export const getShoppingListHistory = async (): Promise<SavedShoppingListResponse[]> => {
  const response = await authFetch(`${API_BASE_URL}/shopping-list/history`);
  if (!response.ok) {
    const errorBody = await response.text();
    throw new Error(errorBody || 'Failed to fetch shopping list history');
  }
  return response.json();
};

export const deleteWeeklyPlan = async (weeklyPlanId: string): Promise<void> => {
  const response = await authFetch(`${API_BASE_URL}/weekly-plan/${weeklyPlanId}`, {
    method: 'DELETE',
  });
  if (!response.ok) {
    const errorBody = await response.text();
    throw new Error(errorBody || 'Failed to delete weekly plan');
  }
};

export const generateShoppingListFromPlan = async (weeklyPlanId: string): Promise<ShoppingListResponse> => {
  const response = await authFetch(`${API_BASE_URL}/shopping-list/from-plan/${weeklyPlanId}`, {
    method: 'POST',
  });
  if (!response.ok) {
    const errorBody = await response.text();
    throw new Error(errorBody || 'Failed to generate shopping list from plan');
  }
  return response.json();
};
