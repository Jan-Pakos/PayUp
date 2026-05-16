package com.payup.userservice.auth;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private static final String SECRET = "test-secret-that-is-long-enough-for-hs256-algorithm-ok";
    private static final long EXPIRATION_MS = 3_600_000L;

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(SECRET, EXPIRATION_MS);
    }

    @Test
    void generateToken_producesNonBlankJwt() {
        String token = jwtService.generateToken(1L, "alice@example.com", "ROLE_USER");
        assertThat(token).isNotBlank().contains(".");
    }

    @Test
    void isTokenValid_returnsTrue_forFreshToken() {
        String token = jwtService.generateToken(1L, "alice@example.com", "ROLE_USER");
        assertThat(jwtService.isTokenValid(token)).isTrue();
    }

    @Test
    void isTokenValid_returnsFalse_forExpiredToken() {
        JwtService shortLived = new JwtService(SECRET, -1000L);
        String expiredToken = shortLived.generateToken(1L, "alice@example.com", "ROLE_USER");
        assertThat(jwtService.isTokenValid(expiredToken)).isFalse();
    }

    @Test
    void isTokenValid_returnsFalse_forGarbage() {
        assertThat(jwtService.isTokenValid("not.a.jwt")).isFalse();
    }

    @Test
    void extractClaims_returnsCorrectSubjectAndCustomClaims() {
        String token = jwtService.generateToken(42L, "bob@example.com", "ROLE_ADMIN");
        Claims claims = jwtService.extractClaims(token);

        assertThat(claims.getSubject()).isEqualTo("bob@example.com");
        assertThat(claims.get("role", String.class)).isEqualTo("ROLE_ADMIN");
        // JWT numbers round-trip through JSON as Integer when they fit; coerce safely
        assertThat(((Number) claims.get("userId")).longValue()).isEqualTo(42L);
    }

    @Test
    void tokensSignedWithDifferentSecrets_areNotValidatedByEachOther() {
        JwtService otherService = new JwtService("completely-different-secret-value-here-abc", EXPIRATION_MS);
        String foreignToken = otherService.generateToken(1L, "alice@example.com", "ROLE_USER");

        assertThat(jwtService.isTokenValid(foreignToken)).isFalse();
    }
}
