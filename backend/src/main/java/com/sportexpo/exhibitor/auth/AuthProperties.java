package com.sportexpo.exhibitor.auth;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.Set;

@Component
@ConfigurationProperties(prefix = "app.auth")
public class AuthProperties {
    public static final String DEV_DEFAULT_JWT_SECRET =
            "dGhpcy1pcy1vbmx5LWZvci1kZXYtZW52LXBsZWFzZS1yZXBsYWNlLWluLXByb2R1Y3Rpb24=";
    private static final Set<String> DEV_DEFAULT_ALLOWED_PROFILES = Set.of("local", "dev", "test");

    private final Environment environment;

    private String jwtSecret;
    private long jwtExpirationSeconds;
    private String jwtIssuer;
    private int loginMaxFailures;
    private long loginLockMinutes;

    public AuthProperties(Environment environment) {
        this.environment = environment;
    }

    @PostConstruct
    void validate() {
        if (usesDevDefaultSecretOutsideAllowedProfiles()) {
            throw new IllegalStateException("JWT_SECRET must be changed outside local/dev/test profiles");
        }
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

    private boolean usesDevDefaultSecretOutsideAllowedProfiles() {
        if (!DEV_DEFAULT_JWT_SECRET.equals(jwtSecret)) {
            return false;
        }
        var activeProfiles = environment.getActiveProfiles();
        if (activeProfiles.length == 0) {
            return true;
        }
        return Arrays.stream(activeProfiles)
                .map(profile -> profile.toLowerCase().trim())
                .anyMatch(profile -> !DEV_DEFAULT_ALLOWED_PROFILES.contains(profile));
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
