import { useQuery } from '@tanstack/react-query';
import { getActiveCookingSessionGlobal } from '../services/simulatorApi';

export function useGlobalActiveSession(enabled = true) {
  return useQuery({
    queryKey: ['active-cooking-session-global'],
    queryFn: getActiveCookingSessionGlobal,
    enabled,
  });
}
