package io.quarkus.http.ratelimit.runtime.internal;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;

import org.jboss.logging.Logger;

import io.quarkus.http.ratelimit.runtime.internal.gcra.RateLimitClock;

/**
 * Logs a warning that can recur on every request, such as a rule that reaches its {@code max-keys}, at most once per
 * interval on the rate limit clock: the first one is logged at once, and the ones in between are dropped. Each
 * instance throttles on its own, so every kind of warning has its own budget. Created by {@link ThrottledLoggers}.
 */
public final class ThrottledLogger {

    private final Logger log;
    private final RateLimitClock clock;
    private final long intervalNanos;
    private final AtomicLong lastNanos;

    ThrottledLogger(Logger log, RateLimitClock clock, Duration interval) {
        this.log = log;
        this.clock = clock;
        this.intervalNanos = interval.toNanos();
        // one interval in the past, so that the first warning is logged at once
        this.lastNanos = new AtomicLong(clock.nanoTime() - intervalNanos);
    }

    public void warn(String message, Throwable failure) {
        if (tryAcquire()) {
            log.warn(message, failure);
        }
    }

    public void warnf(String format, Object param1, Object param2) {
        if (tryAcquire()) {
            log.warnf(format, param1, param2);
        }
    }

    public void warnf(Throwable failure, String format, Object param1) {
        if (tryAcquire()) {
            log.warnf(failure, format, param1);
        }
    }

    // a compare-and-set, so that of several concurrent callers only one logs
    private boolean tryAcquire() {
        long now = clock.nanoTime();
        long last = lastNanos.get();
        return now - last >= intervalNanos && lastNanos.compareAndSet(last, now);
    }
}
