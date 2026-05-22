import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useRef,
  useState,
  type PropsWithChildren,
} from 'react';
import { useQueryClient } from '@tanstack/react-query';
import {
  getCookingSessionHistory,
  getActiveCookingSessionGlobal,
  generateSteps,
  startSimulation,
  completeSimulationSession,
  completeCookingSession,
} from '../services/simulatorApi';
import type { ActiveCookingSession, CookingSessionProgressItem } from '../types/simulator';

interface GuidedCookingContextValue {
  activeSession: ActiveCookingSession | null;
  currentStep: number;
  sessionProgress: CookingSessionProgressItem[];
  isStreaming: boolean;
  globalActiveSessionForOtherRecipe: ActiveCookingSession | null;
  isStartingSession: boolean;
  startSessionError: string | null;
  startStreaming: (recipeId: string) => void;
  stopStreaming: () => void;
  resetSimulationProgress: () => void;
  updateActiveSession: (session: ActiveCookingSession | null) => void;
  forceResetActiveSession: () => Promise<void>;
}

const GuidedCookingContext = createContext<GuidedCookingContextValue | undefined>(undefined);

export function GuidedCookingProvider({ children }: PropsWithChildren) {
  const [activeSession, setActiveSession] = useState<ActiveCookingSession | null>(null);
  const [sessionProgress, setSessionProgress] = useState<CookingSessionProgressItem[]>([]);
  const [isStreaming, setIsStreaming] = useState(false);
  const [globalActiveSessionForOtherRecipe, setGlobalActiveSessionForOtherRecipe] = useState<ActiveCookingSession | null>(null);
  const [isStartingSession, setIsStartingSession] = useState(false);
  const [startSessionError, setStartSessionError] = useState<string | null>(null);

  const eventSourceRef = useRef<EventSource | null>(null);
  const activeRecipeIdRef = useRef<string | null>(null);
  const queryClient = useQueryClient();

  const updateActiveSession = useCallback((session: ActiveCookingSession | null) => {
    setActiveSession(session);
  }, []);

  const stopStreaming = useCallback(() => {
    if (eventSourceRef.current) {
      eventSourceRef.current.close();
      eventSourceRef.current = null;
    }
    activeRecipeIdRef.current = null;
    setIsStreaming(false);
  }, []);

  const mergeProgress = useCallback((progress: CookingSessionProgressItem) => {
    setSessionProgress((prev) => {
      const existingIndex = prev.findIndex((item) => item.stepNumber === progress.stepNumber);
      if (existingIndex >= 0) {
        const next = [...prev];
        next[existingIndex] = progress;
        return next;
      }
      return [...prev, progress].sort((a, b) => a.stepNumber - b.stepNumber);
    });

    setActiveSession((prev) => {
      if (!prev) {
        return {
          sessionId: progress.sessionId,
          recipeId: progress.recipeId,
          status: progress.status === 'COMPLETED' ? 'COMPLETED' : 'RUNNING',
          currentStep: progress.stepNumber,
          lastExecutedAt: progress.executedAt,
        };
      }
      const nextStep = Math.max(prev.currentStep ?? 0, progress.stepNumber);
      return {
        ...prev,
        status: progress.status === 'COMPLETED' ? 'COMPLETED' : prev.status,
        currentStep: nextStep,
        lastExecutedAt: progress.executedAt,
      };
    });
  }, []);

  const startStreaming = useCallback(
    async (recipeId: string) => {
      if (activeRecipeIdRef.current === recipeId) {
        return;
      }

      stopStreaming();
      activeRecipeIdRef.current = recipeId;
      setActiveSession(null);
      setSessionProgress([]);
      setGlobalActiveSessionForOtherRecipe(null);
      setStartSessionError(null);
      setIsStartingSession(true);

      try {
        const globalActive = await getActiveCookingSessionGlobal();
        if (activeRecipeIdRef.current !== recipeId) {
          return;
        }

        if (globalActive) {
          if (globalActive.recipeId === recipeId) {
            setActiveSession(globalActive);
            const history = await getCookingSessionHistory(recipeId);
            if (activeRecipeIdRef.current === recipeId) {
              setSessionProgress(history);
            }
          } else {
            setGlobalActiveSessionForOtherRecipe(globalActive);
          }
          setIsStartingSession(false);
        } else {
          try {
            await generateSteps(recipeId);
            if (activeRecipeIdRef.current !== recipeId) {
              return;
            }
            queryClient.invalidateQueries({ queryKey: ['recipe-steps', recipeId] });

            const res = await startSimulation({ recipeId });
            if (activeRecipeIdRef.current !== recipeId) {
              return;
            }

            const newSession: ActiveCookingSession = {
              sessionId: res.sessionId,
              recipeId,
              status: res.status,
              currentStep: res.currentStep,
              lastExecutedAt: null,
            };
            setActiveSession(newSession);
          } catch (err: any) {
            setStartSessionError(err.message || 'Failed to start guided cooking automatically');
          } finally {
            setIsStartingSession(false);
          }
        }
      } catch (error: any) {
        setStartSessionError(error.message || 'Error checking session status');
        setIsStartingSession(false);
      }
    },
    [stopStreaming, queryClient]
  );

  const forceResetActiveSession = useCallback(async () => {
    const currentRecipeId = activeRecipeIdRef.current;
    if (!currentRecipeId) return;

    setIsStartingSession(true);
    setStartSessionError(null);

    try {
      const targetSession = globalActiveSessionForOtherRecipe;
      if (targetSession) {
        const cleanupResults = await Promise.allSettled([
          completeSimulationSession(targetSession.sessionId),
          completeCookingSession(targetSession.sessionId),
        ]);

        const cleanupErrors = cleanupResults
          .filter((result): result is PromiseRejectedResult => result.status === 'rejected')
          .map((result) =>
            result.reason instanceof Error
              ? result.reason.message
              : String(result.reason ?? 'Unknown cleanup error')
          );

        if (cleanupErrors.length > 0) {
          throw new Error(`Failed to complete existing session cleanup: ${cleanupErrors.join('; ')}`);
        }
      }

      setGlobalActiveSessionForOtherRecipe(null);
      setActiveSession(null);
      setSessionProgress([]);

      await generateSteps(currentRecipeId);
      queryClient.invalidateQueries({ queryKey: ['recipe-steps', currentRecipeId] });

      const res = await startSimulation({ recipeId: currentRecipeId });
      const newSession: ActiveCookingSession = {
        sessionId: res.sessionId,
        recipeId: currentRecipeId,
        status: res.status,
        currentStep: res.currentStep,
        lastExecutedAt: null,
      };
      setActiveSession(newSession);
    } catch (err: any) {
      setStartSessionError(err.message || 'Failed to force reset and start session');
    } finally {
      setIsStartingSession(false);
    }
  }, [globalActiveSessionForOtherRecipe, queryClient]);

  const resetSimulationProgress = useCallback(() => {
    stopStreaming();
    setActiveSession(null);
    setSessionProgress([]);
    setGlobalActiveSessionForOtherRecipe(null);
    setStartSessionError(null);
  }, [stopStreaming]);

  useEffect(() => {
    if (activeSession && activeSession.status === 'RUNNING') {
      const recipeId = activeSession.recipeId;
      setIsStreaming(true);

      const source = new EventSource(`/api/cooking-sessions/recipes/${recipeId}/stream`);
      eventSourceRef.current = source;

      source.addEventListener('progress', (event) => {
        try {
          const parsed = JSON.parse(event.data) as CookingSessionProgressItem;
          if (activeRecipeIdRef.current === parsed.recipeId) {
            mergeProgress(parsed);
          }
        } catch (error) {
          console.error('Error parsing SSE progress payload:', error);
        }
      });

      source.onerror = (event) => {
        if (eventSourceRef.current) {
          console.warn('Cooking session SSE connection state change:', event);
        }
      };

      return () => {
        if (source) {
          source.close();
        }
        if (eventSourceRef.current === source) {
          eventSourceRef.current = null;
        }
        setIsStreaming(false);
      };
    } else {
      if (eventSourceRef.current) {
        eventSourceRef.current.close();
        eventSourceRef.current = null;
      }
      setIsStreaming(false);
    }
  }, [activeSession?.sessionId, activeSession?.status, mergeProgress]);

  const currentStep = useMemo(() => {
    if (!activeSession) {
      return 0;
    }

    const executedSteps = sessionProgress
      .filter((item) => item.status === 'EXECUTED' || item.status === 'COMPLETED')
      .map((item) => item.stepNumber);

    const lastExecuted = executedSteps.length > 0
      ? Math.max(...executedSteps)
      : activeSession.currentStep ?? 0;

    return lastExecuted <= 0 ? 1 : lastExecuted + 1;
  }, [activeSession, sessionProgress]);

  const value = useMemo(
    () => ({
      activeSession,
      currentStep,
      sessionProgress,
      isStreaming,
      globalActiveSessionForOtherRecipe,
      isStartingSession,
      startSessionError,
      startStreaming,
      stopStreaming,
      resetSimulationProgress,
      updateActiveSession,
      forceResetActiveSession,
    }),
    [
      activeSession,
      currentStep,
      sessionProgress,
      isStreaming,
      globalActiveSessionForOtherRecipe,
      isStartingSession,
      startSessionError,
      startStreaming,
      stopStreaming,
      resetSimulationProgress,
      updateActiveSession,
      forceResetActiveSession,
    ]
  );

  return <GuidedCookingContext.Provider value={value}>{children}</GuidedCookingContext.Provider>;
}

export function useGuidedCookingContext(): GuidedCookingContextValue {
  const context = useContext(GuidedCookingContext);
  if (!context) {
    throw new Error('useGuidedCookingContext must be used within GuidedCookingProvider');
  }
  return context;
}
