import keycloak from '../auth/keycloak';

/**
 * Wrapper na fetch() automatycznie dodający Bearer token z Keycloak.
 * Przed każdym requestem odświeża token jeśli wygasa w ciągu 30 sekund.
 * Jeśli odświeżenie się nie powiedzie (sesja wygasła), przekierowuje do logowania.
 */
export async function authFetch(url: string, options: RequestInit = {}): Promise<Response> {
  if (keycloak.authenticated) {
    try {
      await keycloak.updateToken(30);
    } catch {
      keycloak.login();
      return Promise.reject(new Error('Session expired. Redirecting to login.'));
    }
  }

  const headers = new Headers(options.headers);
  if (keycloak.token) {
    headers.set('Authorization', `Bearer ${keycloak.token}`);
  }

  return fetch(url, { ...options, headers });
}
