/**
 * Types matching the simulator-service Java DTOs.
 */

export interface StartSimulationRequest {
  recipeId: string;
  targetPortions?: number;
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

export interface CookingSessionProgressItem {
  sessionId: string;
  recipeId: string;
  stepNumber: number;
  status: string;
  executedAt: string;
}

export interface ActiveCookingSession {
  sessionId: string;
  recipeId: string;
  status: string;
  currentStep: number;
  lastExecutedAt: string | null;
  targetPortions?: number;
}

export interface SimulationStatusResponse {
  sessionId: string;
  status: 'RUNNING' | 'COMPLETED' | 'CREATED';
  currentStep: number;
  totalSteps: number;
  totalRecipes: number;
  message: string;
  history: SimulationStepHistoryItem[];
  targetPortions?: number;
}
