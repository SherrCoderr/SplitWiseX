/**
 * Decodes the payload of a JWT without verifying its signature. This is
 * only used client-side to read the expiry so the UI can pre-emptively
 * treat an expired token as "logged out" instead of waiting for a 401 from
 * the API. The backend (JwtService) is the sole source of truth for
 * whether a token is actually valid.
 */
function decodePayload(token: string): Record<string, unknown> | null {
  const parts = token.split(".");
  if (parts.length !== 3) return null;

  try {
    const base64 = parts[1].replace(/-/g, "+").replace(/_/g, "/");
    const json = decodeURIComponent(
      atob(base64)
        .split("")
        .map((char) => "%" + char.charCodeAt(0).toString(16).padStart(2, "0"))
        .join("")
    );
    return JSON.parse(json) as Record<string, unknown>;
  } catch {
    return null;
  }
}

export function isTokenExpired(token: string): boolean {
  const payload = decodePayload(token);
  const exp = payload?.exp;
  if (typeof exp !== "number") return true;
  return Date.now() >= exp * 1000;
}
