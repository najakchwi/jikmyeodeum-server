package com.sportsmate.server.tools;

import com.sportsmate.server.common.enums.Role;
import com.sportsmate.server.infrastructure.adapter.out.token.JwtProvider;
import java.time.Instant;

public class AdminTokenGenerator {

    private static final String DEFAULT_SUBJECT = "admin-service";
    private static final long DEFAULT_DAYS = 30L;
    private static final long SECONDS_PER_DAY = 86_400L;

    public static void main(String[] args) {
        String secret = requireEnv("JWT_SECRET");
        String subject = envOrDefault("ADMIN_TOKEN_SUBJECT", DEFAULT_SUBJECT);
        long days = parseDays(envOrDefault("ADMIN_TOKEN_DAYS", Long.toString(DEFAULT_DAYS)));
        long expirationSeconds = Math.multiplyExact(days, SECONDS_PER_DAY);

        JwtProvider jwtProvider = new JwtProvider(secret, 0L, 0L);
        String token = jwtProvider.issueServiceToken(subject, Role.ADMIN, expirationSeconds);

        System.err.println("Generated ADMIN service access token");
        System.err.println("subject=" + subject);
        System.err.println("expiresAt=" + Instant.now().plusSeconds(expirationSeconds));
        System.out.println(token);
    }

    private static String requireEnv(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " environment variable is required");
        }
        return value;
    }

    private static String envOrDefault(String name, String defaultValue) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return value;
    }

    private static long parseDays(String value) {
        try {
            long days = Long.parseLong(value);
            if (days <= 0) {
                throw new IllegalArgumentException("ADMIN_TOKEN_DAYS must be positive");
            }
            return days;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("ADMIN_TOKEN_DAYS must be a number", exception);
        }
    }
}
