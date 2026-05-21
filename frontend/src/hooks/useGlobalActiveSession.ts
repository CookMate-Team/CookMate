import { useQuery } from '@tanstack/react-query';
import { getActiveCookingSessionGlobal } from '../services/simulatorApi';

export function useGlobalActiveSession() {
  return useQuery({
    queryKey: ['active-cooking-session-global'],
    queryFn: getActiveCookingSessionGlobal,
    refetchInterval: 5000, // Poll every 5 seconds to keep main page and layouts in sync
  });
}
