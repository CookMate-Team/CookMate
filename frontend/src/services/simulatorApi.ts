import type {
  StartSimulationRequest,
  StepExecutionResult,
  SimulationStatusResponse,
  SimulationStepHistoryItem,
  CookingSessionProgressItem,
  ActiveCookingSession,
} from '../types/simulator';
import { authFetch } from './authFetch';
import keycloak from '../auth/keycloak';

const API_BASE_URL = '/api/simulator';
const MAIN_API_URL = '/api';
const COOKING_SESSION_API_URL = '/api/cooking-sessions';

/**
 * Generate cooking steps for a TheMealDB recipe via Groq LLM.
 * Must be called before starting a simulation for discovery recipes
 * so that main-service has the steps in its database.
 */
export const generateSteps = async (mealId: string): Promise<void> => {
  const response = await authFetch(`${MAIN_API_URL}/steps/generate`, {
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
  const response = await authFetch(`${API_BASE_URL}/sessions/start`, {
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
  const response = await authFetch(
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
  const response = await authFetch(
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
  const response = await authFetch(
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
  const response = await authFetch(
    `${API_BASE_URL}/sessions/${sessionId}/rewind?stepNumber=${stepNumber}`,
    { method: 'POST' }
  );
  if (!response.ok) {
    const errorBody = await response.text();
    throw new Error(errorBody || 'Failed to rewind simulation');
  }
  return response.json();
};

/** Get active cooking session for recipe (or null if none). */
export const getActiveCookingSession = async (
  recipeId: string
): Promise<ActiveCookingSession | null> => {
  const response = await authFetch(
    `${COOKING_SESSION_API_URL}/recipes/${recipeId}/active`
  );
  if (response.status === 404) {
    return null;
  }
  if (!response.ok) {
    const errorBody = await response.text();
    throw new Error(errorBody || 'Failed to get active cooking session');
  }
  return response.json();
};

/** Get cooking session progress history for recipe. */
export const getCookingSessionHistory = async (
  recipeId: string
): Promise<CookingSessionProgressItem[]> => {
  const response = await authFetch(
    `${COOKING_SESSION_API_URL}/recipes/${recipeId}/history`
  );
  if (response.status === 404) {
    return [];
  }
  if (!response.ok) {
    const errorBody = await response.text();
    throw new Error(errorBody || 'Failed to get cooking session history');
  }
  return response.json();
};

/** Get global active cooking session (or null if none). */
export const getActiveCookingSessionGlobal = async (): Promise<ActiveCookingSession | null> => {
  const response = await authFetch(`${COOKING_SESSION_API_URL}/active`);
  if (response.status === 404) {
    return null;
  }
  if (!response.ok) {
    const errorBody = await response.text();
    throw new Error(errorBody || 'Failed to get global active cooking session');
  }
  const text = await response.text();
  if (!text || text === 'null') {
    return null;
  }
  return JSON.parse(text);
};

/** Manually complete/reset active simulation session (simulator-service). */
export const completeSimulationSession = async (
  sessionId: string
): Promise<void> => {
  const response = await authFetch(`${API_BASE_URL}/sessions/${sessionId}/complete`, {
    method: 'POST',
  });
  if (!response.ok) {
    const errorBody = await response.text();
    throw new Error(errorBody || 'Failed to complete session');
  }
};

/** Manually complete session in cooking-session-service directly. */
export const completeCookingSession = async (
  sessionId: string
): Promise<void> => {
  const response = await authFetch(`${COOKING_SESSION_API_URL}/sessions/${sessionId}/complete`, {
    method: 'POST',
  });
  if (!response.ok) {
    const errorBody = await response.text();
    throw new Error(errorBody || 'Failed to complete cooking session');
  }
};

/**
 * Buduje URL do SSE stream z tokenem jako query param.
 * EventSource nie obsługuje custom headers — token przekazywany przez ?token=.
 * Gateway/serwisy muszą wspierać ten parametr LUB używamy proxy z ciasteczkiem sesji.
 */
export function buildSseUrl(recipeId: string): string {
  const token = keycloak.token;
  const base = `${COOKING_SESSION_API_URL}/recipes/${recipeId}/stream`;
  return token ? `${base}?token=${encodeURIComponent(token)}` : base;
}
