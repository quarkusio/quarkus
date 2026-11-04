package io.quarkus.http.ratelimit;

import java.time.Duration;
import java.util.Objects;

record RejectedDecision(Duration retryAfter) implements RateLimitDecision.Rejected {

    RejectedDecision {
        Objects.requireNonNull(retryAfter, "retryAfter");
    }
}
