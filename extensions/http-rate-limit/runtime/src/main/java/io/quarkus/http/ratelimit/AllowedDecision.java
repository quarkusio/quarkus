package io.quarkus.http.ratelimit;

record AllowedDecision(long remaining) implements RateLimitDecision.Allowed {
    AllowedDecision {
        if (remaining < 0) {
            throw new IllegalArgumentException("remaining must not be negative");
        }
    }
}
