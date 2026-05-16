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
    void should_produce_non_blank_jwt_token() {
        // given
        Long userId = 1L;
        String email = "alice@example.com";
        String role = "ROLE_USER";

        // when
        String token = jwtService.generateToken(userId, email, role);

        // then
        assertThat(token).isNotBlank().contains(".");
    }

    @Test
    void should_return_true_for_a_fresh_token() {
        // given
        String token = jwtService.generateToken(1L, "alice@example.com", "ROLE_USER");

        // when
        boolean result = jwtService.isTokenValid(token);

        // then
        assertThat(result).isTrue();
    }

    @Test
    void should_return_false_for_an_expired_token() {
        // given
        JwtService shortLived = new JwtService(SECRET, -1000L);
        String expiredToken = shortLived.generateToken(1L, "alice@example.com", "ROLE_USER");

        // when
        boolean result = jwtService.isTokenValid(expiredToken);

        // then
        assertThat(result).isFalse();
    }

    @Test
    void should_return_false_for_a_garbage_string() {
        // given
        String garbage = "not.a.jwt";

        // when
        boolean result = jwtService.isTokenValid(garbage);

        // then
        assertThat(result).isFalse();
    }

    @Test
    void should_extract_correct_subject_and_custom_claims() {
        // given
        String token = jwtService.generateToken(42L, "bob@example.com", "ROLE_ADMIN");

        // when
        Claims claims = jwtService.extractClaims(token);

        // then
        assertThat(claims.getSubject()).isEqualTo("bob@example.com");
        assertThat(claims.get("role", String.class)).isEqualTo("ROLE_ADMIN");
        assertThat(((Number) claims.get("userId")).longValue()).isEqualTo(42L);
    }

    @Test
    void should_reject_token_signed_with_a_different_secret() {
        // given
        JwtService otherService = new JwtService("completely-different-secret-value-here-abc", EXPIRATION_MS);
        String foreignToken = otherService.generateToken(1L, "alice@example.com", "ROLE_USER");

        // when
        boolean result = jwtService.isTokenValid(foreignToken);

        // then
        assertThat(result).isFalse();
    }
}
