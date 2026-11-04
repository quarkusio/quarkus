package io.quarkus.http.ratelimit.runtime.internal.gcra;

import java.time.Duration;

/**
 * The time source of the rate limiter. Timers and timestamps share one clock, so tests that advance the clock fire
 * due timers deterministically.
 * <p>
 * {@link #nanoTime()} has an arbitrary origin, possibly close to the signed wrap: compare its values only by their
 * difference.
 */
public interface RateLimitClock {

    /**
     * @return the current value of a monotonic nanosecond clock
     */
    long nanoTime();

    /**
     * Runs {@code task} after {@code delay} on the caller's Vert.x context.
     *
     * @return a handle that cancels the task
     */
    Cancellable schedule(Duration delay, Runnable task);

    interface Cancellable {

        void cancel();
    }
}
