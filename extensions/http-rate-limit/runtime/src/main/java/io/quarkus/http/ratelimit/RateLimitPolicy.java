package io.quarkus.http.ratelimit;

import static java.time.temporal.ChronoUnit.MICROS;

import java.time.Duration;

/**
 * The limit of one rule, as resolved from configuration (and possibly a per-key resolved limit).
 * <p>
 * The limit is a GCRA bucket: {@code permits} are replenished evenly over {@code window}, and up to {@code burst}
 * permits can be consumed at once. {@code burst} defaults to {@code permits}.
 * <p>
 * Everything but the rule, the phase, the permits, the window and the burst is derived from them by default methods,
 * so that an implementation, such as one in the tests of a {@link RateLimitStore}, cannot make them disagree.
 */
public interface RateLimitPolicy {

    /**
     * @return the rule name
     */
    String rule();

    /**
     * @return the phase this policy applies to
     */
    RateLimitPhase phase();

    /**
     * @return the number of permits replenished per {@code window}
     */
    long permits();

    /**
     * @return the replenishment window
     */
    Duration window();

    /**
     * @return the maximum number of permits that can be consumed at once
     */
    long burst();

    /**
     * The one quantisation step: everything else derives from it. Rounded up, so that rounding never admits more than
     * {@code permits} per {@code window}.
     *
     * @return {@code window / permits} in microseconds, rounded up
     */
    default long emissionIntervalMicros() {
        return Math.ceilDiv(Math.ceilDiv(window().toNanos(), 1_000), permits());
    }

    /**
     * @return {@code emissionIntervalMicros() * burst}
     */
    default long burstToleranceMicros() {
        return Math.multiplyExact(emissionIntervalMicros(), burst());
    }

    /**
     * @return the emission interval in nanoseconds, for comparison with {@code System.nanoTime()}
     */
    default long emissionIntervalNanos() {
        return Math.multiplyExact(emissionIntervalMicros(), 1_000L);
    }

    /**
     * @return the burst tolerance in nanoseconds
     */
    default long burstToleranceNanos() {
        return Math.multiplyExact(burstToleranceMicros(), 1_000L);
    }

    /**
     * @return the time from an empty bucket to a full one
     */
    default Duration refillTime() {
        return Duration.of(burstToleranceMicros(), MICROS);
    }

    /**
     * @return {@code rule + "/" + phase.configName()}
     */
    default String id() {
        return rule() + "/" + phase().configName();
    }
}
