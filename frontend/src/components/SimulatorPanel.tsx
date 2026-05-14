import { useState, useCallback } from 'react';
import {
  startSimulation,
  executeNextStep,
  getSimulationStatus,
  generateSteps,
} from '../services/simulatorApi';
import type { SimulationStatusResponse } from '../types/simulator';

const STATUS_LABEL: Record<string, { label: string; emoji: string }> = {
  PENDING: { label: 'Oczekujący', emoji: '⏳' },
  EXECUTED: { label: 'Wykonany', emoji: '✅' },
};

/** Map known backend error codes to user-friendly Polish messages. */
const ERROR_MESSAGES: Record<string, string> = {
  MAIN_SERVICE_UNAVAILABLE:
    'Serwis przepisów jest niedostępny. Upewnij się, że przepis posiada zdefiniowane kroki gotowania.',
  SESSION_NOT_FOUND: 'Sesja symulacji nie została znaleziona. Spróbuj rozpocząć od nowa.',
  INVALID_STATE: 'Nieprawidłowy stan symulacji. Spróbuj zresetować sesję.',
};

/** Try to extract a friendly message from a backend error response. */
function parseApiError(raw: string): string {
  try {
    const parsed = JSON.parse(raw);
    if (parsed.code && ERROR_MESSAGES[parsed.code]) {
      return ERROR_MESSAGES[parsed.code];
    }
    if (parsed.message) {
      return parsed.message;
    }
  } catch {
    // not JSON – use as-is
  }
  return raw;
}

interface SimulatorPanelProps {
  recipeId: string;
  recipeName?: string;
}

export function SimulatorPanel({ recipeId, recipeName }: SimulatorPanelProps) {
  const [sessionId, setSessionId] = useState<string | null>(null);
  const [status, setStatus] = useState<SimulationStatusResponse | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const [, setLoadingMessage] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  // ── Start session ──
  const handleStart = useCallback(async () => {
    setIsLoading(true);
    setError(null);
    try {
      // First, ensure steps exist (generates via LLM if needed)
      setLoadingMessage('Generowanie kroków przepisu...');
      await generateSteps(recipeId);

      // Then start the simulation session
      setLoadingMessage('Uruchamianie symulacji...');
      const res = await startSimulation({ recipeId });
      setSessionId(res.sessionId);
      setStatus(res);
    } catch (err: any) {
      setError(parseApiError(err.message ?? 'Nie udało się rozpocząć symulacji'));
    } finally {
      setIsLoading(false);
      setLoadingMessage(null);
    }
  }, [recipeId]);

  // ── Execute next step ──
  const handleExecuteNext = useCallback(async () => {
    if (!sessionId) return;
    setIsLoading(true);
    setError(null);
    try {
      await executeNextStep(sessionId);
      const updated = await getSimulationStatus(sessionId);
      setStatus(updated);
    } catch (err: any) {
      setError(parseApiError(err.message ?? 'Błąd wykonania kroku'));
    } finally {
      setIsLoading(false);
    }
  }, [sessionId]);

  // ── Reset ──
  const handleReset = useCallback(() => {
    setSessionId(null);
    setStatus(null);
    setError(null);
  }, []);

  const isCompleted = status?.status === 'COMPLETED';
  const currentStep = status?.currentStep ?? 0;
  const totalSteps = status?.totalSteps ?? 0;
  const progressPercent = totalSteps > 0 ? (currentStep / totalSteps) * 100 : 0;
  const history = status?.history ?? [];

  return (
    <div className="flex flex-col h-full bg-gradient-to-b from-stone-50 to-white">
      {/* ── Panel Header ── */}
      <div className="px-5 py-4 bg-gradient-to-r from-amber-500 to-orange-500 text-white shadow-md flex-shrink-0">
        <h2 className="text-lg font-bold tracking-tight">🍳 Symulator Gotowania</h2>
        {recipeName && <p className="text-amber-100 text-sm mt-0.5 truncate">{recipeName}</p>}
      </div>

      {/* ── Scrollable content ── */}
      <div className="flex-1 overflow-y-auto px-5 py-4 space-y-5 guided-scrollbar">

        {/* Not started */}
        {!sessionId && (
          <div className="flex flex-col items-center justify-center py-12 text-center gap-4">
            <div className="w-20 h-20 bg-amber-100 rounded-full flex items-center justify-center text-4xl shadow-inner">
              🚀
            </div>
            <h3 className="text-xl font-bold text-stone-700">Gotowy do gotowania?</h3>
            <p className="text-stone-500 text-sm max-w-xs">
              Rozpocznij symulację krok po kroku. Każdy krok wykonasz jednym kliknięciem.
            </p>
            <button
              id="simulator-start-btn"
              onClick={handleStart}
              disabled={isLoading}
              className="mt-2 px-8 py-3 bg-gradient-to-r from-amber-500 to-orange-500 text-white font-bold rounded-full shadow-lg hover:shadow-xl hover:scale-105 transition-all duration-300 disabled:opacity-50 disabled:cursor-not-allowed disabled:hover:scale-100"
            >
              {isLoading ? (
                <span className="flex items-center gap-2">
                  <svg className="animate-spin h-5 w-5" viewBox="0 0 24 24" fill="none">
                    <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
                    <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z" />
                  </svg>
                  Ładowanie…
                </span>
              ) : (
                'Rozpocznij gotowanie'
              )}
            </button>
          </div>
        )}

        {/* Error */}
        {error && (
          <div className="bg-red-50 border border-red-200 rounded-xl px-4 py-3 text-red-600 text-sm flex items-start gap-2">
            <span className="text-lg leading-none flex-shrink-0">⚠️</span>
            <div className="flex-1">
              <p className="font-semibold">Wystąpił błąd</p>
              <p className="mt-0.5">{error}</p>
            </div>
            <button
              onClick={() => setError(null)}
              className="text-red-400 hover:text-red-600 transition-colors flex-shrink-0 p-1"
              aria-label="Zamknij"
            >
              ✕
            </button>
          </div>
        )}

        {/* Session active */}
        {sessionId && status && (
          <>
            {/* Progress bar */}
            <div className="space-y-2">
              <div className="flex justify-between text-sm text-stone-500">
                <span>Krok {currentStep} z {totalSteps}</span>
                <span className={`font-semibold ${isCompleted ? 'text-green-600' : 'text-amber-600'}`}>
                  {isCompleted ? '✅ Ukończono' : '🔄 W trakcie'}
                </span>
              </div>
              <div className="w-full bg-stone-200 rounded-full h-2.5 overflow-hidden">
                <div
                  className="bg-gradient-to-r from-amber-400 to-orange-500 h-2.5 rounded-full transition-all duration-500 ease-out"
                  style={{ width: `${progressPercent}%` }}
                />
              </div>
            </div>

            {/* Status message */}
            {status.message && (
              <div className="bg-amber-50 border border-amber-200 rounded-xl px-4 py-3 text-amber-700 text-sm">
                💬 {status.message}
              </div>
            )}

            {/* Step history */}
            <div className="space-y-2">
              <h4 className="text-sm font-bold text-stone-600 uppercase tracking-wider">Historia kroków</h4>
              {history.length === 0 && (
                <p className="text-stone-400 text-sm italic">Brak kroków.</p>
              )}
              <div className="space-y-2">
                {history.map((step) => {
                  const info = STATUS_LABEL[step.status] ?? { label: step.status, emoji: '❓' };
                  const isCurrent = step.stepNumber === currentStep + 1 && step.status === 'PENDING';
                  return (
                    <div
                      key={step.stepNumber}
                      className={`flex items-center gap-3 p-3 rounded-xl border transition-all duration-300
                        ${isCurrent
                          ? 'border-amber-300 bg-amber-50 shadow-sm ring-2 ring-amber-200'
                          : step.status === 'EXECUTED'
                            ? 'border-green-200 bg-green-50/50'
                            : 'border-stone-200 bg-white'
                        }`}
                    >
                      <span className="text-xl">{info.emoji}</span>
                      <div className="flex-1 min-w-0">
                        <p className="text-sm font-semibold text-stone-700 truncate">
                          Krok {step.stepNumber}{step.recipeName && ` – ${step.recipeName}`}
                        </p>
                        <p className="text-xs text-stone-400">
                          {info.label}{step.preparationTime && ` • ${step.preparationTime}`}
                        </p>
                      </div>
                      {step.status === 'EXECUTED' && step.executedAt && (
                        <span className="text-[10px] text-stone-400 whitespace-nowrap">
                          {new Date(step.executedAt).toLocaleTimeString('pl-PL', { hour: '2-digit', minute: '2-digit' })}
                        </span>
                      )}
                    </div>
                  );
                })}
              </div>
            </div>

            {/* Actions */}
            <div className="pt-2 flex flex-col gap-3 border-t border-stone-200">
              {!isCompleted && (
                <button
                  id="simulator-execute-btn"
                  onClick={handleExecuteNext}
                  disabled={isLoading}
                  className="w-full py-3 bg-gradient-to-r from-amber-500 to-orange-500 text-white font-bold rounded-xl shadow-md hover:shadow-lg hover:scale-[1.02] active:scale-[0.98] transition-all duration-200 disabled:opacity-50 disabled:cursor-not-allowed disabled:hover:scale-100 flex items-center justify-center gap-2"
                >
                  {isLoading ? (
                    <>
                      <svg className="animate-spin h-5 w-5" viewBox="0 0 24 24" fill="none">
                        <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
                        <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z" />
                      </svg>
                      Wykonywanie…
                    </>
                  ) : (
                    <>▶️ Potwierdź – wykonaj następny krok</>
                  )}
                </button>
              )}

              {isCompleted && (
                <div className="text-center py-4">
                  <div className="text-5xl mb-3">🎉</div>
                  <h3 className="text-lg font-bold text-stone-700">Gratulacje!</h3>
                  <p className="text-stone-500 text-sm mt-1">Wszystkie kroki zostały wykonane. Smacznego!</p>
                </div>
              )}

              <button
                id="simulator-reset-btn"
                onClick={handleReset}
                className="w-full py-2 text-sm text-stone-500 hover:text-red-500 font-medium rounded-xl border border-stone-200 hover:border-red-300 hover:bg-red-50 transition-all duration-200"
              >
                🔄 Resetuj symulację
              </button>
            </div>
          </>
        )}
      </div>
    </div>
  );
}
