import { useGuidedCookingContext } from '../context/GuidedCookingProvider';
import type { ActiveCookingSession, CookingSessionProgressItem } from '../types/simulator';

interface UseSimulationProgressReturn {
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

export const useSimulationProgress = (): UseSimulationProgressReturn => {
  const {
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
  } = useGuidedCookingContext();

  return {
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
  };
};
