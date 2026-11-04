package io.quarkus.http.ratelimit;

import java.time.Duration;
import java.util.Optional;

record OverrideLimit(long permits, Duration window, Optional<Long> burst) implements RateLimitOverride.Limit {
}
