import type { UserSummary } from "../types/auth";

/**
 * Where the auth token and user live in the browser.
 *
 * Why localStorage: the backend expects the token as an "Authorization:
 * Bearer <token>" header (see JwtAuthenticationFilter), not as a cookie, so
 * there's no server-side cookie/session to piggyback on. localStorage keeps
 * this simple for a project of this scope. The trade-off is that it's
 * readable by any JS on the page (XSS risk) — an httpOnly cookie would avoid
 * that, but would require the backend to issue and read cookies plus CSRF
 * protection, which is more than this stage's JWT-header design calls for.
 */
const TOKEN_KEY = "splitwisex_token";
const USER_KEY = "splitwisex_user";

export function getToken(): string | null {
  return localStorage.getItem(TOKEN_KEY);
}

export function getStoredUser(): UserSummary | null {
  const raw = localStorage.getItem(USER_KEY);
  if (!raw) return null;
  try {
    return JSON.parse(raw) as UserSummary;
  } catch {
    return null;
  }
}

export function setAuthStorage(token: string, user: UserSummary): void {
  localStorage.setItem(TOKEN_KEY, token);
  localStorage.setItem(USER_KEY, JSON.stringify(user));
}

export function clearAuthStorage(): void {
  localStorage.removeItem(TOKEN_KEY);
  localStorage.removeItem(USER_KEY);
}
