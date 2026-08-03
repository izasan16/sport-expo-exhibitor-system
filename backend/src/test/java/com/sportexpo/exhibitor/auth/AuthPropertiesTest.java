package com.sportexpo.exhibitor.auth;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuthPropertiesTest {
    private static final String CUSTOM_SECRET = "not-a-base64-secret-that-is-long-enough";

    @Test
    void rejectsDevDefaultSecretWhenNoActiveProfileIsSet() {
        var properties = validProperties();
        properties.setJwtSecret(AuthProperties.DEV_DEFAULT_JWT_SECRET);

        assertThatThrownBy(properties::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT_SECRET must be changed");
    }

    @Test
    void allowsDevDefaultSecretForLocalDevAndTestProfiles() {
        for (var profile : new String[]{"local", "dev", "test"}) {
            var properties = validProperties(profile);
            properties.setJwtSecret(AuthProperties.DEV_DEFAULT_JWT_SECRET);

            assertThatCode(properties::validate).doesNotThrowAnyException();
        }
    }

    @Test
    void rejectsDevDefaultSecretForProductionProfile() {
        var properties = validProperties("prod");
        properties.setJwtSecret(AuthProperties.DEV_DEFAULT_JWT_SECRET);

        assertThatThrownBy(properties::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT_SECRET must be changed");
    }

    @Test
    void rejectsDevDefaultSecretForNonLocalProfile() {
        var properties = validProperties("staging");
        properties.setJwtSecret(AuthProperties.DEV_DEFAULT_JWT_SECRET);

        assertThatThrownBy(properties::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT_SECRET must be changed");
    }

    @Test
    void allowsCustomSecretForProductionProfile() {
        var properties = validProperties("production");
        properties.setJwtSecret(CUSTOM_SECRET);

        assertThatCode(properties::validate).doesNotThrowAnyException();
    }

    @Test
    void rejectsSecretsShorterThanThirtyTwoBytes() {
        var properties = validProperties("prod");
        properties.setJwtSecret("too-short");

        assertThatThrownBy(properties::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("at least 32 bytes");
    }

    private AuthProperties validProperties(String... activeProfiles) {
        var environment = new MockEnvironment();
        environment.setActiveProfiles(activeProfiles);
        var properties = new AuthProperties(environment);
        properties.setJwtSecret(CUSTOM_SECRET);
        properties.setJwtExpirationSeconds(60);
        properties.setJwtIssuer("test-issuer");
        properties.setLoginMaxFailures(5);
        properties.setLoginLockMinutes(15);
        return properties;
    }
}
