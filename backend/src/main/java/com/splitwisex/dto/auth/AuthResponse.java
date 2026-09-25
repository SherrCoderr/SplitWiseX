package com.splitwisex.dto.auth;

/**
 * Returned by both /api/auth/register and /api/auth/login on success.
 *
 * expiresInMs lets the frontend know when the token will expire without
 * having to decode the JWT itself, though the frontend does also decode it
 * client-side to pre-emptively treat an expired token as logged out.
 */
public record AuthResponse(
        String token,
        String tokenType,
        long expiresInMs,
        UserSummaryDto user
) {
    public static AuthResponse of(String token, long expiresInMs, UserSummaryDto user) {
        return new AuthResponse(token, "Bearer", expiresInMs, user);
    }
}
