package com.civiclens.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JwtProviderTest {

    private static final String SECRET = "test-secret-at-least-32-characters-long-for-hs256";
    private JwtProvider jwtProvider;

    @BeforeEach
    void setUp() {
        JwtProperties props = new JwtProperties();
        props.setSecret(SECRET);
        props.setExpirationMs(3600000L);
        jwtProvider = new JwtProvider(props);
    }

    @Test
    void generateToken_andParseToken_roundTrip() {
        String token = jwtProvider.generateToken("user@example.com", 1L);
        assertThat(token).isNotBlank();

        JwtProvider.JwtClaims claims = jwtProvider.parseToken(token);
        assertThat(claims).isNotNull();
        assertThat(claims.email()).isEqualTo("user@example.com");
        assertThat(claims.userId()).isEqualTo(1L);
    }

    @Test
    void parseToken_returnsNullForInvalidToken() {
        assertThat(jwtProvider.parseToken("invalid")).isNull();
        assertThat(jwtProvider.parseToken("")).isNull();
        assertThat(jwtProvider.parseToken(null)).isNull();
    }
}
