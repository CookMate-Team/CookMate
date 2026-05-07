import { create } from 'zustand';

interface RecipeState {
  source: 'LOCAL' | 'DISCOVERY';
  setSource: (source: 'LOCAL' | 'DISCOVERY') => void;
  searchQuery: string;
  setSearchQuery: (query: string) => void;
}

export const useRecipeStore = create<RecipeState>((set) => ({
  source: 'LOCAL',
  setSource: (source) => set({ source }),
  searchQuery: 'Chicken',
  setSearchQuery: (searchQuery) => set({ searchQuery }),
}));
