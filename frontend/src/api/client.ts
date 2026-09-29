import axios from "axios";
import { clearAuthStorage, getToken } from "../lib/authStorage";

/**
 * Central Axios instance. Requests go through the Vite dev-server proxy
 * (see vite.config.ts) so we can just use relative "/api/..." paths and
 * avoid hardcoding http://localhost:8080 everywhere.
 */
const API_URL = import.meta.env.VITE_API_URL || "";

export const api = axios.create({
  baseURL: `${API_URL}/api`,
  headers: {
    "Content-Type": "application/json",
  },
});

// Attach the JWT to every request, if we have one.
api.interceptors.request.use((config) => {
  const token = getToken();
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// A 401 means the token is missing/invalid/expired as far as the backend is
// concerned. Clear local auth state and let AuthContext (which listens for
// this event) redirect to /login — the interceptor itself has no router
// access, so it just broadcasts what happened.
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      clearAuthStorage();
      window.dispatchEvent(new Event("splitwisex:unauthorized"));
    }
    return Promise.reject(error);
  }
);
