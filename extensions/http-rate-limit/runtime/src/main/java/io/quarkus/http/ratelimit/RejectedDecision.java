package io.quarkus.http.ratelimit;

import java.time.Duration;
import java.util.Objects;

record RejectedDecision(Duration retryAfter) implements RateLimitDecision.Rejected {

    RejectedDecision {
        Objects.requireNonNull(retryAfter, "retryAfter");
        if (retryAfter.isNegative() || retryAfter.isZero()) {
            throw new IllegalArgumentException("retryAfter must be positive");
        }
        try {
            retryAfter.toNanos();
        } catch (ArithmeticException e) {
            throw new IllegalArgumentException("retryAfter must be representable in nanoseconds", e);
        }
    }
}
