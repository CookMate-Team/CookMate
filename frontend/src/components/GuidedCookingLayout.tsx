import { useState } from 'react';
import { useMealDetails } from '../hooks/useMealDetails';

interface GuidedCookingLayoutProps {
  recipeId: string;
  onClose: () => void;
}

type ActiveView = 'recipe' | 'simulator';

/**
 * GuidedCookingLayout – responsive split-screen layout.
 *
 * Desktop (≥ md): Side-by-side 50/50 split below the sticky navbar.
 * Mobile (< md):  Full-width with a ViewToggle to switch between recipe
 *                 and simulator views without losing state.
 *
 * The simulator panel is currently a green placeholder square.
 */
export function GuidedCookingLayout({ recipeId, onClose }: GuidedCookingLayoutProps) {
  const [activeView, setActiveView] = useState<ActiveView>('recipe');
  const { data, isLoading, isError } = useMealDetails(recipeId);
  const meal = data?.meals?.[0];

  return (
    <div className="flex flex-col flex-1 overflow-hidden">
      {/* ── Mobile-only tab switcher ── */}
      <div className="md:hidden flex-shrink-0 bg-white/80 backdrop-blur-md border-b border-stone-200 shadow-sm z-30">
        <div className="flex">
          <button
            id="view-toggle-recipe"
            onClick={() => setActiveView('recipe')}
            className={`flex-1 py-3 text-sm font-semibold tracking-wide transition-all duration-300
              ${activeView === 'recipe'
                ? 'text-amber-600 border-b-2 border-amber-500 bg-amber-50/50'
                : 'text-stone-500 hover:text-stone-700 hover:bg-stone-50'
              }`}
          >
            📖 Instrukcje
          </button>
          <button
            id="view-toggle-simulator"
            onClick={() => setActiveView('simulator')}
            className={`flex-1 py-3 text-sm font-semibold tracking-wide transition-all duration-300
              ${activeView === 'simulator'
                ? 'text-amber-600 border-b-2 border-amber-500 bg-amber-50/50'
                : 'text-stone-500 hover:text-stone-700 hover:bg-stone-50'
              }`}
          >
            🍳 Symulator
          </button>
        </div>
      </div>

      {/* ── Main content area ── */}
      <div className="flex flex-1 min-h-0">
        {/* ── Recipe section (left on desktop) ──
             Uses overflow-y-scroll (not auto) so the scrollbar gutter is always
             reserved and the layout doesn't "jump" when switching views on mobile.
        */}
        <div
          className={`
            overflow-y-scroll guided-scrollbar
            md:block md:w-1/2 md:flex-none md:border-r md:border-stone-200
            ${activeView === 'recipe' ? 'block w-full' : 'hidden'}
          `}
        >
          <div className="p-4 sm:p-6 space-y-6">
            {/* Back button */}
            <button
              onClick={onClose}
              className="inline-flex items-center gap-2 text-stone-500 hover:text-amber-600 font-medium transition-colors text-sm group"
            >
              <svg xmlns="http://www.w3.org/2000/svg" className="h-4 w-4 transition-transform group-hover:-translate-x-0.5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 19l-7-7 7-7" />
              </svg>
              Wróć do przepisów
            </button>

            {isLoading && (
              <div className="flex items-center justify-center py-20">
                <div className="animate-pulse flex flex-col items-center gap-3">
                  <span className="font-medium text-stone-500">Ładowanie przepisu…</span>
                </div>
              </div>
            )}

            {isError && (
              <div className="text-center text-red-500 py-10">Nie udało się załadować przepisu.</div>
            )}

            {meal && (
              <>
                {/* Hero image */}
                <div className="relative rounded-2xl overflow-hidden shadow-lg">
                  <img src={meal.strMealThumb} alt={meal.strMeal} className="w-full h-48 sm:h-56 md:h-72 object-cover" />
                  <div className="absolute inset-0 bg-gradient-to-t from-black/60 via-transparent to-transparent" />
                  <div className="absolute bottom-0 left-0 right-0 p-4 sm:p-5">
                    <div className="flex flex-wrap gap-2 mb-2">
                      {meal.strCategory && (
                        <span className="bg-amber-500 text-white text-xs font-bold px-3 py-1 rounded-full shadow-md">{meal.strCategory}</span>
                      )}
                      {meal.strArea && (
                        <span className="bg-stone-800/80 text-white text-xs font-bold px-3 py-1 rounded-full shadow-md backdrop-blur-sm">{meal.strArea}</span>
                      )}
                    </div>
                    <h1 className="text-xl sm:text-2xl md:text-3xl font-extrabold text-white leading-tight drop-shadow-lg">{meal.strMeal}</h1>
                  </div>
                </div>

                {/* Ingredients */}
                <div>
                  <h2 className="text-lg sm:text-xl font-bold text-amber-600 mb-3 sm:mb-4 border-b border-stone-100 pb-2">🧑‍🍳 Składniki</h2>
                  <ul className="grid grid-cols-1 sm:grid-cols-2 gap-2">
                    {Array.from({ length: 20 }).map((_, i) => {
                      const ingredient = meal[`strIngredient${i + 1}`];
                      const measure = meal[`strMeasure${i + 1}`];
                      if (ingredient && ingredient.trim() !== '') {
                        return (
                          <li key={i} className="flex items-center gap-2 text-stone-700 text-sm sm:text-base">
                            <span className="w-2 h-2 bg-amber-400 rounded-full flex-shrink-0" />
                            <span className="font-medium">{measure}</span> {ingredient}
                          </li>
                        );
                      }
                      return null;
                    })}
                  </ul>
                </div>

                {/* Instructions */}
                <div>
                  <h2 className="text-lg sm:text-xl font-bold text-amber-600 mb-3 sm:mb-4 border-b border-stone-100 pb-2">📋 Instrukcje</h2>
                  <div className="text-stone-600 leading-relaxed whitespace-pre-line text-sm sm:text-base">{meal.strInstructions}</div>
                </div>
              </>
            )}
          </div>
        </div>

        {/* ── Simulator section (right on desktop) – GREEN PLACEHOLDER ──
             Also uses overflow-y-scroll so scrollbar gutter matches the recipe panel.
        */}
        <div
          className={`
            overflow-y-scroll guided-scrollbar
            md:block md:w-1/2 md:flex-none
            ${activeView === 'simulator' ? 'block w-full' : 'hidden'}
          `}
        >
          <div className="h-full flex items-center justify-center bg-green-500 m-4 rounded-2xl min-h-[300px]">
            <p className="text-white text-xl sm:text-2xl font-bold text-center px-4">Simulator Panel Placeholder</p>
          </div>
        </div>
      </div>
    </div>
  );
}
