package io.quarkus.smallrye.faulttolerance.test.config;

import java.time.temporal.ChronoUnit;

import jakarta.enterprise.context.ApplicationScoped;

import io.smallrye.faulttolerance.api.RateLimit;

@ApplicationScoped
public class RateLimitConfigBean {
    @RateLimit(value = 10)
    public String value() {
        return "value";
    }

    @RateLimit(value = 3, window = 10, windowUnit = ChronoUnit.MINUTES)
    public String window() {
        return "window";
    }

    @RateLimit(value = 3, minSpacing = 10, minSpacingUnit = ChronoUnit.MINUTES)
    public String minSpacing() {
        return "minSpacing";
    }
}
