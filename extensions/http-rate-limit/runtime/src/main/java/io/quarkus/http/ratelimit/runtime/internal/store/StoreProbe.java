package io.quarkus.http.ratelimit.runtime.internal.store;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import org.jboss.logging.Logger;

import io.quarkus.http.ratelimit.RateLimitDecision;
import io.quarkus.http.ratelimit.RateLimitPolicy;
import io.quarkus.http.ratelimit.runtime.internal.gcra.RateLimitClock;
import io.smallrye.mutiny.subscription.Cancellable;

/**
 * One per non-local store, owned by {@link StoreRegistry}: a task that, while the store is
 * {@linkplain GuardedRateLimitStore#degraded() degraded}, issues one guarded probe, so that recovery never depends on
 * request traffic. Each time the breaker opens, including a reopen after a failed probe, {@link #opened()} schedules
 * the probe for just after the breaker delay ends; a probe the breaker still prevents is retried one breaker delay
 * later, at least 250 ms.
 * <p>
 * The probe passes through the same concurrency limit as every call; slots held by calls that were in flight when the
 * breaker opened are released within the store timeout by the inner deadline, so a saturated limit at probe time is
 * transient. One probe is in flight at a time.
 */
final class StoreProbe implements AutoCloseable {

    private static final Logger LOG = Logger.getLogger(StoreProbe.class);

    /**
     * How long after the end of the breaker delay the probe runs, so that the breaker lets it through.
     */
    private static final Duration AFTER_DELAY = Duration.ofMillis(5);
    private static final Duration MIN_RETRY_PERIOD = Duration.ofMillis(250);

    private final GuardedRateLimitStore guarded;
    private final RateLimitPolicy policy;
    private final RateLimitClock clock;
    private final Duration delay;
    private final Duration retryPeriod;
    private final AtomicBoolean probing = new AtomicBoolean();
    private volatile boolean closed;
    private RateLimitClock.Cancellable timer;
    private volatile Cancellable inFlight;

    /**
     * @param policy any policy of the store, for the zero-cost probe
     * @param delay the delay of the store's breaker
     */
    StoreProbe(GuardedRateLimitStore guarded, RateLimitPolicy policy, RateLimitClock clock, Duration delay) {
        this.guarded = guarded;
        this.policy = policy;
        this.clock = clock;
        this.delay = delay;
        this.retryPeriod = delay.compareTo(MIN_RETRY_PERIOD) < 0 ? MIN_RETRY_PERIOD : delay;
    }

    /**
     * Called each time the breaker opens: the probe runs just after the breaker delay ends.
     */
    void opened() {
        schedule(delay.plus(AFTER_DELAY));
    }

    private synchronized void schedule(Duration after) {
        if (closed) {
            return;
        }
        if (timer != null) {
            timer.cancel();
        }
        timer = clock.schedule(after, new Runnable() {
            @Override
            public void run() {
                tick();
            }
        });
    }

    private void tick() {
        // a healthy store needs no timer: the next opening schedules one through opened()
        if (closed || !guarded.degraded()) {
            return;
        }
        // scheduled first, so that a probe that fails at once and reopens the breaker replaces it through opened()
        schedule(retryPeriod);
        if (probing.compareAndSet(false, true)) {
            inFlight = guarded.probe(policy).subscribe().with(
                    new Consumer<>() {
                        @Override
                        public void accept(RateLimitDecision item) {
                            probing.set(false);
                        }
                    },
                    new Consumer<>() {
                        @Override
                        public void accept(Throwable failure) {
                            probing.set(false);
                            LOG.debugf(failure, "Rate limit store probe failed");
                        }
                    });
        }
    }

    /**
     * Cancels the timer and the subscription of a probe still running.
     */
    @Override
    public void close() {
        synchronized (this) {
            closed = true;
            if (timer != null) {
                timer.cancel();
            }
        }
        Cancellable probe = inFlight;
        if (probe != null) {
            probe.cancel();
        }
    }
}
