import { useMealDetails } from '../hooks/useMealDetails';

interface ExpandedRecipeCardProps {
  id: string | null;
  onClose: () => void;
  onStartCooking?: (id: string) => void;
}

export function ExpandedRecipeCard({ id, onClose, onStartCooking }: ExpandedRecipeCardProps) {
  const { data, isLoading, isError } = useMealDetails(id);

  if (!id) return null;

  const meal = data?.meals?.[0];

  return (
    <div className="w-full bg-white rounded-3xl overflow-hidden shadow-xl border border-stone-100 flex flex-col md:flex-row relative">
      
      {/* Close Button */}
      <button 
        onClick={onClose}
        className="absolute top-4 right-4 z-10 bg-white/80 backdrop-blur-md hover:bg-white text-stone-600 p-2 rounded-full shadow-sm transition"
      >
        <svg xmlns="http://www.w3.org/2000/svg" className="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
        </svg>
      </button>

      {isLoading && (
        <div className="w-full min-h-[500px] flex items-center justify-center text-stone-500 bg-stone-50">
          <div className="animate-pulse flex flex-col items-center gap-3">
            <svg xmlns="http://www.w3.org/2000/svg" className="h-8 w-8 text-stone-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
            </svg>
            <span className="font-medium">Loading recipe details...</span>
          </div>
        </div>
      )}

      {isError && (
        <div className="w-full p-12 text-center text-red-500">Failed to load recipe details.</div>
      )}

      {meal && (
        <>
          {/* Image Section */}
          <div className="md:w-2/5 min-h-[300px] relative bg-stone-100 flex-shrink-0">
            <img 
              src={meal.strMealThumb} 
              alt={meal.strMeal} 
              className="w-full h-full object-cover absolute inset-0"
            />
            <div className="absolute top-4 left-4 flex gap-2">
              {meal.strCategory && (
                <span className="bg-amber-500 text-white text-xs font-bold px-3 py-1 rounded-full shadow-md">
                  {meal.strCategory}
                </span>
              )}
              {meal.strArea && (
                <span className="bg-stone-800 text-white text-xs font-bold px-3 py-1 rounded-full shadow-md">
                  {meal.strArea}
                </span>
              )}
            </div>
          </div>

          {/* Content Section */}
          <div className="md:w-3/5 p-8">
            <h2 className="text-3xl font-extrabold text-stone-800 mb-6 leading-tight pr-8">{meal.strMeal}</h2>
            
            <div className="mb-8">
              <h3 className="text-xl font-bold text-amber-600 mb-4 border-b border-stone-100 pb-2">Ingredients</h3>
              <ul className="grid grid-cols-1 sm:grid-cols-2 gap-2">
                {Array.from({ length: 20 }).map((_, i) => {
                  const ingredient = meal[`strIngredient${i + 1}`];
                  const measure = meal[`strMeasure${i + 1}`];
                  if (ingredient && ingredient.trim() !== '') {
                    return (
                      <li key={i} className="flex items-center gap-2 text-stone-700">
                        <span className="w-2 h-2 bg-amber-400 rounded-full"></span>
                        <span className="font-medium">{measure}</span> {ingredient}
                      </li>
                    );
                  }
                  return null;
                })}
              </ul>
            </div>

            <div>
              <h3 className="text-xl font-bold text-amber-600 mb-4 border-b border-stone-100 pb-2">Instructions</h3>
              <div className="text-stone-600 leading-relaxed whitespace-pre-line">
                {meal.strInstructions}
              </div>
            </div>
            
            <div className="mt-8 pt-6 border-t border-stone-100 flex flex-wrap items-center gap-4">
              {onStartCooking && id && (
                <button
                  id="start-cooking-btn"
                  onClick={(e) => { e.stopPropagation(); onStartCooking(id); }}
                  className="inline-flex items-center gap-2 px-6 py-2.5 bg-gradient-to-r from-amber-500 to-orange-500 text-white font-bold rounded-full shadow-md hover:shadow-lg hover:scale-105 transition-all duration-300"
                >
                  🍳 Rozpocznij gotowanie
                </button>
              )}
              {meal.strYoutube && (
                <a 
                  href={meal.strYoutube} 
                  target="_blank" 
                  rel="noopener noreferrer"
                  className="inline-flex items-center gap-2 text-red-600 font-bold hover:text-red-700 transition"
                >
                  <svg xmlns="http://www.w3.org/2000/svg" className="h-6 w-6" fill="currentColor" viewBox="0 0 24 24">
                    <path d="M19.615 3.184c-3.604-.246-11.631-.245-15.23 0-3.897.266-4.356 2.62-4.385 8.816.029 6.185.484 8.549 4.385 8.816 3.6.245 11.626.246 15.23 0 3.897-.266 4.356-2.62 4.385-8.816-.029-6.185-.484-8.549-4.385-8.816zm-10.615 12.816v-8l8 3.993-8 4.007z" />
                  </svg>
                  Watch Video Recipe
                </a>
              )}
            </div>
          </div>
        </>
      )}
    </div>
  );
}
