package com.sportexpo.exhibitor.auth;

import com.sportexpo.exhibitor.users.User;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Service
public class JwtService {
    private final AuthProperties properties;
    private final SecretKey signingKey;

    public JwtService(AuthProperties properties) {
        this.properties = properties;
        this.signingKey = Keys.hmacShaKeyFor(properties.signingKeyBytes());
    }

    public String issueToken(User user, List<String> roles) {
        var now = Instant.now();
        var expiresAt = now.plusSeconds(properties.getJwtExpirationSeconds());
        var builder = Jwts.builder()
                .issuer(properties.getJwtIssuer())
                .subject(user.getId().toString())
                .claim("username", user.getUsername())
                .claim("roles", roles)
                .id(UUID.randomUUID().toString())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .signWith(signingKey);
        if (user.getExhibitorId() != null) {
            builder.claim("exhibitorId", user.getExhibitorId().toString());
        }
        return builder.compact();
    }

    public JwtPrincipal parse(String token) {
        try {
            var claims = Jwts.parser()
                    .verifyWith(signingKey)
                    .requireIssuer(properties.getJwtIssuer())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            var rawRoles = claims.get("roles");
            if (claims.getId() == null || claims.getId().isBlank()) {
                throw invalidToken();
            }
            var username = claims.get("username", String.class);
            if (username == null || username.isBlank()) {
                throw invalidToken();
            }
            if (!(rawRoles instanceof List<?> roleValues) || roleValues.isEmpty()) {
                throw invalidToken();
            }
            if (roleValues.stream().anyMatch(role -> !(role instanceof String value) || value.isBlank())) {
                throw invalidToken();
            }
            var roles = roleValues.stream().map(String.class::cast).toList();
            var exhibitorId = claims.get("exhibitorId") == null
                    ? null
                    : UUID.fromString(claims.get("exhibitorId", String.class));
            return new JwtPrincipal(
                    UUID.fromString(claims.getSubject()),
                    username,
                    roles,
                    exhibitorId,
                    claims.getId(),
                    claims.getIssuedAt().toInstant(),
                    claims.getExpiration().toInstant()
            );
        } catch (ExpiredJwtException exception) {
            throw new AuthException(AuthErrorCodes.TOKEN_EXPIRED, "Token expired");
        } catch (JwtException | IllegalArgumentException | NullPointerException exception) {
            throw new AuthException(AuthErrorCodes.TOKEN_INVALID, "Invalid token");
        }
    }

    private AuthException invalidToken() {
        return new AuthException(AuthErrorCodes.TOKEN_INVALID, "Invalid token");
    }

    public long expirationSeconds() {
        return properties.getJwtExpirationSeconds();
    }
}
