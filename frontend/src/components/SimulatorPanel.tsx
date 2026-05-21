import { useState, useCallback, useEffect, useRef } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import {
  startSimulation,
  executeNextStep,
  getSimulationStatus,
  generateSteps,
} from '../services/simulatorApi';
import { useSimulationProgress } from '../hooks/useSimulationProgress';
import type { SimulationStatusResponse } from '../types/simulator';

const STATUS_LABEL: Record<string, { label: string; emoji: string }> = {
  PENDING: { label: 'Pending', emoji: '⏳' },
  EXECUTED: { label: 'Executed', emoji: '✅' },
};

/** Map known backend error codes to user-friendly Polish messages. */
const ERROR_MESSAGES: Record<string, string> = {
  MAIN_SERVICE_UNAVAILABLE:
    'Recipe service is unavailable. Make sure the recipe has defined cooking steps.',
  SESSION_NOT_FOUND: 'Simulation session not found. Try starting over.',
  INVALID_STATE: 'Invalid simulation state. Try resetting the session.',
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

function toScaledSeconds(preparationTime?: string | null): number | null {
  if (!preparationTime) {
    return null;
  }

  const normalized = preparationTime.toLowerCase();
  const minutesMatch = normalized.match(/(\d+)\s*(minute|minutes|min)/);
  if (minutesMatch) {
    return Math.max(1, Number(minutesMatch[1]));
  }

  const secondsMatch = normalized.match(/(\d+)\s*(second|seconds|sec)/);
  if (secondsMatch) {
    const seconds = Number(secondsMatch[1]);
    return Math.max(1, Math.ceil(seconds / 60));
  }

  const fallback = Number.parseInt(normalized, 10);
  return Number.isNaN(fallback) ? null : Math.max(1, fallback);
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
  const [remainingSeconds, setRemainingSeconds] = useState<number | null>(null);
  const [stepDurationSeconds, setStepDurationSeconds] = useState<number | null>(null);
  const [isCounting, setIsCounting] = useState(false);
  const autoAdvanceStepRef = useRef<number | null>(null);
  const {
    activeSession,
    resetSimulationProgress,
    updateActiveSession,
  } = useSimulationProgress();
  const queryClient = useQueryClient();
  const isCompleted = status?.status === 'COMPLETED';
  const currentStep = status?.currentStep ?? 0;
  const totalSteps = status?.totalSteps ?? 0;
  const progressPercent = totalSteps > 0 ? (currentStep / totalSteps) * 100 : 0;
  const history = status?.history ?? [];
  const displayStepNumber = status
    ? isCompleted
      ? currentStep
      : Math.min(currentStep + 1, totalSteps || currentStep + 1)
    : 0;
  const activeStep = history.find((step) => step.stepNumber === displayStepNumber) ?? null;
  const timeProgressPercent =
    stepDurationSeconds && remainingSeconds !== null
      ? Math.min(100, Math.max(0, ((stepDurationSeconds - remainingSeconds) / stepDurationSeconds) * 100))
      : 0;

  useEffect(() => {
    if (!activeSession || sessionId || activeSession.recipeId !== recipeId) {
      return;
    }

    setSessionId(activeSession.sessionId);
    setIsLoading(true);
    getSimulationStatus(activeSession.sessionId)
      .then((updated) => setStatus(updated))
      .catch((err: any) => setError(parseApiError(err.message ?? 'Failed to load active session')))
      .finally(() => setIsLoading(false));
  }, [activeSession, recipeId, sessionId]);

  // ── Start session ──
  const handleStart = useCallback(async () => {
    setIsLoading(true);
    setError(null);
    try {
      // First, ensure steps exist (generates via LLM if needed)
      setLoadingMessage('Generating recipe steps...');
      await generateSteps(recipeId);
      
      // Invalidate recipe steps so the UI fetches the newly generated steps
      queryClient.invalidateQueries({ queryKey: ['recipe-steps', recipeId] });

      // Then start the simulation session
      setLoadingMessage('Starting simulation...');
      const res = await startSimulation({ recipeId });
      setSessionId(res.sessionId);
      setStatus(res);
      updateActiveSession({
        sessionId: res.sessionId,
        recipeId,
        status: res.status,
        currentStep: res.currentStep,
        lastExecutedAt: null,
      });
    } catch (err: any) {
      setError(parseApiError(err.message ?? 'Failed to start simulation'));
    } finally {
      setIsLoading(false);
      setLoadingMessage(null);
    }
  }, [recipeId, queryClient, updateActiveSession]);

  // ── Execute next step ──
  const handleExecuteNext = useCallback(async () => {
    if (!sessionId || isLoading) return;
    setIsCounting(false);
    setRemainingSeconds(0);
    setIsLoading(true);
    setError(null);
    try {
      await executeNextStep(sessionId);
      const updated = await getSimulationStatus(sessionId);
      setStatus(updated);
    } catch (err: any) {
      setError(parseApiError(err.message ?? 'Step execution error'));
    } finally {
      setIsLoading(false);
    }
  }, [isLoading, sessionId]);

  useEffect(() => {
    autoAdvanceStepRef.current = null;

    if (!activeStep || isCompleted) {
      setRemainingSeconds(null);
      setStepDurationSeconds(null);
      setIsCounting(false);
      return;
    }

    const scaledSeconds = toScaledSeconds(activeStep.preparationTime);
    if (!scaledSeconds) {
      setRemainingSeconds(null);
      setStepDurationSeconds(null);
      setIsCounting(false);
      return;
    }

    setStepDurationSeconds(scaledSeconds);
    setRemainingSeconds(scaledSeconds);
    setIsCounting(true);
  }, [activeStep?.preparationTime, activeStep?.stepNumber, isCompleted]);

  useEffect(() => {
    if (!isCounting) {
      return;
    }

    const timer = window.setInterval(() => {
      setRemainingSeconds((prev) => {
        if (prev === null) {
          return prev;
        }
        return prev <= 1 ? 0 : prev - 1;
      });
    }, 1000);

    return () => clearInterval(timer);
  }, [isCounting]);

  useEffect(() => {
    if (remainingSeconds !== 0) {
      return;
    }
    setIsCounting(false);
  }, [remainingSeconds]);

  useEffect(() => {
    if (!activeStep || isCompleted) {
      return;
    }
    if (remainingSeconds === null || remainingSeconds > 0) {
      return;
    }
    if (autoAdvanceStepRef.current === activeStep.stepNumber) {
      return;
    }

    autoAdvanceStepRef.current = activeStep.stepNumber;
    void handleExecuteNext();
  }, [activeStep, handleExecuteNext, isCompleted, remainingSeconds]);

  // ── Reset ──
  const handleReset = useCallback(() => {
    resetSimulationProgress();
    updateActiveSession(null);
    setSessionId(null);
    setStatus(null);
    setError(null);
  }, [resetSimulationProgress, updateActiveSession]);


  return (
    <div className="flex flex-col h-full bg-gradient-to-b from-stone-50 to-white">
      {/* ── Panel Header ── */}
      <div className="px-5 py-4 bg-gradient-to-r from-amber-500 to-orange-500 text-white shadow-md flex-shrink-0">
        <h2 className="text-lg font-bold tracking-tight">🍳 Cooking Simulator</h2>
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
            <h3 className="text-xl font-bold text-stone-700">Ready to cook?</h3>
            <p className="text-stone-500 text-sm max-w-xs">
              Start the step-by-step simulation. You will execute each step with one click.
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
                  Loading...
                </span>
              ) : (
                'Start cooking'
              )}
            </button>
          </div>
        )}

        {/* Error */}
        {error && (
          <div className="bg-red-50 border border-red-200 rounded-xl px-4 py-3 text-red-600 text-sm flex items-start gap-2">
            <span className="text-lg leading-none flex-shrink-0">⚠️</span>
            <div className="flex-1">
              <p className="font-semibold">An error occurred</p>
              <p className="mt-0.5">{error}</p>
            </div>
            <button
              onClick={() => setError(null)}
              className="text-red-400 hover:text-red-600 transition-colors flex-shrink-0 p-1"
              aria-label="Close"
            >
              ✕
            </button>
          </div>
        )}

        {/* Session active */}
        {sessionId && status && (
          <>
            {/* Current Step Indicator */}
            <div className="bg-gradient-to-r from-amber-50 to-orange-50 border-2 border-amber-200 rounded-xl px-4 py-3 flex items-center gap-3">
              <div className="text-3xl">📍</div>
              <div className="flex-1">
                <p className="text-xs text-amber-600 font-semibold uppercase tracking-wider">Current step</p>
                <p className="text-lg font-bold text-amber-900">
                  {isCompleted
                    ? '✨ Completed!'
                    : displayStepNumber > 0
                      ? `Step ${displayStepNumber} of ${totalSteps}`
                      : 'Waiting for first step...'}
                </p>
              </div>
            </div>

            {/* Progress bar */}
            <div className="space-y-2">
              <div className="flex justify-between text-sm text-stone-500">
                <span>Progress: {Math.round(progressPercent)}%</span>
                <span className={`font-semibold ${isCompleted ? 'text-green-600' : 'text-amber-600'}`}>
                  {isCompleted ? '✅ Completed' : '🔄 In progress'}
                </span>
              </div>
              <div className="w-full bg-stone-200 rounded-full h-2.5 overflow-hidden">
                <div
                  className="bg-gradient-to-r from-amber-400 to-orange-500 h-2.5 rounded-full transition-all duration-500 ease-out"
                  style={{ width: `${progressPercent}%` }}
                />
              </div>
            </div>

            {/* Active step */}
            <div className="space-y-2">
              <div className="flex items-center justify-between">
                <h4 className="text-sm font-bold text-stone-600 uppercase tracking-wider">Step details</h4>
                {activeStep?.preparationTime && (
                  <span className="text-xs bg-amber-100 text-amber-700 px-2 py-1 rounded-full font-semibold">
                    {activeStep.preparationTime}
                  </span>
                )}
              </div>

              {activeStep ? (
                <div className="rounded-xl border border-amber-200 bg-amber-50/40 px-4 py-3">
                  <p className="text-xs text-amber-600 font-semibold uppercase tracking-wider">
                    Step {displayStepNumber}
                  </p>
                  <p className="mt-1 text-base font-semibold text-stone-800">
                    {activeStep.recipeName}
                  </p>
                  <p className="mt-1 text-xs text-stone-500">
                    {STATUS_LABEL[activeStep.status]?.label ?? activeStep.status}
                  </p>
                </div>
              ) : (
                <p className="text-stone-400 text-sm italic">Waiting for step details.</p>
              )}
            </div>

            {/* Step timer */}
            <div className="space-y-2">
              <div className="flex justify-between text-sm text-stone-500">
                <span>
                  {remainingSeconds !== null ? `Time left: ${remainingSeconds}s` : 'Timer unavailable'}
                </span>
                {stepDurationSeconds !== null && <span>{stepDurationSeconds}s total</span>}
              </div>
              <div className="w-full bg-stone-200 rounded-full h-2.5 overflow-hidden">
                <div
                  className="bg-gradient-to-r from-emerald-400 to-amber-500 h-2.5 rounded-full transition-all duration-500 ease-out"
                  style={{ width: `${timeProgressPercent}%` }}
                />
              </div>
            </div>

            {/* Status message */}
            {status.message && (
              <div className="bg-amber-50 border border-amber-200 rounded-xl px-4 py-3 text-amber-700 text-sm">
                💬 {status.message}
              </div>
            )}

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
                      Executing...
                    </>
                  ) : (
                    <>▶️ Next step</>
                  )}
                </button>
              )}

              {isCompleted && (
                <div className="text-center py-4">
                  <div className="text-5xl mb-3">🎉</div>
                  <h3 className="text-lg font-bold text-stone-700">Congratulations!</h3>
                  <p className="text-stone-500 text-sm mt-1">All steps have been executed. Enjoy your meal!</p>
                </div>
              )}

              <button
                id="simulator-reset-btn"
                onClick={handleReset}
                className="w-full py-2 text-sm text-stone-500 hover:text-red-500 font-medium rounded-xl border border-stone-200 hover:border-red-300 hover:bg-red-50 transition-all duration-200"
              >
                🔄 Reset simulation
              </button>
            </div>
          </>
        )}
      </div>
    </div>
  );
}
