import { useMealDetails } from '../hooks/useMealDetails';
import { useCheckFavorite, useAddFavorite, useRemoveFavorite } from '../hooks/useFavorites';
import { useAuth } from '../context/AuthContext';
import { useState } from 'react';
import { scaleMeasurement } from '../utils/scaling';
import { useDeleteCustomRecipe } from '../hooks/useCustomRecipes';

interface ExpandedRecipeCardProps {
  id: string | null;
  onClose: () => void;
  onStartCooking?: (id: string, targetPortions?: number) => void;
  onRequireLogin?: () => void;
  onEditRecipe?: () => void;
}

export function ExpandedRecipeCard({ id, onClose, onStartCooking, onRequireLogin, onEditRecipe }: ExpandedRecipeCardProps) {
  const { isAuthenticated } = useAuth();
  const { data, isLoading, isError } = useMealDetails(id);
  const { data: isFav } = useCheckFavorite(id);
  const { mutate: addFav } = useAddFavorite();
  const { mutate: removeFav } = useRemoveFavorite();
  const { mutate: deleteRecipe, isPending: isDeleting } = useDeleteCustomRecipe();
  
  const defaultPortions = 4;
  const [targetPortions, setTargetPortions] = useState(defaultPortions);
  const [showDeleteConfirm, setShowDeleteConfirm] = useState(false);

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
              <div className="flex flex-col sm:flex-row sm:items-center justify-between border-b border-stone-100 pb-2 mb-4 gap-4">
                <h3 className="text-xl font-bold text-amber-600">Ingredients</h3>
                <div className="flex items-center gap-2 bg-stone-50 px-3 py-1.5 rounded-full border border-stone-200">
                  <span className="text-sm text-stone-500 font-medium">Portions:</span>
                  <button 
                    onClick={() => setTargetPortions(Math.max(1, targetPortions - 1))}
                    className="w-6 h-6 rounded-full bg-white border border-stone-300 flex items-center justify-center text-stone-600 hover:bg-stone-100 hover:text-amber-600 transition"
                  >-</button>
                  <span className="w-4 text-center font-bold text-stone-700">{targetPortions}</span>
                  <button 
                    onClick={() => setTargetPortions(Math.min(10, targetPortions + 1))}
                    className="w-6 h-6 rounded-full bg-white border border-stone-300 flex items-center justify-center text-stone-600 hover:bg-stone-100 hover:text-amber-600 transition"
                  >+</button>
                </div>
              </div>
              <ul className="grid grid-cols-1 sm:grid-cols-2 gap-2">
                {Array.from({ length: 20 }).map((_, i) => {
                  const ingredient = meal[`strIngredient${i + 1}`];
                  const baseMeasure = meal[`strMeasure${i + 1}`];
                  if (ingredient && ingredient.trim() !== '') {
                    const measure = scaleMeasurement(baseMeasure || '', targetPortions / defaultPortions, ingredient);
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
                  onClick={(e) => { e.stopPropagation(); onStartCooking(id, targetPortions); }}
                  className="inline-flex items-center gap-2 px-6 py-2.5 bg-gradient-to-r from-amber-500 to-orange-500 text-white font-bold rounded-full shadow-md hover:shadow-lg hover:scale-105 transition-all duration-300"
                >
                  Start cooking
                </button>
              )}
              {meal?.strCategory === 'Custom' && onEditRecipe && (
                <button
                  onClick={(e) => {
                    e.stopPropagation();
                    onEditRecipe();
                  }}
                  className="inline-flex items-center gap-2 px-6 py-2.5 bg-white text-stone-700 border border-stone-200 hover:bg-stone-50 hover:border-amber-500 hover:text-amber-600 font-bold rounded-full shadow-sm transition-all duration-300"
                >
                  <svg xmlns="http://www.w3.org/2000/svg" className="h-5 w-5 text-current" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z" />
                  </svg>
                  Edit Recipe
                </button>
              )}
              {meal?.strCategory === 'Custom' && (
                <button
                  onClick={(e) => {
                    e.stopPropagation();
                    setShowDeleteConfirm(true);
                  }}
                  disabled={isDeleting}
                  className="inline-flex items-center gap-2 px-6 py-2.5 bg-red-50 hover:bg-red-100 disabled:opacity-50 text-red-600 font-bold rounded-full border border-red-200/60 shadow-sm active:scale-[0.98] transition-all duration-200"
                >
                  <svg xmlns="http://www.w3.org/2000/svg" className="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
                  </svg>
                  {isDeleting ? 'Usuwanie...' : 'Usuń przepis'}
                </button>
              )}
              {id && meal?.strCategory !== 'Custom' && meal?.strCategory !== 'Local' && (
                <button
                  onClick={(e) => {
                    e.stopPropagation();
                    if (!isAuthenticated) {
                      onRequireLogin?.();
                      return;
                    }
                    if (isFav) {
                      removeFav(id);
                    } else {
                      addFav({ recipeId: id, request: { recipeTitle: meal.strMeal, imageUrl: meal.strMealThumb } });
                    }
                  }}
                  className={`inline-flex items-center gap-2 px-6 py-2.5 font-bold rounded-full border shadow-sm transition-all duration-300 ${isFav ? 'bg-red-50 text-red-600 border-red-200 hover:bg-red-100' : 'bg-white text-stone-600 border-stone-200 hover:bg-stone-50'}`}
                >
                  <svg xmlns="http://www.w3.org/2000/svg" className={`h-5 w-5 ${isFav ? 'fill-current' : 'fill-transparent'}`} viewBox="0 0 24 24" stroke="currentColor" strokeWidth={isFav ? 0 : 2}>
                    <path strokeLinecap="round" strokeLinejoin="round" d="M4.318 6.318a4.5 4.5 0 000 6.364L12 20.364l7.682-7.682a4.5 4.5 0 00-6.364-6.364L12 7.636l-1.318-1.318a4.5 4.5 0 00-6.364 0z" />
                  </svg>
                  {isFav ? 'Remove from favorites' : 'Add to favorites'}
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
      {showDeleteConfirm && (
        <div 
          className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-stone-900/60 backdrop-blur-sm transition-opacity duration-300 animate-fadeIn"
          onClick={(e) => { e.stopPropagation(); setShowDeleteConfirm(false); }}
        >
          <div 
            className="bg-white p-6 rounded-3xl shadow-2xl max-w-sm w-full text-center border border-stone-200/50"
            onClick={(e) => e.stopPropagation()}
          >
            <div className="w-12 h-12 bg-red-50 text-red-500 rounded-full flex items-center justify-center mx-auto mb-4">
              <svg xmlns="http://www.w3.org/2000/svg" className="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
              </svg>
            </div>
            <h3 className="text-lg font-extrabold text-stone-800 mb-2">Usuń przepis</h3>
            <p className="text-sm text-stone-500 mb-6 leading-relaxed">
              Czy na pewno chcesz usunąć ten przepis? Ta operacja jest nieodwracalna. Wszystkie powiązane kroki wygenerowane przez AI również zostaną usunięte.
            </p>
            <div className="flex gap-3 justify-center">
              <button
                type="button"
                onClick={() => setShowDeleteConfirm(false)}
                className="px-4 py-2.5 bg-stone-100 hover:bg-stone-200 text-stone-700 font-bold rounded-xl transition duration-150 text-sm"
              >
                Anuluj
              </button>
              <button
                type="button"
                onClick={() => {
                  deleteRecipe(id, {
                    onSuccess: () => {
                      setShowDeleteConfirm(false);
                      onClose();
                    }
                  });
                }}
                disabled={isDeleting}
                className="px-4 py-2.5 bg-red-600 hover:bg-red-700 text-white font-bold rounded-xl shadow-md hover:shadow-lg transition duration-150 text-sm flex items-center gap-1.5"
              >
                {isDeleting ? 'Usuwanie...' : 'Usuń przepis'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
