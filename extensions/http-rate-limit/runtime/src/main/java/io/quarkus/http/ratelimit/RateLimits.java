package io.quarkus.http.ratelimit;

/**
 * Injectable registry of limits configured under {@code quarkus.http.rate-limit.limits}.
 * Values are immutable and loaded at application startup. Looking up a limit performs no I/O.
 */
public interface RateLimits {

    /**
     * @param name the configured limit name
     * @return the configured limit
     * @throws IllegalArgumentException if the name is not configured
     */
    RateLimit get(String name);
}
