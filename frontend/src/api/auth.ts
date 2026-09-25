import { AxiosError } from "axios";
import { api } from "./client";
import type { ApiErrorBody, AuthResponse, LoginPayload, RegisterPayload } from "../types/auth";

export async function registerRequest(payload: RegisterPayload): Promise<AuthResponse> {
  const { data } = await api.post<AuthResponse>("/auth/register", payload);
  return data;
}

export async function loginRequest(payload: LoginPayload): Promise<AuthResponse> {
  const { data } = await api.post<AuthResponse>("/auth/login", payload);
  return data;
}

/**
 * Pulls a human-readable message out of an Axios error thrown by the calls
 * above. The backend always returns { message, fieldErrors? } (see
 * GlobalExceptionHandler), so this prefers the first field error if present
 * (more specific), falling back to the top-level message, then a generic
 * network-failure message.
 */
export function extractErrorMessage(error: unknown): string {
  if (error instanceof AxiosError) {
    const body = error.response?.data as ApiErrorBody | undefined;
    if (body?.fieldErrors) {
      const firstFieldError = Object.values(body.fieldErrors)[0];
      if (firstFieldError) return firstFieldError;
    }
    if (body?.message) return body.message;
    if (error.code === "ERR_NETWORK") {
      return "Can't reach the server. Is the backend running?";
    }
  }
  return "Something went wrong. Please try again.";
}
