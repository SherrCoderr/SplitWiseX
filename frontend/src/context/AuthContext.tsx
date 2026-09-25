import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useState,
  type ReactNode,
} from "react";
import { loginRequest, registerRequest } from "../api/auth";
import { clearAuthStorage, getStoredUser, getToken, setAuthStorage } from "../lib/authStorage";
import { isTokenExpired } from "../lib/jwt";
import type { LoginPayload, RegisterPayload, UserSummary } from "../types/auth";

interface AuthContextValue {
  user: UserSummary | null;
  /** True only while restoring auth state from localStorage on first load. */
  isLoading: boolean;
  login: (payload: LoginPayload) => Promise<void>;
  register: (payload: RegisterPayload) => Promise<void>;
  logout: () => void;
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<UserSummary | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  // Restore session from localStorage on first load, discarding it if the
  // token has already expired.
  useEffect(() => {
    const token = getToken();
    const storedUser = getStoredUser();

    if (token && storedUser && !isTokenExpired(token)) {
      setUser(storedUser);
    } else {
      clearAuthStorage();
    }
    setIsLoading(false);
  }, []);

  // The Axios interceptor (api/client.ts) fires this on any 401 response.
  useEffect(() => {
    function handleUnauthorized() {
      setUser(null);
    }
    window.addEventListener("splitwisex:unauthorized", handleUnauthorized);
    return () => window.removeEventListener("splitwisex:unauthorized", handleUnauthorized);
  }, []);

  const login = useCallback(async (payload: LoginPayload) => {
    const response = await loginRequest(payload);
    setAuthStorage(response.token, response.user);
    setUser(response.user);
  }, []);

  const register = useCallback(async (payload: RegisterPayload) => {
    const response = await registerRequest(payload);
    setAuthStorage(response.token, response.user);
    setUser(response.user);
  }, []);

  const logout = useCallback(() => {
    clearAuthStorage();
    setUser(null);
  }, []);

  return (
    <AuthContext.Provider value={{ user, isLoading, login, register, logout }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth(): AuthContextValue {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error("useAuth must be used within an AuthProvider");
  }
  return context;
}
