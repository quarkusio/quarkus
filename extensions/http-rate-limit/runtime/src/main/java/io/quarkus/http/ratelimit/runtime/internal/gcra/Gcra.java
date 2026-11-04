package io.quarkus.http.ratelimit.runtime.internal.gcra;

import io.quarkus.http.ratelimit.RateLimitPolicy;

/**
 * The generic cell rate algorithm: a token bucket stored as a single theoretical arrival time (TAT) per key.
 * <p>
 * All values are nanoseconds on a monotonic clock whose origin is arbitrary (possibly negative, possibly close to the
 * signed wrap). The arithmetic therefore only ever compares elapsed values ({@code tat - now}), never absolute ones.
 * Every store must reproduce exactly this arithmetic.
 */
public final class Gcra {

    /**
     * Marks a missing TAT. Never 0, since {@link System#nanoTime()} has an arbitrary, possibly negative origin.
     */
    public static final long NO_STATE = Long.MIN_VALUE;

    private Gcra() {
    }

    /**
     * {@code retryAfterNanos} is 0 when not applicable; {@code newTat} equals the normalised tat when rejected, which a
     * store writes back only when it differs from the stored one, after a {@linkplain #tryConsume cap}.
     */
    public record Outcome(boolean allowed, long newTat, long remaining, long retryAfterNanos) {
    }

    /**
     * Attempts to consume {@code cost} permits. A {@code cost} of 0 is a probe: the decision and wait are computed as for a
     * cost of 1, but nothing is consumed ({@code newTat} is the normalised tat) and {@code remaining} is the number of
     * permits available before the call.
     * <p>
     * The debt is capped at the burst tolerance. A larger debt cannot come from this policy: it was written under a
     * larger one, before a limit resolver or a configuration change, or by a clock that jumped forward. Uncapped, it would
     * lock the key out for the leftover debt, up to 30 days; capped, it costs the key at most one emission interval.
     */
    public static Outcome tryConsume(long tat, long now, long cost, long emissionIntervalNanos, long burstToleranceNanos) {
        long debt = Math.min(debt(tat, now), burstToleranceNanos); // normalised tat - now, in [0, tau]
        long candidate = debt + Math.max(cost, 1) * emissionIntervalNanos; // candidate tat - now
        long allowAt = candidate - burstToleranceNanos; // allowAt - now
        if (allowAt > 0) {
            return new Outcome(false, now + debt, available(debt, emissionIntervalNanos, burstToleranceNanos), allowAt);
        }
        if (cost == 0) {
            return new Outcome(true, now + debt, available(debt, emissionIntervalNanos, burstToleranceNanos), 0);
        }
        return new Outcome(true, now + candidate, -allowAt / emissionIntervalNanos, 0);
    }

    /**
     * The cost check every store makes before consuming.
     *
     * @throws IllegalArgumentException unless {@code 0 <= cost <= policy.burst()}
     */
    public static void checkCost(long cost, RateLimitPolicy policy) {
        if (cost < 0 || cost > policy.burst()) {
            throw new IllegalArgumentException(
                    "Rate limit " + policy.id() + ": cost must be between 0 and the burst (" + policy.burst() + "), was "
                            + cost);
        }
    }

    /**
     * {@code max(tat, now) - now}, computed on the difference so that it is correct across the signed wrap. A tat at or
     * before {@code now} is equivalent to no state.
     */
    private static long debt(long tat, long now) {
        if (tat == NO_STATE) {
            return 0;
        }
        long debt = tat - now;
        return debt > 0 ? debt : 0;
    }

    private static long available(long debt, long emissionIntervalNanos, long burstToleranceNanos) {
        return (burstToleranceNanos - debt) / emissionIntervalNanos;
    }
}
