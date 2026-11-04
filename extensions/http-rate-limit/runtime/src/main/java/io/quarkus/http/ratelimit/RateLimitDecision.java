package io.quarkus.http.ratelimit;

import java.time.Duration;

/**
 * The outcome of a {@link RateLimitStore#tryConsume} call.
 */
public sealed interface RateLimitDecision {

    /**
     * @param remaining the whole permits left after the call
     * @return an {@link Allowed} decision
     * @throws IllegalArgumentException if remaining is negative
     */
    static Allowed allowed(long remaining) {
        return new AllowedDecision(remaining);
    }

    /**
     * @param retryAfter the wait until the requested number of permits is available
     * @return a {@link Rejected} decision
     * @throws IllegalArgumentException if retryAfter is not positive or cannot be represented in nanoseconds
     * @throws NullPointerException if {@code retryAfter} is {@code null}
     */
    static Rejected rejected(Duration retryAfter) {
        return new RejectedDecision(retryAfter);
    }

    /**
     * The request is admitted.
     */
    sealed interface Allowed extends RateLimitDecision permits AllowedDecision {

        /**
         * @return the whole permits left after the call
         */
        long remaining();
    }

    /**
     * The request is rejected without consuming permits.
     */
    sealed interface Rejected extends RateLimitDecision permits RejectedDecision {

        /**
         * @return the wait until the requested number of permits is available
         */
        Duration retryAfter();
    }

}
