package com.sportexpo.exhibitor.auth;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Component
@ConfigurationProperties(prefix = "app.auth")
public class AuthProperties {
    private String jwtSecret;
    private long jwtExpirationSeconds;
    private String jwtIssuer;
    private int loginMaxFailures;
    private long loginLockMinutes;

    @PostConstruct
    void validate() {
        if (signingKeyBytes().length < 32) {
            throw new IllegalStateException("JWT secret must be at least 32 bytes");
        }
        if (jwtExpirationSeconds <= 0) {
            throw new IllegalStateException("JWT expiration seconds must be positive");
        }
        if (loginMaxFailures <= 0) {
            throw new IllegalStateException("Login max failures must be positive");
        }
        if (loginLockMinutes <= 0) {
            throw new IllegalStateException("Login lock minutes must be positive");
        }
    }

    public byte[] signingKeyBytes() {
        try {
            return Base64.getDecoder().decode(jwtSecret);
        } catch (IllegalArgumentException exception) {
            return jwtSecret.getBytes(StandardCharsets.UTF_8);
        }
    }

    public String getJwtSecret() { return jwtSecret; }
    public void setJwtSecret(String jwtSecret) { this.jwtSecret = jwtSecret; }
    public long getJwtExpirationSeconds() { return jwtExpirationSeconds; }
    public void setJwtExpirationSeconds(long jwtExpirationSeconds) { this.jwtExpirationSeconds = jwtExpirationSeconds; }
    public String getJwtIssuer() { return jwtIssuer; }
    public void setJwtIssuer(String jwtIssuer) { this.jwtIssuer = jwtIssuer; }
    public int getLoginMaxFailures() { return loginMaxFailures; }
    public void setLoginMaxFailures(int loginMaxFailures) { this.loginMaxFailures = loginMaxFailures; }
    public long getLoginLockMinutes() { return loginLockMinutes; }
    public void setLoginLockMinutes(long loginLockMinutes) { this.loginLockMinutes = loginLockMinutes; }
}
