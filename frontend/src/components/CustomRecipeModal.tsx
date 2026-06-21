import React, { useState, useEffect } from 'react';
import { useCreateCustomRecipe, useUpdateCustomRecipe, useCustomRecipeDetails } from '../hooks/useCustomRecipes';

interface CustomRecipeModalProps {
  isOpen: boolean;
  onClose: () => void;
  recipeId?: string; // for editing
}

interface IngredientItem {
  name: string;
  measure: string;
}

export function CustomRecipeModal({ isOpen, onClose, recipeId }: CustomRecipeModalProps) {
  const { data: recipeDetails, isLoading: isDetailsLoading } = useCustomRecipeDetails(recipeId);
  const { mutate: createRecipe, isPending: isCreating } = useCreateCustomRecipe();
  const { mutate: updateRecipe, isPending: isUpdating } = useUpdateCustomRecipe();

  const [name, setName] = useState('');
  const [description, setDescription] = useState('');
  const [preparationTimeMinutes, setPreparationTimeMinutes] = useState(30);
  const [defaultPortions, setDefaultPortions] = useState(4);
  const [ingredients, setIngredients] = useState<IngredientItem[]>([{ name: '', measure: '' }]);
  const [instructions, setInstructions] = useState('');
  const [imageUrl, setImageUrl] = useState('');
  const [errorMsg, setErrorMsg] = useState('');

  // Parse ingredients string from "name (measure)" newline separated list
  const parseIngredients = (ingredientsStr: string): IngredientItem[] => {
    if (!ingredientsStr) return [{ name: '', measure: '' }];
    const lines = ingredientsStr.split('\n');
    const parsed = lines.map(line => {
      const trimmed = line.trim();
      if (!trimmed) return null;
      const match = trimmed.match(/^([^(]+)(?:\(([^)]+)\))?$/);
      if (match) {
        return {
          name: match[1].trim(),
          measure: match[2] ? match[2].trim() : ''
        };
      }
      return { name: trimmed, measure: '' };
    }).filter(Boolean) as IngredientItem[];

    return parsed.length > 0 ? parsed : [{ name: '', measure: '' }];
  };

  // Populate form if editing
  useEffect(() => {
    if (recipeId && recipeDetails) {
      setName(recipeDetails.name || '');
      setDescription(recipeDetails.description || '');
      setPreparationTimeMinutes(recipeDetails.preparationTimeMinutes || 30);
      setDefaultPortions(recipeDetails.defaultPortions || 4);
      setIngredients(parseIngredients(recipeDetails.ingredients));
      setInstructions(recipeDetails.instructions || '');
      setImageUrl(recipeDetails.imageUrl || '');
    } else if (!recipeId) {
      // Reset form for creation
      setName('');
      setDescription('');
      setPreparationTimeMinutes(30);
      setDefaultPortions(4);
      setIngredients([{ name: '', measure: '' }]);
      setInstructions('');
      setImageUrl('');
      setErrorMsg('');
    }
  }, [recipeId, recipeDetails, isOpen]);

  if (!isOpen) return null;

  const handleAddIngredient = () => {
    setIngredients([...ingredients, { name: '', measure: '' }]);
  };

  const handleRemoveIngredient = (index: number) => {
    const newIngs = [...ingredients];
    newIngs.splice(index, 1);
    setIngredients(newIngs.length > 0 ? newIngs : [{ name: '', measure: '' }]);
  };

  const handleIngredientChange = (index: number, field: keyof IngredientItem, value: string) => {
    const newIngs = [...ingredients];
    newIngs[index][field] = value;
    setIngredients(newIngs);
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    setErrorMsg('');

    if (!name.trim()) {
      setErrorMsg('Nazwa przepisu jest wymagana');
      return;
    }

    if (!instructions.trim()) {
      setErrorMsg('Instrukcje przygotowania są wymagane');
      return;
    }

    // Filter out empty ingredients
    const validIngredients = ingredients.filter(ing => ing.name.trim() !== '');
    if (validIngredients.length === 0) {
      setErrorMsg('Dodaj przynajmniej jeden składnik');
      return;
    }

    // Serialize ingredients to "name (measure)"
    const ingredientsStr = validIngredients
      .map(ing => ing.measure.trim() 
        ? `${ing.name.trim()} (${ing.measure.trim()})` 
        : ing.name.trim()
      )
      .join('\n');

    const recipeData = {
      name: name.trim(),
      description: description.trim(),
      preparationTimeMinutes,
      defaultPortions,
      ingredients: ingredientsStr,
      instructions: instructions.trim(),
      imageUrl: imageUrl.trim() || undefined,
    };

    const submitCallback = {
      onSuccess: () => {
        onClose();
      },
      onError: (err: any) => {
        setErrorMsg(err.message || 'Wystąpił błąd podczas zapisywania przepisu');
      }
    };

    if (recipeId) {
      updateRecipe({ id: recipeId, recipe: recipeData }, submitCallback);
    } else {
      createRecipe(recipeData, submitCallback);
    }
  };

  const isLoading = recipeId && isDetailsLoading;
  const isSaving = isCreating || isUpdating;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-stone-900/60 backdrop-blur-md transition-opacity duration-300">
      <div 
        className="bg-white/95 w-full max-w-2xl max-h-[90vh] overflow-y-auto rounded-3xl shadow-2xl border border-stone-200/50 flex flex-col transition-all duration-300 scale-100 relative"
        onClick={(e) => e.stopPropagation()}
      >
        {/* Header */}
        <div className="px-6 py-5 border-b border-stone-100 flex items-center justify-between sticky top-0 bg-white/90 backdrop-blur-md z-10">
          <div>
            <h2 className="text-xl sm:text-2xl font-extrabold text-stone-800 tracking-tight">
              {recipeId ? 'Edytuj swój przepis' : 'Dodaj własny przepis'}
            </h2>
            <p className="text-xs sm:text-sm text-stone-500 mt-0.5">
              Stwórz unikalną potrawę i pozwól AI wygenerować kroki do gotowania.
            </p>
          </div>
          <button 
            onClick={onClose}
            className="p-2 text-stone-400 hover:text-stone-600 hover:bg-stone-50 rounded-full transition-all"
            disabled={isSaving}
          >
            <svg xmlns="http://www.w3.org/2000/svg" className="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
        </div>

        {isLoading ? (
          <div className="flex-grow flex flex-col items-center justify-center py-20 gap-4">
            <svg className="animate-spin h-10 w-10 text-amber-500" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
              <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
              <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
            </svg>
            <p className="text-sm font-semibold text-stone-500 animate-pulse">Ładowanie szczegółów przepisu...</p>
          </div>
        ) : (
          <form onSubmit={handleSubmit} className="p-6 flex-grow flex flex-col gap-6">
            {errorMsg && (
              <div className="p-4 bg-red-50 border border-red-200 text-red-700 text-sm font-medium rounded-xl flex items-center gap-2">
                <svg xmlns="http://www.w3.org/2000/svg" className="h-5 w-5 flex-shrink-0" viewBox="0 0 20 20" fill="currentColor">
                  <path fillRule="evenodd" d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-7 4a1 1 0 11-2 0 1 1 0 012 0zm-1-9a1 1 0 00-1 1v4a1 1 0 102 0V6a1 1 0 00-1-1z" clipRule="evenodd" />
                </svg>
                {errorMsg}
              </div>
            )}

            {/* Basic Info */}
            <div className="grid grid-cols-1 gap-5">
              <div>
                <label className="block text-xs font-bold text-stone-500 uppercase tracking-wider mb-2">Nazwa przepisu *</label>
                <input
                  type="text"
                  placeholder="np. Pikantny makaron czosnkowy"
                  value={name}
                  onChange={(e) => setName(e.target.value)}
                  className="w-full px-4 py-3 bg-stone-50/50 border border-stone-200 focus:outline-none focus:ring-2 focus:ring-amber-500/20 focus:border-amber-500 rounded-xl transition text-stone-800 placeholder-stone-400 font-medium"
                  required
                />
              </div>

              <div>
                <label className="block text-xs font-bold text-stone-500 uppercase tracking-wider mb-2">Krótki opis / Wstęp</label>
                <textarea
                  placeholder="Krótka historia potrawy lub wskazówki smakowe..."
                  value={description}
                  onChange={(e) => setDescription(e.target.value)}
                  className="w-full px-4 py-3 bg-stone-50/50 border border-stone-200 focus:outline-none focus:ring-2 focus:ring-amber-500/20 focus:border-amber-500 rounded-xl transition text-stone-800 placeholder-stone-400 font-medium h-20 resize-none"
                />
              </div>

              <div>
                <label className="block text-xs font-bold text-stone-500 uppercase tracking-wider mb-2">Link do zdjęcia (URL)</label>
                <input
                  type="url"
                  placeholder="np. https://images.unsplash.com/photo-..."
                  value={imageUrl}
                  onChange={(e) => setImageUrl(e.target.value)}
                  className="w-full px-4 py-3 bg-stone-50/50 border border-stone-200 focus:outline-none focus:ring-2 focus:ring-amber-500/20 focus:border-amber-500 rounded-xl transition text-stone-800 placeholder-stone-400 font-medium"
                />
              </div>

              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-xs font-bold text-stone-500 uppercase tracking-wider mb-2">Czas przyg. (minut)</label>
                  <input
                    type="number"
                    min="1"
                    max="1440"
                    value={preparationTimeMinutes}
                    onChange={(e) => setPreparationTimeMinutes(Math.max(1, parseInt(e.target.value) || 0))}
                    className="w-full px-4 py-3 bg-stone-50/50 border border-stone-200 focus:outline-none focus:ring-2 focus:ring-amber-500/20 focus:border-amber-500 rounded-xl transition text-stone-800 font-bold"
                    required
                  />
                </div>
                <div>
                  <label className="block text-xs font-bold text-stone-500 uppercase tracking-wider mb-2">Domyślne porcje</label>
                  <input
                    type="number"
                    min="1"
                    max="100"
                    value={defaultPortions}
                    onChange={(e) => setDefaultPortions(Math.max(1, parseInt(e.target.value) || 0))}
                    className="w-full px-4 py-3 bg-stone-50/50 border border-stone-200 focus:outline-none focus:ring-2 focus:ring-amber-500/20 focus:border-amber-500 rounded-xl transition text-stone-800 font-bold"
                    required
                  />
                </div>
              </div>
            </div>

            {/* Ingredients Section */}
            <div>
              <div className="flex items-center justify-between mb-3">
                <label className="block text-xs font-bold text-stone-500 uppercase tracking-wider">Potrzebne składniki</label>
                <button
                  type="button"
                  onClick={handleAddIngredient}
                  className="px-3 py-1 bg-amber-50 hover:bg-amber-100 text-amber-700 text-xs font-bold rounded-lg transition-colors flex items-center gap-1"
                >
                  <svg xmlns="http://www.w3.org/2000/svg" className="h-4.5 w-4.5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
                    <path strokeLinecap="round" strokeLinejoin="round" d="M12 4v16m8-8H4" />
                  </svg>
                  Dodaj składnik
                </button>
              </div>

              <div className="flex flex-col gap-2.5 max-h-[220px] overflow-y-auto pr-1">
                {ingredients.map((ing, idx) => (
                  <div key={idx} className="flex gap-2.5 items-center">
                    <input
                      type="text"
                      placeholder="Nazwa (np. Mąka pszenna)"
                      value={ing.name}
                      onChange={(e) => handleIngredientChange(idx, 'name', e.target.value)}
                      className="flex-grow px-4 py-2.5 bg-stone-50/50 border border-stone-200 focus:outline-none focus:ring-2 focus:ring-amber-500/20 focus:border-amber-500 rounded-xl transition text-stone-800 text-sm font-medium"
                    />
                    <input
                      type="text"
                      placeholder="Ilość/Miara (np. 200g, 2 łyżki)"
                      value={ing.measure}
                      onChange={(e) => handleIngredientChange(idx, 'measure', e.target.value)}
                      className="w-1/3 min-w-[100px] px-4 py-2.5 bg-stone-50/50 border border-stone-200 focus:outline-none focus:ring-2 focus:ring-amber-500/20 focus:border-amber-500 rounded-xl transition text-stone-800 text-sm font-medium"
                    />
                    <button
                      type="button"
                      onClick={() => handleRemoveIngredient(idx)}
                      className="p-2 text-stone-400 hover:text-red-500 hover:bg-red-50 rounded-lg transition-colors flex-shrink-0"
                    >
                      <svg xmlns="http://www.w3.org/2000/svg" className="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
                      </svg>
                    </button>
                  </div>
                ))}
              </div>
            </div>

            {/* Instructions Section */}
            <div>
              <label className="block text-xs font-bold text-stone-500 uppercase tracking-wider mb-2">Instrukcje przygotowania *</label>
              <textarea
                placeholder="Napisz jak krok po kroku przygotować to danie..."
                value={instructions}
                onChange={(e) => setInstructions(e.target.value)}
                className="w-full px-4 py-3 bg-stone-50/50 border border-stone-200 focus:outline-none focus:ring-2 focus:ring-amber-500/20 focus:border-amber-500 rounded-xl transition text-stone-800 placeholder-stone-400 font-medium h-32 resize-y mb-3"
                required
              />

              {/* Dynamic Info Alert */}
              <div className="p-4 bg-gradient-to-r from-amber-500/5 to-orange-500/5 border border-amber-200/40 rounded-2xl flex gap-3">
                <div className="text-amber-500 mt-0.5">
                  <svg xmlns="http://www.w3.org/2000/svg" className="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 10V3L4 14h7v7l9-11h-7z" />
                  </svg>
                </div>
                <div className="text-xs text-stone-600 leading-relaxed">
                  <span className="font-bold text-stone-700">Moc Sztucznej Inteligencji:</span> Po zapisaniu tego przepisu, nasz zaawansowany LLM (w tle) przetłumaczy Twój opis na szczegółowe, interaktywne kroki. Będą one natychmiast gotowe do użytku w trybie <strong className="text-amber-600">Guided Cooking</strong> i symulatorze!
                </div>
              </div>
            </div>

            {/* Action Buttons */}
            <div className="mt-4 pt-5 border-t border-stone-100 flex justify-end gap-3 sticky bottom-0 bg-white/95 py-2">
              <button
                type="button"
                onClick={onClose}
                className="px-6 py-3 bg-stone-100 hover:bg-stone-200 text-stone-700 font-bold rounded-xl transition duration-200 text-sm"
                disabled={isSaving}
              >
                Anuluj
              </button>
              <button
                type="submit"
                disabled={isSaving}
                className="px-6 py-3 bg-gradient-to-r from-amber-500 to-orange-500 hover:from-amber-600 hover:to-orange-600 text-white font-bold rounded-xl shadow-md hover:shadow-lg disabled:opacity-50 active:scale-[0.98] transition-all duration-200 text-sm flex items-center gap-2"
              >
                {isSaving && (
                  <svg className="animate-spin h-5 w-5 text-white" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                    <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
                    <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                  </svg>
                )}
                {isSaving ? 'Zapisywanie...' : recipeId ? 'Zapisz zmiany' : 'Dodaj przepis'}
              </button>
            </div>
          </form>
        )}
      </div>
    </div>
  );
}
