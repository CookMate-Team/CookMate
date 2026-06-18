export interface MealItem {
  id: string;
  name: string;
  thumbnailUrl: string;
}

export interface DayPlan {
  day: string;
  meals: MealItem[];
}

export interface WeeklyPlanResponse {
  days: DayPlan[];
}

export interface SavedWeeklyPlanResponse {
  id: string;
  createdAt: string;
  mealsPerDay: number;
  days: DayPlan[];
}

export interface ShoppingListItem {
  name: string;
  measures: string[];
  recipes: string[];
}

export interface ShoppingListResponse {
  items: ShoppingListItem[];
}

export interface SavedShoppingListResponse {
  id: string;
  createdAt: string;
  items: ShoppingListItem[];
}
