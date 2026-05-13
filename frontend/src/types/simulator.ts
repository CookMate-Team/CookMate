/**
 * Types matching the simulator-service Java DTOs.
 */

export interface StartSimulationRequest {
  recipeId: string;
}

export interface StepExecutionResult {
  stepNumber: number;
  success: boolean;
}

export interface SimulationStepHistoryItem {
  stepNumber: number;
  recipeId: number | null;
  recipeName: string;
  preparationTime: string;
  status: 'PENDING' | 'EXECUTED';
  executedAt: string | null;
}

export interface SimulationStatusResponse {
  sessionId: string;
  status: 'RUNNING' | 'COMPLETED' | 'CREATED';
  currentStep: number;
  totalSteps: number;
  totalRecipes: number;
  message: string;
  history: SimulationStepHistoryItem[];
}
