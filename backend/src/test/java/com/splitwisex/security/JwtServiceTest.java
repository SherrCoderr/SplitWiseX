package com.splitwisex.security;

import com.splitwisex.entity.User;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private static final String SECRET = "test-only-secret-value-needs-32-bytes-min";
    private static final long EXPIRATION_MS = 60_000;

    private JwtService jwtService;
    private User user;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(SECRET, EXPIRATION_MS);
        user = User.builder()
                .id(42L)
                .name("Sameer Kumar")
                .email("sameer@example.com")
                .passwordHash("irrelevant-for-this-test")
                .build();
    }

    @Test
    void generatesATokenThatParsesBackToTheSameUserId() {
        String token = jwtService.generateToken(user);

        Optional<Long> extractedUserId = jwtService.extractUserId(token);

        assertThat(extractedUserId).contains(42L);
    }

    @Test
    void tokenClaimsIncludeEmailAndName() {
        String token = jwtService.generateToken(user);

        Optional<Claims> claims = jwtService.parseClaims(token);

        assertThat(claims).isPresent();
        assertThat(claims.get().get("email", String.class)).isEqualTo("sameer@example.com");
        assertThat(claims.get().get("name", String.class)).isEqualTo("Sameer Kumar");
    }

    @Test
    void rejectsATokenSignedWithADifferentSecret() {
        JwtService otherService = new JwtService("a-completely-different-secret-value-32b", EXPIRATION_MS);
        String tokenFromOtherService = otherService.generateToken(user);

        Optional<Claims> claims = jwtService.parseClaims(tokenFromOtherService);

        assertThat(claims).isEmpty();
    }

    @Test
    void rejectsAnObviouslyMalformedToken() {
        Optional<Claims> claims = jwtService.parseClaims("not.a.valid.jwt");

        assertThat(claims).isEmpty();
    }

    @Test
    void rejectsAnAlreadyExpiredToken() throws InterruptedException {
        JwtService shortLivedService = new JwtService(SECRET, 1);
        String token = shortLivedService.generateToken(user);

        Thread.sleep(20);

        assertThat(shortLivedService.parseClaims(token)).isEmpty();
    }
}
