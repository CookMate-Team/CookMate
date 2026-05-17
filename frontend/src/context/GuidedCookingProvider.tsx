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
import { getSimulationProgress } from '../services/simulatorApi';
import type { SimulationStepHistoryItem } from '../types/simulator';

interface GuidedCookingContextValue {
  sessionId: string | null;
  currentStep: number;
  mainServiceProgress: SimulationStepHistoryItem[];
  isPolling: boolean;
  startPolling: (sessionId: string) => void;
  stopPolling: () => void;
  resetSimulationProgress: () => void;
}

const GuidedCookingContext = createContext<GuidedCookingContextValue | undefined>(undefined);

export function GuidedCookingProvider({ children }: PropsWithChildren) {
  const [sessionId, setSessionId] = useState<string | null>(null);
  const [mainServiceProgress, setMainServiceProgress] = useState<SimulationStepHistoryItem[]>([]);
  const [isPolling, setIsPolling] = useState(false);
  const pollIntervalRef = useRef<number | null>(null);

  const pollProgress = useCallback(async (sid: string) => {
    try {
      const progress = await getSimulationProgress(sid);
      setMainServiceProgress(progress);
    } catch (error) {
      console.error('Error polling main-service progress:', error);
    }
  }, []);

  const startPolling = useCallback(
    (sid: string) => {
      if (pollIntervalRef.current) {
        clearInterval(pollIntervalRef.current);
      }

      setSessionId(sid);
      setIsPolling(true);
      void pollProgress(sid);

      pollIntervalRef.current = window.setInterval(() => {
        void pollProgress(sid);
      }, 2000);
    },
    [pollProgress]
  );

  const stopPolling = useCallback(() => {
    if (pollIntervalRef.current) {
      clearInterval(pollIntervalRef.current);
      pollIntervalRef.current = null;
    }
    setIsPolling(false);
  }, []);

  const resetSimulationProgress = useCallback(() => {
    stopPolling();
    setSessionId(null);
    setMainServiceProgress([]);
  }, [stopPolling]);

  useEffect(() => {
    return () => stopPolling();
  }, [stopPolling]);

  const currentStep = useMemo(() => {
    if (!sessionId) {
      return 0;
    }

    const executedSteps = mainServiceProgress
      .filter((item) => item.status === 'EXECUTED')
      .map((item) => item.stepNumber);

    if (executedSteps.length === 0) {
      return 1;
    }

    return Math.max(...executedSteps) + 1;
  }, [mainServiceProgress, sessionId]);

  const value = useMemo(
    () => ({
      sessionId,
      currentStep,
      mainServiceProgress,
      isPolling,
      startPolling,
      stopPolling,
      resetSimulationProgress,
    }),
    [
      sessionId,
      currentStep,
      mainServiceProgress,
      isPolling,
      startPolling,
      stopPolling,
      resetSimulationProgress,
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
