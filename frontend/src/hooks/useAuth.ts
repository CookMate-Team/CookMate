import { useQuery } from '@tanstack/react-query';

export interface UserInfo {
  authenticated: boolean;
  username: string;
  email?: string;
  name?: string;
  roles?: string[];
}

export function useAuth() {
  return useQuery<UserInfo | null>({
    queryKey: ['auth-user'],
    queryFn: async () => {
      try {
        const response = await fetch(`/api/v1/users/me?t=${new Date().getTime()}`, { credentials: 'include' });
        if (response.status === 401) {
          return null; // Not logged in
        }
        if (!response.ok) {
          throw new Error('Failed to fetch user');
        }
        return await response.json();
      } catch {
        return null;
      }
    },
    retry: false,
    // Re-check every 30s (vs 5min) so we detect gateway restarts/session loss quickly
    staleTime: 30 * 1000,
    refetchOnWindowFocus: true,
    refetchInterval: 60 * 1000, // Also poll every 60s in background
  });
}
