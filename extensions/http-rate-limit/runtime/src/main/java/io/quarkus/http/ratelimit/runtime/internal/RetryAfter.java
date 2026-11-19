package io.quarkus.http.ratelimit.runtime.internal;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;

final class RetryAfter {

    private RetryAfter() {
    }

    /**
     * {@code max(1, ceil(wait in seconds))}.
     */
    static long seconds(Duration wait) {
        return Math.max(1, wait.getSeconds() + (wait.getNano() > 0 ? 1 : 0));
    }

    /**
     * For the 503 of the {@code closed} failure mode, whose wait is the same for every client: a fixed value would bring
     * every refused client back at the instant the circuit breaker probes the store, and that wave could open it
     * again.
     *
     * @return {@link #seconds(Duration)}, jittered uniformly up to twice its value
     */
    static long jitteredSeconds(Duration wait) {
        long base = seconds(wait);
        return ThreadLocalRandom.current().nextLong(base, 2 * base + 1);
    }
}
