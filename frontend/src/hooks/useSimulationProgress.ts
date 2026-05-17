import { useGuidedCookingContext } from '../context/GuidedCookingProvider';
import type { SimulationStepHistoryItem } from '../types/simulator';

interface UseSimulationProgressReturn {
  sessionId: string | null;
  currentStep: number;
  mainServiceProgress: SimulationStepHistoryItem[];
  isPolling: boolean;
  startPolling: (sessionId: string) => void;
  stopPolling: () => void;
  resetSimulationProgress: () => void;
}

export const useSimulationProgress = (): UseSimulationProgressReturn => {
  const {
    sessionId,
    currentStep,
    mainServiceProgress,
    isPolling,
    startPolling,
    stopPolling,
    resetSimulationProgress,
  } = useGuidedCookingContext();

  return {
    sessionId,
    currentStep,
    mainServiceProgress,
    isPolling,
    startPolling,
    stopPolling,
    resetSimulationProgress,
  };
};
