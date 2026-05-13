import type {
  StartSimulationRequest,
  StepExecutionResult,
  SimulationStatusResponse,
  SimulationStepHistoryItem,
} from '../types/simulator';

const API_BASE_URL = '/api/simulator';

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
    throw new Error(errorBody || 'Nie udało się rozpocząć symulacji');
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
    throw new Error(errorBody || 'Nie udało się wykonać kroku');
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
    throw new Error(errorBody || 'Nie udało się pobrać statusu sesji');
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
    throw new Error(errorBody || 'Nie udało się pobrać historii');
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
    throw new Error(errorBody || 'Nie udało się cofnąć symulacji');
  }
  return response.json();
};
