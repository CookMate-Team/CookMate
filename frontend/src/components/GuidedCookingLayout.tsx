import { useEffect, useMemo, useRef, useState } from 'react';
import { useMealDetails } from '../hooks/useMealDetails';
import { useSimulationProgress } from '../hooks/useSimulationProgress';
import { useRecipeSteps } from '../hooks/useRecipeSteps';
import { SimulatorPanel } from './SimulatorPanel';

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
  const { data: dbSteps } = useRecipeSteps(recipeId);
  const { currentStep } = useSimulationProgress();
  const meal = data?.meals?.[0];
  const stepRefs = useRef<Record<number, HTMLDivElement | null>>({});

  const instructionSteps = useMemo(() => {
    if (dbSteps && dbSteps.length > 0) {
      return dbSteps.map((step) => step.description);
    }

    if (!meal?.strInstructions) {
      return [];
    }

    return meal.strInstructions
      .split(/(?<=[.!?])\s+/)
      .map((step) => step.trim())
      .filter((step) => step.length > 0);
  }, [meal?.strInstructions, dbSteps]);

  useEffect(() => {
    if (currentStep > 0) {
      const element = stepRefs.current[currentStep];
      element?.scrollIntoView({ behavior: 'smooth', block: 'center' });
    }
  }, [currentStep]);

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
            📖 Instructions
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
            🍳 Simulator
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
              Back to recipes
            </button>

            {isLoading && (
              <div className="flex items-center justify-center py-20">
                <div className="animate-pulse flex flex-col items-center gap-3">
                  <span className="font-medium text-stone-500">Loading recipe...</span>
                </div>
              </div>
            )}

            {isError && (
              <div className="text-center text-red-500 py-10">Failed to load recipe.</div>
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
                  <h2 className="text-lg sm:text-xl font-bold text-amber-600 mb-3 sm:mb-4 border-b border-stone-100 pb-2">🧑‍🍳 Ingredients</h2>
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
                  <h2 className="text-lg sm:text-xl font-bold text-amber-600 mb-3 sm:mb-4 border-b border-stone-100 pb-2">📋 Instructions</h2>
                  {/* If simulation is active, show steps with highlighting */}
                  {currentStep > 0 ? (
                    <div className="space-y-3">
                      {instructionSteps.map((step, idx) => {
                        const stepNum = idx + 1;
                        const isCurrentStep = stepNum === currentStep;
                        const isExecuted = stepNum < currentStep;
                        
                        return (
                          <div
                            key={idx}
                            ref={(element) => {
                              stepRefs.current[stepNum] = element;
                            }}
                            className={`p-3 rounded-lg border-l-4 transition-all duration-300
                              ${isCurrentStep
                                ? 'border-l-amber-500 bg-amber-50 ring-2 ring-amber-200'
                                : isExecuted
                                  ? 'border-l-green-500 bg-green-50 opacity-70'
                                  : 'border-l-stone-300 bg-stone-50'
                              }`}
                          >
                            <div className="flex items-start gap-3">
                              <span className={`text-sm font-bold whitespace-nowrap flex-shrink-0
                                ${isCurrentStep
                                  ? 'text-amber-700'
                                  : isExecuted
                                    ? 'text-green-700'
                                    : 'text-stone-400'
                                }`}>
                                {isCurrentStep ? '▶️' : isExecuted ? '✅' : '⏳'} Step {stepNum}
                              </span>
                              <p className={`text-sm sm:text-base leading-relaxed
                                ${isCurrentStep
                                  ? 'text-amber-900 font-semibold'
                                  : isExecuted
                                    ? 'text-green-800'
                                    : 'text-stone-600'
                                }`}>
                                {step.trim()}
                              </p>
                            </div>
                          </div>
                        );
                      })}
                    </div>
                  ) : (
                    <div className="text-stone-600 leading-relaxed whitespace-pre-line text-sm sm:text-base">{meal.strInstructions}</div>
                  )}
                </div>
              </>
            )}
          </div>
        </div>

        {/* ── Simulator section (right on desktop) ── */}
        <div
          className={`
            md:block md:w-1/2 md:flex-none
            ${activeView === 'simulator' ? 'block w-full' : 'hidden'}
          `}
        >
          <SimulatorPanel recipeId={recipeId} recipeName={meal?.strMeal} />
        </div>
      </div>
    </div>
  );
}
