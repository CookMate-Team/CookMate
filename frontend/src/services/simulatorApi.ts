import type {
  StartSimulationRequest,
  StepExecutionResult,
  SimulationStatusResponse,
  SimulationStepHistoryItem,
} from '../types/simulator';

const API_BASE_URL = '/api/simulator';
const MAIN_API_URL = '/api';

/**
 * Generate cooking steps for a TheMealDB recipe via Groq LLM.
 * Must be called before starting a simulation for discovery recipes
 * so that main-service has the steps in its database.
 */
export const generateSteps = async (mealId: string): Promise<void> => {
  const response = await fetch(`${MAIN_API_URL}/steps/generate`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ mealId }),
  });
  if (!response.ok) {
    const errorBody = await response.text();
    throw new Error(errorBody || 'Failed to generate recipe steps');
  }
};

/** Start a new simulation session for a given recipe. */
export const startSimulation = async (
  request: StartSimulationRequest
): Promise<SimulationStatusResponse> => {
  const response = await fetch(`${API_BASE_URL}/sessions/start`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(request),
  });
  if (!response.ok) {
    const errorBody = await response.text();
    throw new Error(errorBody || 'Failed to start simulation');
  }
  return response.json();
};

/** Execute the next pending step in a session (one-click). */
export const executeNextStep = async (
  sessionId: string
): Promise<StepExecutionResult> => {
  const response = await fetch(
    `${API_BASE_URL}/sessions/${sessionId}/steps/execute`,
    { method: 'POST' }
  );
  if (!response.ok) {
    const errorBody = await response.text();
    throw new Error(errorBody || 'Failed to execute step');
  }
  return response.json();
};

/** Get the current status of a simulation session. */
export const getSimulationStatus = async (
  sessionId: string
): Promise<SimulationStatusResponse> => {
  const response = await fetch(
    `${API_BASE_URL}/sessions/${sessionId}/status`
  );
  if (!response.ok) {
    const errorBody = await response.text();
    throw new Error(errorBody || 'Failed to get session status');
  }
  return response.json();
};

/** Get step history for a simulation session. */
export const getSimulationHistory = async (
  sessionId: string
): Promise<SimulationStepHistoryItem[]> => {
  const response = await fetch(
    `${API_BASE_URL}/sessions/${sessionId}/history`
  );
  if (!response.ok) {
    const errorBody = await response.text();
    throw new Error(errorBody || 'Failed to get history');
  }
  return response.json();
};

/** Rewind the session to a specific step number. */
export const rewindSimulation = async (
  sessionId: string,
  stepNumber: number
): Promise<SimulationStatusResponse> => {
  const response = await fetch(
    `${API_BASE_URL}/sessions/${sessionId}/rewind?stepNumber=${stepNumber}`,
    { method: 'POST' }
  );
  if (!response.ok) {
    const errorBody = await response.text();
    throw new Error(errorBody || 'Failed to rewind simulation');
  }
  return response.json();
};

/** Get simulation progress history from main-service. */
export const getSimulationProgress = async (
  sessionId: string
): Promise<SimulationStepHistoryItem[]> => {
  const response = await fetch(
    `${MAIN_API_URL}/simulation-progress/sessions/${sessionId}`
  );
  if (!response.ok) {
    const errorBody = await response.text();
    throw new Error(errorBody || 'Failed to get progress from main-service');
  }
  return response.json();
};
