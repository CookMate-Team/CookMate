import { create } from 'zustand';

export type RecipeSource = 'LOCAL' | 'DISCOVERY' | 'ACTIVE';

interface RecipeState {
  source: RecipeSource;
  setSource: (source: RecipeSource) => void;
  searchQuery: string;
  setSearchQuery: (query: string) => void;
}

export const useRecipeStore = create<RecipeState>((set) => ({
  source: 'DISCOVERY',
  setSource: (source) => set({ source }),
  searchQuery: 'Chicken',
  setSearchQuery: (searchQuery) => set({ searchQuery }),
}));

