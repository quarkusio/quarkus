package io.quarkus.http.ratelimit.runtime.internal.config;

import java.time.Duration;

import io.quarkus.http.ratelimit.RateLimitPhase;
import io.quarkus.http.ratelimit.RateLimitPolicy;

/**
 * The limit of one rule, as resolved from configuration (and possibly a per-key override). Every policy the extension
 * hands to a store is of this type; {@link #maxKeys()} is internal and not part of {@link RateLimitPolicy}.
 * <p>
 * The canonical constructor rejects {@code permits < 1}, {@code burst < 1}, an {@linkplain #emissionIntervalMicros()
 * emission interval} below one microsecond, a {@linkplain #burstToleranceMicros() burst tolerance} above 30 days, and
 * any arithmetic overflow, with {@link IllegalArgumentException}. Both the configuration validator and the override
 * path go through it, so Java and Lua stores see the same domain.
 *
 * @param rule the rule name
 * @param phase the phase this policy applies to
 * @param permits the number of permits replenished per {@code window}
 * @param window the replenishment window
 * @param burst the maximum number of permits that can be consumed at once
 * @param maxKeys the maximum number of keys held in the memory of a replica for this policy: by the local store, the
 *        lease cache and the local fallback of a non-local store
 */
public record DefaultRateLimitPolicy(String rule, RateLimitPhase phase, long permits, Duration window, long burst,
        long maxKeys) implements RateLimitPolicy {

    private static final long MAX_BURST_TOLERANCE_MICROS = Duration.ofDays(30).toNanos() / 1_000;

    public DefaultRateLimitPolicy {
        String where = "Rate limit rule '" + rule + "' (" + phase.configName() + "): ";
        if (permits < 1) {
            throw new IllegalArgumentException(where + "permits must be at least 1");
        }
        if (burst < 1) {
            throw new IllegalArgumentException(where + "burst must be at least 1");
        }
        if (window == null) {
            throw new IllegalArgumentException(where + "window must be set");
        }
        long interval;
        long tolerance;
        try {
            // checked on the exact ratio: rounded up, an interval below one microsecond would silently become one
            if (window.toNanos() / 1_000 < permits) {
                throw new IllegalArgumentException(where + "window / permits must be at least 1 microsecond");
            }
            interval = Math.ceilDiv(Math.ceilDiv(window.toNanos(), 1_000), permits);
            tolerance = Math.multiplyExact(interval, burst);
        } catch (ArithmeticException e) {
            throw new IllegalArgumentException(where + "window, permits and burst overflow", e);
        }
        if (tolerance > MAX_BURST_TOLERANCE_MICROS) {
            throw new IllegalArgumentException(where + "(window / permits) * burst must not exceed 30 days");
        }
    }

    /**
     * The policy of a configured rule; {@code burst} defaults to {@code permits}.
     */
    public static DefaultRateLimitPolicy of(RateLimitRuleConfig rule) {
        return new DefaultRateLimitPolicy(rule.name(), rule.phase(), rule.permits(), rule.window(),
                rule.burst().orElse(rule.permits()), rule.maxKeys());
    }
}
