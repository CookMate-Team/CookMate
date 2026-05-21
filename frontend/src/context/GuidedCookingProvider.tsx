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
import { getActiveCookingSession, getCookingSessionHistory } from '../services/simulatorApi';
import type { ActiveCookingSession, CookingSessionProgressItem } from '../types/simulator';

interface GuidedCookingContextValue {
  activeSession: ActiveCookingSession | null;
  currentStep: number;
  sessionProgress: CookingSessionProgressItem[];
  isStreaming: boolean;
  startStreaming: (recipeId: string) => void;
  stopStreaming: () => void;
  resetSimulationProgress: () => void;
  updateActiveSession: (session: ActiveCookingSession | null) => void;
}

const GuidedCookingContext = createContext<GuidedCookingContextValue | undefined>(undefined);

export function GuidedCookingProvider({ children }: PropsWithChildren) {
  const [activeSession, setActiveSession] = useState<ActiveCookingSession | null>(null);
  const [sessionProgress, setSessionProgress] = useState<CookingSessionProgressItem[]>([]);
  const [isStreaming, setIsStreaming] = useState(false);
  const eventSourceRef = useRef<EventSource | null>(null);
  const activeRecipeIdRef = useRef<string | null>(null);

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
          status: 'RUNNING',
          currentStep: progress.stepNumber,
          lastExecutedAt: progress.executedAt,
        };
      }
      const nextStep = Math.max(prev.currentStep ?? 0, progress.stepNumber);
      return {
        ...prev,
        currentStep: nextStep,
        lastExecutedAt: progress.executedAt,
      };
    });
  }, []);

  const startStreaming = useCallback(
    async (recipeId: string) => {
      if (activeRecipeIdRef.current === recipeId && eventSourceRef.current) {
        return;
      }

      stopStreaming();
      activeRecipeIdRef.current = recipeId;
      setIsStreaming(true);

      try {
        const active = await getActiveCookingSession(recipeId);
        setActiveSession(active);
      } catch (error) {
        console.error('Error loading active cooking session:', error);
        setActiveSession(null);
      }

      try {
        const history = await getCookingSessionHistory(recipeId);
        setSessionProgress(history);
      } catch (error) {
        console.error('Error loading cooking session history:', error);
        setSessionProgress([]);
      }

      const source = new EventSource(`/api/cooking-sessions/recipes/${recipeId}/stream`);
      eventSourceRef.current = source;

      source.addEventListener('progress', (event) => {
        try {
          const parsed = JSON.parse(event.data) as CookingSessionProgressItem;
          mergeProgress(parsed);
        } catch (error) {
          console.error('Error parsing SSE progress payload:', error);
        }
      });

      source.onerror = (event) => {
        console.error('Cooking session SSE error:', event);
      };
    },
    [mergeProgress, stopStreaming]
  );

  const resetSimulationProgress = useCallback(() => {
    stopStreaming();
    setActiveSession(null);
    setSessionProgress([]);
  }, [stopStreaming]);

  useEffect(() => {
    return () => stopStreaming();
  }, [stopStreaming]);

  const currentStep = useMemo(() => {
    if (!activeSession) {
      return 0;
    }

    const executedSteps = sessionProgress
      .filter((item) => item.status === 'EXECUTED')
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
      startStreaming,
      stopStreaming,
      resetSimulationProgress,
      updateActiveSession,
    }),
    [
      activeSession,
      currentStep,
      sessionProgress,
      isStreaming,
      startStreaming,
      stopStreaming,
      resetSimulationProgress,
      updateActiveSession,
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
