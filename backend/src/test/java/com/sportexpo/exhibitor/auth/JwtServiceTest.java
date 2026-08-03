package com.sportexpo.exhibitor.auth;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {
    private static final String CUSTOM_SECRET = "not-a-base64-secret-that-is-long-enough";

    @Test
    void parseRejectsTokenWithoutJti() {
        var service = new JwtService(properties());
        var token = tokenBuilder()
                .compact();

        assertThatThrownBy(() -> service.parse(token))
                .isInstanceOf(AuthException.class)
                .extracting("code")
                .isEqualTo(AuthErrorCodes.TOKEN_INVALID);
    }

    @Test
    void parseRejectsTokenWithBlankUsername() {
        var service = new JwtService(properties());
        var token = tokenBuilder()
                .claim("username", " ")
                .id(UUID.randomUUID().toString())
                .compact();

        assertThatThrownBy(() -> service.parse(token))
                .isInstanceOf(AuthException.class)
                .extracting("code")
                .isEqualTo(AuthErrorCodes.TOKEN_INVALID);
    }

    @Test
    void parseRejectsTokenWithEmptyRoles() {
        var service = new JwtService(properties());
        var token = tokenBuilder()
                .claim("roles", List.of())
                .id(UUID.randomUUID().toString())
                .compact();

        assertThatThrownBy(() -> service.parse(token))
                .isInstanceOf(AuthException.class)
                .extracting("code")
                .isEqualTo(AuthErrorCodes.TOKEN_INVALID);
    }

    @Test
    void parseRejectsTokenWithNonStringRoleValue() {
        var service = new JwtService(properties());
        var token = tokenBuilder()
                .claim("roles", List.of(42))
                .id(UUID.randomUUID().toString())
                .compact();

        assertThatThrownBy(() -> service.parse(token))
                .isInstanceOf(AuthException.class)
                .extracting("code")
                .isEqualTo(AuthErrorCodes.TOKEN_INVALID);
    }

    @Test
    void parseRejectsTokenWithBlankRoleValue() {
        var service = new JwtService(properties());
        var token = tokenBuilder()
                .claim("roles", List.of("SYSTEM_ADMIN", " "))
                .id(UUID.randomUUID().toString())
                .compact();

        assertThatThrownBy(() -> service.parse(token))
                .isInstanceOf(AuthException.class)
                .extracting("code")
                .isEqualTo(AuthErrorCodes.TOKEN_INVALID);
    }

    @Test
    void parseRejectsExpiredTokenAsExpired() {
        var service = new JwtService(properties());
        var now = Instant.now();
        var token = Jwts.builder()
                .issuer("test-issuer")
                .subject(UUID.randomUUID().toString())
                .claim("username", "auth-user")
                .claim("roles", List.of("SYSTEM_ADMIN"))
                .id(UUID.randomUUID().toString())
                .issuedAt(Date.from(now.minusSeconds(120)))
                .expiration(Date.from(now.minusSeconds(60)))
                .signWith(Keys.hmacShaKeyFor(properties().signingKeyBytes()))
                .compact();

        assertThatThrownBy(() -> service.parse(token))
                .isInstanceOf(AuthException.class)
                .extracting("code")
                .isEqualTo(AuthErrorCodes.TOKEN_EXPIRED);
    }

    private io.jsonwebtoken.JwtBuilder tokenBuilder() {
        var now = Instant.now();
        return Jwts.builder()
                .issuer("test-issuer")
                .subject(UUID.randomUUID().toString())
                .claim("username", "auth-user")
                .claim("roles", List.of("SYSTEM_ADMIN"))
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(60)))
                .signWith(Keys.hmacShaKeyFor(properties().signingKeyBytes()));
    }

    private AuthProperties properties() {
        var properties = new AuthProperties(new MockEnvironment());
        properties.setJwtSecret(CUSTOM_SECRET);
        properties.setJwtExpirationSeconds(60);
        properties.setJwtIssuer("test-issuer");
        properties.setLoginMaxFailures(5);
        properties.setLoginLockMinutes(15);
        return properties;
    }
}
