package com.vocably.auth;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/** Lê as chaves vocably.jwt.* do application.yml de forma tipada. */
@ConfigurationProperties(prefix = "vocably.jwt")
public record JwtProperties(String secret, Duration expiration) {

    public JwtProperties {
        if (secret == null || secret.length() < 32) {
            throw new IllegalArgumentException("vocably.jwt.secret must have at least 32 characters");
        }
    }
}
