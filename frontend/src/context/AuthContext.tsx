import {
  createContext,
  useContext,
  useEffect,
  useState,
  useCallback,
  useMemo,
  type PropsWithChildren,
} from 'react';
import keycloak from '../auth/keycloak';

interface AuthUser {
  username: string;
  email: string;
  firstName: string;
  lastName: string;
  roles: string[];
  sub: string;
}

interface AuthContextValue {
  isAuthenticated: boolean;
  isLoading: boolean;
  user: AuthUser | null;
  login: () => void;
  logout: () => void;
  register: () => void;
  getToken: () => Promise<string | undefined>;
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined);

function parseUser(kc: typeof keycloak): AuthUser | null {
  if (!kc.tokenParsed) return null;
  const p = kc.tokenParsed as Record<string, unknown>;
  const realmRoles = (p['realm_access'] as { roles?: string[] } | undefined)?.roles ?? [];
  return {
    username: (p['preferred_username'] as string) ?? '',
    email: (p['email'] as string) ?? '',
    firstName: (p['given_name'] as string) ?? '',
    lastName: (p['family_name'] as string) ?? '',
    roles: realmRoles,
    sub: (p['sub'] as string) ?? '',
  };
}

export function AuthProvider({ children }: PropsWithChildren) {
  const [isAuthenticated, setIsAuthenticated] = useState(false);
  const [isLoading, setIsLoading] = useState(true);
  const [user, setUser] = useState<AuthUser | null>(null);

  useEffect(() => {
    keycloak
      .init({
        onLoad: 'check-sso',
        silentCheckSsoRedirectUri: `${window.location.origin}/silent-check-sso.html`,
        pkceMethod: 'S256',
        checkLoginIframe: false,
      })
      .then((authenticated) => {
        setIsAuthenticated(authenticated);
        if (authenticated) {
          setUser(parseUser(keycloak));
        }
      })
      .catch((err) => {
        console.error('Keycloak init failed:', err);
      })
      .finally(() => {
        setIsLoading(false);
      });

    // Odśwież token gdy wygasa — 30s przed wygaśnięciem
    keycloak.onTokenExpired = () => {
      keycloak
        .updateToken(30)
        .then((refreshed) => {
          if (refreshed) {
            setUser(parseUser(keycloak));
          }
        })
        .catch(() => {
          // Sesja wygasła — wyloguj
          setIsAuthenticated(false);
          setUser(null);
        });
    };
  }, []);

  const login = useCallback(() => {
    keycloak.login();
  }, []);

  const logout = useCallback(() => {
    keycloak.logout({ redirectUri: window.location.origin });
  }, []);

  const register = useCallback(() => {
    keycloak.register();
  }, []);

  const getToken = useCallback(async (): Promise<string | undefined> => {
    try {
      await keycloak.updateToken(30);
    } catch {
      keycloak.login();
      return undefined;
    }
    return keycloak.token;
  }, []);

  const value = useMemo(
    () => ({ isAuthenticated, isLoading, user, login, logout, register, getToken }),
    [isAuthenticated, isLoading, user, login, logout, register, getToken]
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used within AuthProvider');
  return ctx;
}
