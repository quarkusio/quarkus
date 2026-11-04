package io.quarkus.http.ratelimit;

record AllowedDecision(long remaining) implements RateLimitDecision.Allowed {
}
