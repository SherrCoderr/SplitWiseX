package com.splitwisex.security;

import com.splitwisex.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;

/**
 * Issues and verifies the JWTs used as bearer tokens for the API.
 *
 * The token carries the user's id (as the subject) plus email/name claims
 * so the JwtAuthenticationFilter can authenticate a request without an
 * extra database round trip for every call — it still re-fetches the User
 * from the database (see JwtAuthenticationFilter) to make sure the account
 * still exists, but doesn't need the claims for anything but that lookup.
 */
@Service
public class JwtService {

    private final SecretKey signingKey;
    private final long expirationMs;

    public JwtService(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expiration-ms}") long expirationMs
    ) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    public long getExpirationMs() {
        return expirationMs;
    }

    public String generateToken(User user) {
        Instant now = Instant.now();
        Instant expiry = now.plusMillis(expirationMs);

        return Jwts.builder()
                .subject(String.valueOf(user.getId()))
                .claim("email", user.getEmail())
                .claim("name", user.getName())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(signingKey)
                .compact();
    }

    /**
     * Parses and verifies the token's signature and expiry. Returns empty
     * (rather than throwing) for any invalid, tampered, or expired token so
     * callers — namely JwtAuthenticationFilter — can treat the request as
     * simply unauthenticated instead of handling a stack of JWT-specific
     * exceptions themselves.
     */
    public Optional<Claims> parseClaims(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return Optional.of(claims);
        } catch (JwtException | IllegalArgumentException ex) {
            return Optional.empty();
        }
    }

    public Optional<Long> extractUserId(String token) {
        return parseClaims(token).map(claims -> Long.valueOf(claims.getSubject()));
    }
}
