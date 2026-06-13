import { RecipeCard } from './RecipeCard';
import { useRecipeStore } from '../store/useRecipeStore';
import { useDiscoveryRecipes } from '../hooks/useDiscoveryRecipes';
import { useMealDetails } from '../hooks/useMealDetails';
import { useState, useRef, useLayoutEffect, useEffect, useCallback } from 'react';
import { ExpandedRecipeCard } from './ExpandedRecipeCard';
import { useGridCols } from '../hooks/useGridCols';
import gsap from 'gsap';
import { useGlobalActiveSession } from '../hooks/useGlobalActiveSession';
import { completeSimulationSession, completeCookingSession } from '../services/simulatorApi';
import { useQueryClient } from '@tanstack/react-query';
import { useSimulationProgress } from '../hooks/useSimulationProgress';
import { useFavorites } from '../hooks/useFavorites';

function ActiveCookingCard({ 
  recipeId, 
  currentStep, 
  onStartCooking,
  sessionId
}: { 
  recipeId: string; 
  currentStep: number; 
  onStartCooking?: (recipeId: string) => void;
  sessionId: string;
}) {
  const { data, isLoading } = useMealDetails(recipeId);
  const meal = data?.meals?.[0];
  const queryClient = useQueryClient();
  const [isEnding, setIsEnding] = useState(false);
  const { resetSimulationProgress } = useSimulationProgress();
  const { setSource } = useRecipeStore();

  const handleEndSession = async () => {
    setIsEnding(true);
    try {
      // Complete in both services to ensure cleanup
      const results = await Promise.allSettled([
        completeSimulationSession(sessionId),
        completeCookingSession(sessionId),
      ]);

      const failedResults = results.filter(
        (result): result is PromiseRejectedResult => result.status === 'rejected'
      );

      if (failedResults.length > 0) {
        throw new Error(
          `Failed to end session: ${failedResults
            .map((result) =>
              result.reason instanceof Error ? result.reason.message : String(result.reason)
            )
            .join('; ')}`
        );
      }

      resetSimulationProgress();
      queryClient.invalidateQueries({ queryKey: ['active-cooking-session-global'] });
      setSource('DISCOVERY');
    } catch (error) {
      console.error('Failed to end session:', error);
    } finally {
      setIsEnding(false);
    }
  };

  if (isLoading) {
    return (
      <div className="w-full bg-white/60 border border-stone-200/50 rounded-3xl p-8 flex items-center justify-center min-h-[200px] shadow-lg backdrop-blur-md animate-pulse">
        <div className="text-stone-500 font-semibold animate-pulse">Loading active cooking details...</div>
      </div>
    );
  }

  return (
    <div className="w-full bg-gradient-to-br from-amber-500/10 to-orange-500/10 border border-amber-200/50 rounded-3xl p-6 sm:p-8 shadow-xl backdrop-blur-md relative overflow-hidden flex flex-col md:flex-row items-center gap-6 sm:gap-8 transition-all duration-300">
      <div className="absolute top-0 right-0 w-32 h-32 bg-amber-500/5 rounded-full blur-2xl pointer-events-none" />
      
      {meal?.strMealThumb && (
        <img 
          src={meal.strMealThumb} 
          alt={meal.strMeal} 
          className="w-24 h-24 sm:w-32 sm:h-32 rounded-2xl object-cover shadow-md flex-shrink-0" 
        />
      )}

      <div className="flex-grow text-center md:text-left">
        <div className="flex items-center justify-center md:justify-start gap-2 mb-2">
          <span className="px-3 py-1 bg-amber-500 text-white text-[10px] font-bold uppercase tracking-wider rounded-full shadow-sm flex items-center gap-1">
            <span className="w-1.5 h-1.5 bg-red-500 rounded-full animate-pulse" />
            Cooking Session Active
          </span>
          <span className="text-xs text-stone-500 font-medium">Step {currentStep || 1} in progress</span>
        </div>
        
        <h2 className="text-xl sm:text-2xl font-extrabold text-stone-800 tracking-tight">{meal?.strMeal || 'Loading...'}</h2>
        <p className="mt-1 text-sm text-stone-600 max-w-lg">
          You have an active session for this recipe. Resume now to complete your guided cooking.
        </p>
      </div>

      <div className="flex-shrink-0 w-full md:w-auto flex flex-col sm:flex-row gap-3">
        <button
          onClick={() => onStartCooking?.(recipeId)}
          className="px-6 py-3 bg-gradient-to-r from-amber-500 to-orange-500 hover:from-amber-600 hover:to-orange-600 text-white font-bold rounded-xl shadow-lg hover:shadow-xl active:scale-[0.98] transition-all duration-200 text-sm flex items-center justify-center gap-2"
        >
          🍳 Resume Guided Cooking
        </button>
        <button
          onClick={handleEndSession}
          disabled={isEnding}
          className="px-6 py-3 bg-red-50 hover:bg-red-100 text-red-600 font-bold rounded-xl border border-red-200/60 shadow-sm active:scale-[0.98] transition-all duration-200 text-sm disabled:opacity-50 flex items-center justify-center gap-2"
        >
          {isEnding ? 'Ending...' : '🛑 End Session'}
        </button>
      </div>
    </div>
  );
}

// Temporary mock data for prototype LOCAL
const MOCK_RECIPES = [
  { id: '1', name: 'Spicy Garlic Butter Noodles', time: 15, category: 'Pasta', imageUrl: 'https://images.unsplash.com/photo-1552611052-33e04de081de?ixlib=rb-4.0.3&auto=format&fit=crop&w=500&q=60' },
  { id: '2', name: 'Classic Beef Stew', time: 120, category: 'Beef', imageUrl: 'https://images.unsplash.com/photo-1547592180-85f173990554?ixlib=rb-4.0.3&auto=format&fit=crop&w=500&q=60' },
  { id: '3', name: 'Avocado Toast with Poached Egg', time: 10, category: 'Breakfast', imageUrl: 'https://images.unsplash.com/photo-1525351484163-7529414344d8?ixlib=rb-4.0.3&auto=format&fit=crop&w=500&q=60' },
  { id: '4', name: 'Margherita Pizza', time: 45, category: 'Vegetarian', imageUrl: 'https://images.unsplash.com/photo-1574071318508-1cdbab80d002?ixlib=rb-4.0.3&auto=format&fit=crop&w=500&q=60' },
  { id: '5', name: 'Mango Sticky Rice', time: 30, category: 'Dessert', imageUrl: 'https://images.unsplash.com/photo-1621303837174-89787a7d4729?ixlib=rb-4.0.3&auto=format&fit=crop&w=500&q=60' },
  { id: '6', name: 'Grilled Salmon with Asparagus', time: 25, category: 'Seafood', imageUrl: 'https://images.unsplash.com/photo-1467003909585-2f8a72700288?ixlib=rb-4.0.3&auto=format&fit=crop&w=500&q=60' },
];

export function RecipeGallery({ onStartCooking }: { onStartCooking?: (recipeId: string) => void }) {
  const { source, searchQuery, setSearchQuery } = useRecipeStore();
  const { data: activeSession } = useGlobalActiveSession();
  const [searchInput, setSearchInput] = useState(searchQuery);
  const [selectedRecipeId, setSelectedRecipeId] = useState<string | null>(null);
  const [pendingRecipeId, setPendingRecipeId] = useState<string | null>(null);
  const expandedRef = useRef<HTMLDivElement>(null);
  const cols = useGridCols();
  
  const { data: listData, isLoading: isListLoading, isError: isListError } = useDiscoveryRecipes(searchQuery);
  const { data: favoritesData, isLoading: isFavoritesLoading, isError: isFavoritesError, fetchNextPage, hasNextPage, isFetchingNextPage } = useFavorites();
  
  const fetchId = pendingRecipeId || selectedRecipeId;
  const { isLoading: isDetailsLoading } = useMealDetails(fetchId);

  const handleSelect = useCallback((id: string | null) => {
    if (id === null) {
      // Animate close, then unmount
      const wrapper = expandedRef.current;
      if (wrapper) {
        gsap.to(wrapper, {
          height: 0,
          opacity: 0,
          duration: 0.4,
          ease: 'power2.inOut',
          onComplete: () => {
            setSelectedRecipeId(null);
            setPendingRecipeId(null);
          }
        });
      } else {
        setSelectedRecipeId(null);
        setPendingRecipeId(null);
      }
    } else {
      // If switching from one expanded card to another, close first
      if (selectedRecipeId) {
        setSelectedRecipeId(null);
        setTimeout(() => {
          if (source === 'LOCAL') {
            setSelectedRecipeId(id);
          } else {
            setPendingRecipeId(id);
          }
        }, 50);
      } else {
        if (source === 'LOCAL') {
          setSelectedRecipeId(id);
        } else {
          setPendingRecipeId(id);
        }
      }
    }
  }, [selectedRecipeId, source]);

  // Wait for details to load, then expand
  useEffect(() => {
    if (pendingRecipeId && !isDetailsLoading) {
      setSelectedRecipeId(pendingRecipeId);
      setPendingRecipeId(null);
    }
  }, [pendingRecipeId, isDetailsLoading]);

  // Animate expand when a card is selected
  useLayoutEffect(() => {
    if (selectedRecipeId && expandedRef.current) {
      const el = expandedRef.current;
      gsap.fromTo(el, 
        { height: 0, opacity: 0, overflow: 'hidden' },
        { 
          height: 'auto', 
          opacity: 1, 
          duration: 0.5, 
          ease: 'power3.out',
          onComplete: () => {
            gsap.set(el, { clearProps: 'height,overflow' });
          }
        }
      );
    }
  }, [selectedRecipeId]);

  const reorderToRowStart = <T extends Record<string, any>>(array: T[], selectedId: string | null, idKey: keyof T): T[] => {
    if (!selectedId) return array;
    
    const index = array.findIndex(item => item[idKey] === selectedId);
    if (index === -1) return array;

    const rowStartIndex = Math.floor(index / cols) * cols;
    
    const newArray = [...array];
    const [selectedItem] = newArray.splice(index, 1);
    newArray.splice(rowStartIndex, 0, selectedItem);
    
    return newArray;
  };

  const sortedMockRecipes = reorderToRowStart(MOCK_RECIPES, selectedRecipeId, 'id');
  const sortedDiscoveryMeals = reorderToRowStart(listData?.meals || [], selectedRecipeId, 'idMeal');
  
  const allFavorites = favoritesData ? favoritesData.pages.flatMap(page => page.content) : [];
  const sortedFavorites = reorderToRowStart(allFavorites, selectedRecipeId, 'recipeId');

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

      {source === 'DISCOVERY' && isListLoading && (
        <div className="text-center text-stone-500 py-10">Loading discovery recipes...</div>
      )}

      {source === 'DISCOVERY' && isListError && (
        <div className="text-center text-red-500 py-10">Failed to load recipes. Is the backend running?</div>
      )}

      {source === 'DISCOVERY' && listData?.meals?.length === 0 && (
        <div className="text-center text-stone-500 py-10">No meals found.</div>
      )}

      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6">
        {source === 'ACTIVE' && activeSession && (
          <div className="col-span-full">
            <ActiveCookingCard 
              recipeId={activeSession.recipeId} 
              currentStep={activeSession.currentStep} 
              onStartCooking={onStartCooking}
              sessionId={activeSession.sessionId}
            />
          </div>
        )}

        {source === 'LOCAL' && sortedMockRecipes.map((recipe) => (
          <div 
            key={recipe.id} 
            className={selectedRecipeId === recipe.id ? 'col-span-full' : ''}
          >
            {selectedRecipeId === recipe.id ? (
              <div ref={expandedRef}>
                <ExpandedRecipeCard 
                  id={recipe.id} 
                  onClose={() => handleSelect(null)}
                  onStartCooking={onStartCooking}
                />
              </div>
            ) : (
              <RecipeCard
                id={recipe.id}
                name={recipe.name}
                time={recipe.time}
                category={recipe.category}
                imageUrl={recipe.imageUrl}
                onClick={handleSelect}
              />
            )}
          </div>
        ))}

        {source === 'DISCOVERY' && sortedDiscoveryMeals.map((meal) => (
          <div 
            key={meal.idMeal} 
            className={selectedRecipeId === meal.idMeal ? 'col-span-full' : ''}
          >
            {selectedRecipeId === meal.idMeal ? (
              <div ref={expandedRef}>
                <ExpandedRecipeCard 
                  id={meal.idMeal} 
                  onClose={() => handleSelect(null)}
                  onStartCooking={onStartCooking}
                />
              </div>
            ) : (
              <RecipeCard
                id={meal.idMeal}
                name={meal.strMeal}
                category={meal.strCategory}
                imageUrl={meal.strMealThumb}
                onClick={handleSelect}
                isPending={pendingRecipeId === meal.idMeal}
              />
            )}
          </div>
        ))}

        {source === 'FAVORITES' && isFavoritesLoading && (
          <div className="col-span-full text-center text-stone-500 py-10">Loading favorites...</div>
        )}

        {source === 'FAVORITES' && isFavoritesError && (
          <div className="col-span-full text-center text-red-500 py-10">Failed to load favorites.</div>
        )}

        {source === 'FAVORITES' && sortedFavorites.map((fav) => (
          <div 
            key={fav.recipeId} 
            className={selectedRecipeId === fav.recipeId ? 'col-span-full' : ''}
          >
            {selectedRecipeId === fav.recipeId ? (
              <div ref={expandedRef}>
                <ExpandedRecipeCard 
                  id={fav.recipeId} 
                  onClose={() => handleSelect(null)}
                  onStartCooking={onStartCooking}
                />
              </div>
            ) : (
              <RecipeCard
                id={fav.recipeId}
                name={fav.recipeTitle}
                imageUrl={fav.imageUrl}
                onClick={handleSelect}
                isPending={pendingRecipeId === fav.recipeId}
              />
            )}
          </div>
        ))}
      </div>
      
      {/* Load More Pagination for Favorites */}
      {source === 'FAVORITES' && hasNextPage && (
        <div className="flex justify-center items-center mt-12 gap-2">
          <button 
            onClick={() => fetchNextPage()} 
            disabled={isFetchingNextPage}
            className="px-6 py-3 bg-white border border-stone-200 rounded-xl text-stone-700 font-bold hover:bg-stone-50 disabled:opacity-50 shadow-sm transition-all"
          >
            {isFetchingNextPage ? 'Loading more...' : 'Load More'}
          </button>
        </div>
      )}
      {source === 'FAVORITES' && !hasNextPage && sortedFavorites.length > 0 && (
        <div className="flex justify-center items-center mt-12">
          <p className="text-stone-500 font-medium">You have reached the end of your favorites.</p>
        </div>
      )}
      {source === 'FAVORITES' && sortedFavorites.length === 0 && !isFavoritesLoading && !isFavoritesError && (
        <div className="col-span-full text-center text-stone-500 py-10">You have no favorite recipes yet. Start discovering!</div>
      )}
      
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
