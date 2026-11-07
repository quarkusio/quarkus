package io.quarkus.http.ratelimit;

import java.time.Duration;
import java.util.Objects;

record UnavailableDecision(Duration retryAfter) implements RateLimitDecision.Unavailable {

    UnavailableDecision {
        Objects.requireNonNull(retryAfter, "retryAfter");
    }
}
