import { RecipeCard } from './RecipeCard';
import { useRecipeStore } from '../store/useRecipeStore';
import { useDiscoveryRecipes } from '../hooks/useDiscoveryRecipes';
import { useState } from 'react';

// Temporary mock data for prototype LOCAL
const MOCK_RECIPES = [
  { id: '1', name: 'Spicy Garlic Butter Noodles', time: 15, category: 'Pasta', imageUrl: 'https://images.unsplash.com/photo-1552611052-33e04de081de?ixlib=rb-4.0.3&auto=format&fit=crop&w=500&q=60' },
  { id: '2', name: 'Classic Beef Stew', time: 120, category: 'Beef', imageUrl: 'https://images.unsplash.com/photo-1547592180-85f173990554?ixlib=rb-4.0.3&auto=format&fit=crop&w=500&q=60' },
  { id: '3', name: 'Avocado Toast with Poached Egg', time: 10, category: 'Breakfast', imageUrl: 'https://images.unsplash.com/photo-1525351484163-7529414344d8?ixlib=rb-4.0.3&auto=format&fit=crop&w=500&q=60' },
  { id: '4', name: 'Margherita Pizza', time: 45, category: 'Vegetarian', imageUrl: 'https://images.unsplash.com/photo-1574071318508-1cdbab80d002?ixlib=rb-4.0.3&auto=format&fit=crop&w=500&q=60' },
  { id: '5', name: 'Mango Sticky Rice', time: 30, category: 'Dessert', imageUrl: 'https://images.unsplash.com/photo-1621303837174-89787a7d4729?ixlib=rb-4.0.3&auto=format&fit=crop&w=500&q=60' },
  { id: '6', name: 'Grilled Salmon with Asparagus', time: 25, category: 'Seafood', imageUrl: 'https://images.unsplash.com/photo-1467003909585-2f8a72700288?ixlib=rb-4.0.3&auto=format&fit=crop&w=500&q=60' },
];

export function RecipeGallery() {
  const { source, searchQuery, setSearchQuery } = useRecipeStore();
  const [searchInput, setSearchInput] = useState(searchQuery);
  
  const { data, isLoading, isError } = useDiscoveryRecipes(searchQuery);

  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault();
    setSearchQuery(searchInput);
  };

  return (
    <div className="w-full">
      {source === 'DISCOVERY' && (
        <form onSubmit={handleSearch} className="max-w-xl mx-auto mb-10 relative">
          <input
            type="text"
            placeholder="Search for a meal (e.g. Chicken)..."
            value={searchInput}
            onChange={(e) => setSearchInput(e.target.value)}
            className="w-full pl-5 pr-12 py-3 rounded-full border border-stone-200 shadow-sm focus:outline-none focus:ring-2 focus:ring-amber-500 focus:border-amber-500"
          />
          <button type="submit" className="absolute right-3 top-1/2 -translate-y-1/2 p-2 text-stone-400 hover:text-amber-500">
            <svg xmlns="http://www.w3.org/2000/svg" className="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
            </svg>
          </button>
        </form>
      )}

      {source === 'DISCOVERY' && isLoading && (
        <div className="text-center text-stone-500 py-10">Loading discovery recipes...</div>
      )}

      {source === 'DISCOVERY' && isError && (
        <div className="text-center text-red-500 py-10">Failed to load recipes. Is the backend running?</div>
      )}

      {source === 'DISCOVERY' && data?.meals?.length === 0 && (
        <div className="text-center text-stone-500 py-10">No meals found.</div>
      )}

      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6">
        {source === 'LOCAL' && MOCK_RECIPES.map((recipe) => (
          <RecipeCard
            key={recipe.id}
            id={recipe.id}
            name={recipe.name}
            time={recipe.time}
            category={recipe.category}
            imageUrl={recipe.imageUrl}
          />
        ))}

        {source === 'DISCOVERY' && data?.meals && data.meals.map((meal) => (
          <RecipeCard
            key={meal.idMeal}
            id={meal.idMeal}
            name={meal.strMeal}
            category={meal.strCategory}
            imageUrl={meal.strMealThumb}
            // time is not provided by MealDB
          />
        ))}
      </div>
      
      {/* Static Pagination for Prototype LOCAL */}
      {source === 'LOCAL' && (
        <div className="flex justify-center items-center mt-12 gap-2">
          <button className="px-4 py-2 border border-stone-200 rounded-lg text-stone-500 hover:bg-stone-50 disabled:opacity-50" disabled>
            Previous
          </button>
          <button className="w-10 h-10 rounded-lg bg-amber-500 text-white font-medium shadow-sm">1</button>
          <button className="w-10 h-10 rounded-lg border border-stone-200 text-stone-700 hover:bg-stone-50 font-medium">2</button>
          <button className="w-10 h-10 rounded-lg border border-stone-200 text-stone-700 hover:bg-stone-50 font-medium">3</button>
          <span className="text-stone-400 mx-1">...</span>
          <button className="px-4 py-2 border border-stone-200 rounded-lg text-stone-700 hover:bg-stone-50">
            Next
          </button>
        </div>
      )}
    </div>
  );
}
