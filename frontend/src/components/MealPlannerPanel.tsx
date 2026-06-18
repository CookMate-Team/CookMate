import { useState } from 'react';
import { useAuth } from '../context/AuthContext';
import {
  useGenerateWeeklyPlan,
  useSaveWeeklyPlan,
  useWeeklyPlanHistory,
  useDeleteWeeklyPlan,
  useGenerateShoppingListFromPlan,
} from '../hooks/useMealPlanner';
import type { WeeklyPlanResponse, ShoppingListResponse } from '../types/mealPlanner';

const API_ERROR_MESSAGES: Record<string, string> = {
  MAIN_SERVICE_UNAVAILABLE: 'The recipe service is currently unavailable. Please try again in a moment.',
  MEAL_PLAN_GENERATION_FAILED: 'Could not generate the meal plan. Please try again.',
  WEEKLY_PLAN_NOT_FOUND: 'This plan could not be found. It may have already been deleted.',
  VALIDATION_ERROR: 'Invalid request — please check your settings and try again.',
};

function parseApiError(raw: string): string {
  if (!raw) return 'Something went wrong. Please try again.';

  // Network-level errors thrown by the browser / fetch API
  if (
    raw === 'Failed to fetch' ||
    raw.toLowerCase().includes('networkerror') ||
    raw.toLowerCase().includes('load failed')
  ) {
    return 'Could not reach the server. Please check your internet connection.';
  }

  try {
    const parsed = JSON.parse(raw);
    if (parsed.code && API_ERROR_MESSAGES[parsed.code]) {
      return API_ERROR_MESSAGES[parsed.code];
    }
    if (typeof parsed.message === 'string' && parsed.message) {
      return parsed.message;
    }
  } catch {
    // not JSON
  }

  return 'Something went wrong. Please try again.';
}

function downloadAsTxt(list: ShoppingListResponse, planLabel: string) {
  const date = new Date().toLocaleDateString(undefined, { day: 'numeric', month: 'short', year: 'numeric' });
  const lines = [
    `Shopping List — ${planLabel}`,
    `Generated: ${date}`,
    '',
    ...list.items.map((item) => {
      const measures = item.measures.filter(Boolean).join(', ');
      return measures ? `• ${item.name}: ${measures}` : `• ${item.name}`;
    }),
  ];
  const blob = new Blob([lines.join('\n')], { type: 'text/plain;charset=utf-8' });
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = `shopping-list-${Date.now()}.txt`;
  document.body.appendChild(a);
  a.click();
  document.body.removeChild(a);
  URL.revokeObjectURL(url);
}

interface MealPlannerPanelProps {
  onRequireLogin: () => void;
  onStartCooking: (recipeId: string) => void;
}

function Spinner() {
  return (
    <svg className="animate-spin h-5 w-5" viewBox="0 0 24 24" fill="none">
      <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
      <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z" />
    </svg>
  );
}

function ErrorBanner({ message, onDismiss }: { message: string; onDismiss: () => void }) {
  return (
    <div className="bg-red-50 border border-red-200 rounded-xl px-4 py-3 text-red-700 text-sm flex items-center gap-3">
      <span className="text-base leading-none flex-shrink-0">⚠️</span>
      <p className="flex-1">{message}</p>
      <button
        onClick={onDismiss}
        className="text-red-400 hover:text-red-600 transition-colors flex-shrink-0 p-1 rounded"
        aria-label="Close"
      >
        ✕
      </button>
    </div>
  );
}

export function MealPlannerPanel({ onRequireLogin, onStartCooking }: MealPlannerPanelProps) {
  const { isAuthenticated } = useAuth();
  const [mealsPerDay, setMealsPerDay] = useState(3);
  const [generatedPlan, setGeneratedPlan] = useState<WeeklyPlanResponse | null>(null);
  const [planError, setPlanError] = useState<string | null>(null);
  const [saveError, setSaveError] = useState<string | null>(null);
  // accordion: id of the currently expanded saved plan card
  const [expandedPlanId, setExpandedPlanId] = useState<string | null>(null);
  // id of the plan whose shopping list is currently being downloaded (for loading state)
  const [downloadingPlanId, setDownloadingPlanId] = useState<string | null>(null);
  const [downloadError, setDownloadError] = useState<string | null>(null);

  const generatePlan = useGenerateWeeklyPlan();
  const savePlan = useSaveWeeklyPlan();
  const deletePlan = useDeleteWeeklyPlan();
  const generateShoppingFromPlan = useGenerateShoppingListFromPlan();
  const { data: planHistory } = useWeeklyPlanHistory();

  const handleGeneratePlan = async () => {
    if (!isAuthenticated) { onRequireLogin(); return; }
    setPlanError(null);
    setGeneratedPlan(null);
    try {
      const plan = await generatePlan.mutateAsync(mealsPerDay);
      setGeneratedPlan(plan);
    } catch (err: any) {
      setPlanError(parseApiError(err.message ?? 'Failed to generate plan'));
    }
  };

  // Save plan → hide Section 2; plan appears in Saved Plans list via query invalidation
  const handleSavePlan = async () => {
    if (!generatedPlan) return;
    setSaveError(null);
    try {
      await savePlan.mutateAsync(generatedPlan);
      setGeneratedPlan(null);
    } catch (err: any) {
      setSaveError(parseApiError(err.message ?? 'Failed to save plan'));
    }
  };

  const handleDeletePlan = async (planId: string) => {
    try {
      await deletePlan.mutateAsync(planId);
      if (expandedPlanId === planId) setExpandedPlanId(null);
    } catch {
      // plan already gone; history refreshes automatically
    }
  };

  const handleDownloadShoppingList = async (planId: string, dateLabel: string) => {
    if (!isAuthenticated) { onRequireLogin(); return; }
    setDownloadingPlanId(planId);
    setDownloadError(null);
    try {
      const list = await generateShoppingFromPlan.mutateAsync(planId);
      downloadAsTxt(list, `plan from ${dateLabel}`);
    } catch (err: any) {
      setDownloadError(parseApiError(err.message ?? 'Failed to generate shopping list'));
    } finally {
      setDownloadingPlanId(null);
    }
  };

  const toggleExpanded = (planId: string) => {
    setExpandedPlanId((prev) => (prev === planId ? null : planId));
    setDownloadError(null);
  };

  return (
    <div className="max-w-5xl mx-auto w-full px-4 sm:px-6 lg:px-8 py-8 space-y-8">

      {/* ── Header ── */}
      <div className="text-center">
        <h1 className="text-4xl font-extrabold text-stone-800 tracking-tight">Weekly Meal Planner</h1>
        <p className="mt-3 text-lg text-stone-500 max-w-xl mx-auto">
          Generate a personalised 7-day meal plan and download your shopping list in one click.
        </p>
      </div>

      {/* ── Section 1: Generate Plan ── */}
      <div className="bg-white rounded-2xl shadow-md border border-stone-100 p-6 space-y-5">
        <h2 className="text-xl font-bold text-stone-800">Generate a Plan</h2>
        <div className="flex flex-wrap items-end gap-4">
          <div className="flex flex-col gap-1">
            <label className="text-sm font-semibold text-stone-600">Meals per day</label>
            <div className="flex items-center gap-2">
              {[1, 2, 3, 4, 5].map((n) => (
                <button
                  key={n}
                  onClick={() => setMealsPerDay(n)}
                  className={`w-10 h-10 rounded-xl font-bold transition-all duration-150 ${
                    mealsPerDay === n
                      ? 'bg-gradient-to-br from-amber-500 to-orange-500 text-white shadow-md scale-105'
                      : 'bg-stone-100 text-stone-600 hover:bg-amber-100 hover:text-amber-700'
                  }`}
                >
                  {n}
                </button>
              ))}
            </div>
          </div>
          <button
            onClick={handleGeneratePlan}
            disabled={generatePlan.isPending}
            className="flex items-center gap-2 px-6 py-3 bg-gradient-to-r from-amber-500 to-orange-500 text-white font-bold rounded-xl shadow-md hover:shadow-lg hover:scale-[1.02] active:scale-[0.98] transition-all duration-200 disabled:opacity-50 disabled:cursor-not-allowed disabled:hover:scale-100"
          >
            {generatePlan.isPending ? <><Spinner /> Generating...</> : 'Generate Plan'}
          </button>
        </div>
        {planError && <ErrorBanner message={planError} onDismiss={() => setPlanError(null)} />}
      </div>

      {/* ── Section 2: Freshly Generated Plan (temporary — hidden after saving) ── */}
      {generatedPlan && (
        <div className="bg-white rounded-2xl shadow-md border border-stone-100 p-6 space-y-5">
          <div className="flex items-center justify-between flex-wrap gap-3">
            <h2 className="text-xl font-bold text-stone-800">Your 7-Day Plan</h2>
            <button
              onClick={handleSavePlan}
              disabled={savePlan.isPending}
              className="flex items-center gap-2 px-5 py-2.5 bg-gradient-to-r from-amber-500 to-orange-500 text-white font-bold rounded-xl shadow hover:shadow-md hover:scale-[1.02] active:scale-[0.98] transition-all duration-200 disabled:opacity-50 disabled:cursor-not-allowed disabled:hover:scale-100"
            >
              {savePlan.isPending ? <><Spinner /> Saving...</> : 'Save Plan'}
            </button>
          </div>

          {saveError && <ErrorBanner message={saveError} onDismiss={() => setSaveError(null)} />}

          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-4">
            {generatedPlan.days.map((dayPlan) => (
              <div key={dayPlan.day} className="bg-stone-50 rounded-xl border border-stone-200 p-4 space-y-3">
                <h3 className="font-bold text-stone-700 text-sm uppercase tracking-wider">{dayPlan.day}</h3>
                <div className="space-y-2">
                  {dayPlan.meals.map((meal) => (
                    <div key={meal.id} className="flex items-center gap-2">
                      <img
                        src={meal.thumbnailUrl}
                        alt={meal.name}
                        className="w-10 h-10 rounded-lg object-cover flex-shrink-0 shadow-sm"
                        onError={(e) => { (e.target as HTMLImageElement).style.display = 'none'; }}
                      />
                      <span className="text-sm text-stone-700 font-medium leading-tight">{meal.name}</span>
                    </div>
                  ))}
                </div>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* ── Section 3: Saved Plans (accordion) ── */}
      {isAuthenticated && (
        <div className="bg-white rounded-2xl shadow-md border border-stone-100 p-6 space-y-4">
          <h2 className="text-xl font-bold text-stone-800">
            Saved Plans
            <span className="ml-2 text-sm font-medium text-stone-400 bg-stone-100 px-2 py-0.5 rounded-full">
              {planHistory?.length ?? 0}
            </span>
          </h2>

          {!planHistory || planHistory.length === 0 ? (
            <p className="text-sm text-stone-400 italic text-center py-4">
              No saved plans yet. Generate and save a plan to see it here.
            </p>
          ) : (
            <div className="space-y-2">
              {planHistory.map((saved) => {
                const allMeals = saved.days.flatMap((d) => d.meals);
                const isExpanded = expandedPlanId === saved.id;
                const isDownloading = downloadingPlanId === saved.id;
                const dateLabel = new Date(saved.createdAt).toLocaleDateString(undefined, {
                  day: 'numeric', month: 'short', year: 'numeric',
                });

                return (
                  <div
                    key={saved.id}
                    className={`rounded-xl border transition-colors duration-150 ${
                      isExpanded ? 'border-amber-200 bg-amber-50/40' : 'border-stone-200 bg-stone-50'
                    }`}
                  >
                    {/* ── Collapsed header (always visible) ── */}
                    <div className="flex items-center gap-3 p-4">
                      {/* meal thumbnail strip */}
                      <div className="flex gap-1 flex-shrink-0">
                        {allMeals.slice(0, 5).map((meal) => (
                          <img
                            key={meal.id}
                            src={meal.thumbnailUrl}
                            alt={meal.name}
                            title={meal.name}
                            className="w-8 h-8 rounded-lg object-cover shadow-sm"
                            onError={(e) => { (e.target as HTMLImageElement).style.display = 'none'; }}
                          />
                        ))}
                        {allMeals.length > 5 && (
                          <span className="w-8 h-8 rounded-lg bg-stone-200 text-stone-500 text-xs flex items-center justify-center font-semibold flex-shrink-0">
                            +{allMeals.length - 5}
                          </span>
                        )}
                      </div>

                      {/* text info — clickable to expand */}
                      <button
                        onClick={() => toggleExpanded(saved.id)}
                        className="flex-1 text-left min-w-0"
                      >
                        <p className="font-semibold text-stone-700 text-sm truncate">
                          {saved.mealsPerDay} meal{saved.mealsPerDay !== 1 ? 's' : ''} / day
                          <span className="ml-2 text-xs font-normal text-stone-400">({allMeals.length} total)</span>
                        </p>
                        <p className="text-xs text-stone-400 mt-0.5">{dateLabel}</p>
                      </button>

                      {/* expand chevron */}
                      <button
                        onClick={() => toggleExpanded(saved.id)}
                        className="text-stone-400 hover:text-stone-600 transition-colors flex-shrink-0 p-1"
                        aria-label={isExpanded ? 'Collapse' : 'Expand'}
                      >
                        <svg
                          className={`w-5 h-5 transition-transform duration-200 ${isExpanded ? 'rotate-180' : ''}`}
                          fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}
                        >
                          <path strokeLinecap="round" strokeLinejoin="round" d="M19 9l-7 7-7-7" />
                        </svg>
                      </button>
                    </div>

                    {/* ── Expanded content ── */}
                    {isExpanded && (
                      <div className="px-4 pb-4 space-y-4 border-t border-amber-200/60">
                        {/* day grid */}
                        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-3 pt-4">
                          {saved.days.map((dayPlan) => (
                            <div key={dayPlan.day} className="bg-white rounded-xl border border-stone-200 p-3 space-y-2">
                              <h3 className="font-bold text-stone-700 text-xs uppercase tracking-wider">{dayPlan.day}</h3>
                              <div className="space-y-1.5">
                                {dayPlan.meals.map((meal) => (
                                  <div key={meal.id} className="flex items-center gap-2 group">
                                    <img
                                      src={meal.thumbnailUrl}
                                      alt={meal.name}
                                      className="w-9 h-9 rounded-lg object-cover flex-shrink-0 shadow-sm"
                                      onError={(e) => { (e.target as HTMLImageElement).style.display = 'none'; }}
                                    />
                                    <span className="text-xs text-stone-700 font-medium leading-tight flex-1 min-w-0">{meal.name}</span>
                                    <button
                                      onClick={() => onStartCooking(meal.id)}
                                      title="Start cooking"
                                      className="flex-shrink-0 opacity-0 group-hover:opacity-100 text-xs font-semibold text-amber-600 hover:text-white hover:bg-gradient-to-r hover:from-amber-500 hover:to-orange-500 border border-amber-300 hover:border-transparent px-2 py-0.5 rounded-lg transition-all duration-150"
                                    >
                                      ▶ Cook
                                    </button>
                                  </div>
                                ))}
                              </div>
                            </div>
                          ))}
                        </div>

                        {/* actions row */}
                        <div className="flex items-center gap-3 flex-wrap pt-1">
                          <button
                            onClick={() => handleDownloadShoppingList(saved.id, dateLabel)}
                            disabled={isDownloading}
                            className="flex items-center gap-2 text-sm font-bold px-4 py-2 bg-gradient-to-r from-orange-500 to-red-500 text-white rounded-xl shadow hover:shadow-md hover:scale-[1.02] active:scale-[0.98] transition-all duration-200 disabled:opacity-50 disabled:cursor-not-allowed disabled:hover:scale-100"
                          >
                            {isDownloading ? <><Spinner /> Generating...</> : 'Download Shopping List'}
                          </button>
                          <button
                            onClick={() => handleDeletePlan(saved.id)}
                            disabled={deletePlan.isPending}
                            className="text-sm text-stone-400 hover:text-red-500 hover:bg-red-50 border border-transparent hover:border-red-200 px-3 py-2 rounded-xl transition-all duration-150 disabled:opacity-40"
                          >
                            Delete
                          </button>
                        </div>

                        {downloadError && downloadingPlanId === null && expandedPlanId === saved.id && (
                          <ErrorBanner message={downloadError} onDismiss={() => setDownloadError(null)} />
                        )}
                      </div>
                    )}
                  </div>
                );
              })}
            </div>
          )}
        </div>
      )}
    </div>
  );
}
