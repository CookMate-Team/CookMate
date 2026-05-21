import { useGuidedCookingContext } from '../context/GuidedCookingProvider';
import type { ActiveCookingSession, CookingSessionProgressItem } from '../types/simulator';

interface UseSimulationProgressReturn {
  activeSession: ActiveCookingSession | null;
  currentStep: number;
  sessionProgress: CookingSessionProgressItem[];
  isStreaming: boolean;
  startStreaming: (recipeId: string) => void;
  stopStreaming: () => void;
  resetSimulationProgress: () => void;
  updateActiveSession: (session: ActiveCookingSession | null) => void;
}

export const useSimulationProgress = (): UseSimulationProgressReturn => {
  const {
    activeSession,
    currentStep,
    sessionProgress,
    isStreaming,
    startStreaming,
    stopStreaming,
    resetSimulationProgress,
    updateActiveSession,
  } = useGuidedCookingContext();

  return {
    activeSession,
    currentStep,
    sessionProgress,
    isStreaming,
    startStreaming,
    stopStreaming,
    resetSimulationProgress,
    updateActiveSession,
  };
};
