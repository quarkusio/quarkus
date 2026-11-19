package io.quarkus.http.ratelimit.runtime.internal;

import java.time.Duration;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.jboss.logging.Logger;

import io.quarkus.http.ratelimit.runtime.internal.config.HttpRateLimitRuntimeConfig;
import io.quarkus.http.ratelimit.runtime.internal.gcra.RateLimitClock;

/**
 * Creates the {@link ThrottledLogger}s of the rate limiter, on the rate limit clock and with
 * {@code quarkus.http.rate-limit.warning-interval}.
 */
@Singleton
public class ThrottledLoggers {

    /**
     * The default of {@code quarkus.http.rate-limit.warning-interval}, for code that runs without the configuration.
     */
    public static final Duration DEFAULT_INTERVAL = Duration.ofMinutes(1);

    private final RateLimitClock clock;
    private final Duration interval;

    @Inject
    public ThrottledLoggers(RateLimitClock clock, HttpRateLimitRuntimeConfig config) {
        this(clock, config.warningInterval());
    }

    public ThrottledLoggers(RateLimitClock clock, Duration interval) {
        this.clock = clock;
        this.interval = interval;
    }

    /**
     * @return a logger that logs through {@code log}, throttled on its own
     */
    public ThrottledLogger logger(Logger log) {
        return new ThrottledLogger(log, clock, interval);
    }
}
