package io.quarkus.http.ratelimit;

import java.time.Duration;

/**
 * An immutable rate limit. Permits replenish evenly over the window, up to the burst capacity.
 * Named limits and limits returned by a {@link RateLimitResolver} use the same value type.
 *
 * @param permits permits replenished per window, at least one
 * @param window replenishment window, positive
 * @param burst maximum permits available at once, at least one
 */
public record RateLimit(long permits, Duration window, long burst) {

    public RateLimit {
        if (permits < 1) {
            throw new IllegalArgumentException("permits must be at least 1");
        }
        if (window == null || window.isNegative() || window.isZero()) {
            throw new IllegalArgumentException("window must be positive");
        }
        if (burst < 1) {
            throw new IllegalArgumentException("burst must be at least 1");
        }
        try {
            // Validate the exact ratio before rounding: sub-microsecond intervals must not silently become one.
            if (window.toNanos() / 1_000 < permits) {
                throw new IllegalArgumentException("window / permits must be at least 1 microsecond");
            }
            long interval = Math.ceilDiv(Math.ceilDiv(window.toNanos(), 1_000), permits);
            if (Math.multiplyExact(interval, burst) > Duration.ofDays(30).toNanos() / 1_000) {
                throw new IllegalArgumentException("(window / permits) * burst must not exceed 30 days");
            }
        } catch (ArithmeticException e) {
            throw new IllegalArgumentException("window, permits and burst overflow", e);
        }
    }

    /**
     * Creates a limit whose burst equals its permits.
     */
    public static RateLimit of(long permits, Duration window) {
        return new RateLimit(permits, window, permits);
    }

    /**
     * Creates a limit with an explicit burst capacity.
     */
    public static RateLimit of(long permits, Duration window, long burst) {
        return new RateLimit(permits, window, burst);
    }
}
