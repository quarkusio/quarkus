package io.quarkus.load.shedding.runtime;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.jboss.logging.Logger;

import io.quarkus.runtime.configuration.ConfigurationException;

/**
 * An overload detector based on TCP Vegas, as implemented by
 * <a href="https://github.com/Netflix/concurrency-limits/">Netflix Concurrency Limits</a>.
 */
@Singleton
public class OverloadDetector {
    private static final Logger LOG = Logger.getLogger(OverloadDetector.class);

    // The overloaded state may flip very often around the hysteresis band: the band is
    // `0.2 * limit` wide while the number of concurrent requests fluctuates by something like
    // `sqrt(limit)`, so the smaller the limit, the less the count has to move to cross it.
    // At a limit of 1, which the adaptation is allowed to reach and does reach exactly when
    // the application is worst off, every single request is two transitions. Measured rates
    // run into millions per second, so a transition is only ever reported once per this interval;
    // load shedding must not become a source of load itself, and that goes for logging at any
    // level, which is why there is no per-transition DEBUG here either.
    private static final long LOG_INTERVAL_NANOS = TimeUnit.SECONDS.toNanos(10);

    private static final int[] LOG10_PLUS_1_TABLE = new int[1_000];

    static {
        LOG10_PLUS_1_TABLE[0] = 1;
        for (int i = 1; i < 1_000; i++) {
            LOG10_PLUS_1_TABLE[i] = 1 + (int) Math.log10(i);
        }
    }

    static final double HYSTERESIS_FACTOR = 0.8;

    // Controls the initial slope steepness (growth rate) at excess ratio r = 0.
    // Higher values increase initial steepness to shed low-priority requests faster.
    private static final double K = 5.0;

    // Pre-calculated normalization constant. Ensures that overload depth scales
    // precisely to 1.0 at full saturation.
    private static final double TANH_MAX = Math.tanh(K / 2.0);

    private final int maxLimit;
    private final int alphaFactor;
    private final int betaFactor;
    private final double probeFactor;

    private final AtomicInteger currentRequests = new AtomicInteger();
    private final AtomicLong lowestRequestTime = new AtomicLong(Long.MAX_VALUE);
    private final AtomicLong requestsSinceLastProbe = new AtomicLong();
    private volatile long currentLimit;
    // when the current overloaded state was entered, or 0 if the application is not overloaded
    private final AtomicLong overloadedSince = new AtomicLong();

    private final ReentrantLock lock = new ReentrantLock();
    // this field is guarded by `lock`
    private double probeJitter;

    // Which clock interval was last reported, see `logInterval()`. Deliberately neither
    // volatile nor compared-and-set: this only has to keep the log from filling up,
    // so the worst a lost update can do is let one more message through per CPU core.
    // A plain `int` also cannot word-tear, while a plain `long` is allowed to.
    private int lastLogInterval;

    @Inject
    public OverloadDetector(LoadSheddingRuntimeConfig config) {
        validate(config);

        maxLimit = config.maxLimit();
        alphaFactor = config.alphaFactor();
        betaFactor = config.betaFactor();
        probeFactor = config.probeFactor();
        currentLimit = config.initialLimit();

        lock.lock();
        probeJitter = ThreadLocalRandom.current().nextDouble(0.5, 1);
        lock.unlock();

        // so that the first transition is always reported, whatever interval the clock starts in
        lastLogInterval = logInterval() - 1;
    }

    /**
     * The tuning knobs of the overload detection algorithm should typically be left untouched,
     * which is exactly why a mistake in one must not be swallowed silently. Every value rejected
     * here would otherwise produce load shedding that looks like it works and does not.
     */
    private static void validate(LoadSheddingRuntimeConfig config) {
        if (config.maxLimit() < 1) {
            throw new ConfigurationException("quarkus.load-shedding.max-limit must be at least 1, but is "
                    + config.maxLimit());
        }
        if (config.initialLimit() < 1) {
            throw new ConfigurationException("quarkus.load-shedding.initial-limit must be at least 1, but is "
                    + config.initialLimit());
        }
        // we could technically fail on `>=`, but it is a valid configuration (if a bit strange)
        // and makes a lot of tests easier to write
        if (config.initialLimit() > config.maxLimit()) {
            throw new ConfigurationException("quarkus.load-shedding.initial-limit must be less than or equal to"
                    + " max-limit, but is " + config.initialLimit() + " and max-limit is " + config.maxLimit());
        }
        if (config.alphaFactor() < 1) {
            throw new ConfigurationException("quarkus.load-shedding.alpha-factor must be at least 1, but is "
                    + config.alphaFactor());
        }
        if (config.betaFactor() <= config.alphaFactor()) {
            throw new ConfigurationException("quarkus.load-shedding.beta-factor must be greater than"
                    + " alpha-factor, but is " + config.betaFactor() + " and alpha-factor is " + config.alphaFactor());
        }
        if (!(config.probeFactor() > 0.0)) { // also rejects NaN
            throw new ConfigurationException("quarkus.load-shedding.probe-factor must be positive, but is "
                    + config.probeFactor());
        }
    }

    public boolean isOverloaded() {
        return overloadedSince.get() != 0L;
    }

    /**
     * Returns the number of requests that are currently in flight.
     */
    public int currentRequests() {
        return currentRequests.get();
    }

    /**
     * Returns the limit of concurrent requests the algorithm has currently adapted to.
     */
    public long currentLimit() {
        return currentLimit;
    }

    /**
     * Returns how far past the concurrency limit the application currently is, as a number
     * between 0.0 and 1.0. It is 0.0 when the number of current requests is at the current limit.
     * <p>
     * When the number of current requests exceeds the current limit, the result scales smoothly
     * using the right half of a sigmoid curve (hyperbolic tangent). This provides maximum initial
     * steepness to aggressively shed lower-priority requests immediately upon crossing the limit,
     * while smoothly flattening out near 1.0 to provide a broad runway for higher-priority traffic.
     * <p>
     * When the limit is below the maximum limit, the result reaches 1.0 when the number of current
     * requests reaches the maximum limit. When the limit is at the maximum limit, the result
     * reaches 1.0 when the number of current requests reaches 130% of the maximum limit, leaving
     * a 30% safety buffer for the most important requests.
     */
    public double overloadDepth() {
        long limit = currentLimit;
        int current = currentRequests.get();

        if (current <= limit) {
            return 0.0;
        }

        double headroom = Math.max(maxLimit - limit, 0.3 * maxLimit);
        double ratio = (double) (current - limit) / headroom;

        if (ratio >= 1.0) {
            return 1.0;
        }

        double normalizedDepth = Math.tanh((K * ratio) / 2.0) / TANH_MAX;
        return Math.clamp(normalizedDepth, 0.0, 1.0);
    }

    public void requestBegin() {
        updateOverloaded(currentRequests.incrementAndGet());
    }

    public void requestEnd(long timeInMicros) {
        // we divide by the request time when the queue size is estimated below, so it must not be 0
        timeInMicros = Math.max(1, timeInMicros);

        int current = currentRequests.getAndDecrement();

        boolean isLowest = updateLowestRequestTime(timeInMicros);

        updateLimit(timeInMicros, isLowest, current);

        // after `updateLimit()`, so that the possibly adapted limit is used
        updateOverloaded(current - 1);
    }

    private boolean updateLowestRequestTime(long requestTime) {
        long current = lowestRequestTime.get();
        if (current > requestTime) {
            // a lost race means another thread set a lower value, or a probe reset raised
            // the baseline; either way this is not worth retrying
            lowestRequestTime.compareAndSet(current, requestTime);
            return true;
        }
        return false;
    }

    private void updateLimit(long requestTime, boolean isLowest, int currentRequests) {
        // must increment before the lock, so that requests skipped because of contention
        // still count towards the next probe
        requestsSinceLastProbe.incrementAndGet();

        if (!lock.tryLock()) {
            // someone else holds the lock and performs adaptation, we can skip that entirely
            return;
        }
        try {
            long probeCount = requestsSinceLastProbe.get(); // must read inside the lock
            if (probeFactor * probeJitter * currentLimit <= probeCount) {
                probeJitter = ThreadLocalRandom.current().nextDouble(0.5, 1);
                requestsSinceLastProbe.set(0);
                lowestRequestTime.set(requestTime);
                return;
            }

            // this request established a new baseline; the queue size computed below would be 0,
            // which would grow the limit without evidence that there is spare capacity
            if (isLowest) {
                return;
            }

            long currentLimit = this.currentLimit;

            if (2L * currentRequests < currentLimit) {
                return;
            }

            // the `lowestRequestTime` is updated outside the lock, in `updateLowestRequestTime()`
            // it is possible that between `updateLowestRequestTime()` and this point, another thread
            // have raised the `lowestRequestTime` past this request's time, and the queue size
            // below would then go negative, growing the limit as if there were no queue at all
            long lowest = lowestRequestTime.get();
            if (lowest > requestTime) {
                return;
            }

            int queueSize = (int) Math.ceil(currentLimit * (1.0 - (double) lowest / (double) requestTime));

            int currentLimitLog10Plus1;
            if (currentLimit >= 0 && currentLimit < 1_000) {
                currentLimitLog10Plus1 = LOG10_PLUS_1_TABLE[(int) currentLimit];
            } else {
                currentLimitLog10Plus1 = 1 + (int) Math.log10(currentLimit);
            }
            int alpha = alphaFactor * currentLimitLog10Plus1;
            int beta = betaFactor * currentLimitLog10Plus1;

            long newLimit;
            if (queueSize <= currentLimitLog10Plus1) {
                newLimit = currentLimit + beta;
            } else if (queueSize < alpha) {
                newLimit = currentLimit + currentLimitLog10Plus1;
            } else if (queueSize > beta) {
                newLimit = currentLimit - currentLimitLog10Plus1;
            } else {
                return;
            }

            newLimit = Math.clamp(newLimit, 1, maxLimit);
            this.currentLimit = newLimit;
        } finally {
            lock.unlock();
        }
    }

    // the overloaded state is hysteretic: it is entered when the number of concurrent
    // requests reaches the limit and only left when it drops well below it
    private void updateOverloaded(int currentRequests) {
        long limit = currentLimit;
        long since = overloadedSince.get();
        if (since != 0L) {
            // the compare and set also makes the transition happen once, so that only the
            // thread that performed it reports it
            if (currentRequests < limit * HYSTERESIS_FACTOR && overloadedSince.compareAndSet(since, 0L)) {
                logTransition(false, currentRequests, limit);
            }
        } else {
            if (currentRequests >= limit && overloadedSince.compareAndSet(0L, startTimestamp())) {
                logTransition(true, currentRequests, limit);
            }
        }
    }

    // 0 means "not overloaded", so we have to guard against `System.nanoTime()`
    // returning exactly that; a nanosecond skew is not observable
    private static long startTimestamp() {
        long now = System.nanoTime();
        return now == 0L ? 1L : now;
    }

    /**
     * Returns how long the current overloaded state has lasted in nanoseconds,
     * or 0 if the application is not overloaded.
     */
    public long overloadDurationNanos() {
        long since = overloadedSince.get();
        return since == 0L ? 0L : Math.max(0L, System.nanoTime() - since);
    }

    private void logTransition(boolean entered, int currentRequests, long limit) {
        int interval = logInterval();
        if (interval == lastLogInterval) {
            return;
        }
        lastLogInterval = interval;

        LOG.infof("Load shedding %s the overloaded state (%d concurrent requests, limit %d)",
                entered ? "entered" : "left", currentRequests, limit);
    }

    // `System.nanoTime()` divided into intervals of `LOG_INTERVAL_NANOS`. Storing the interval
    // rather than a deadline is what makes a plain field enough: two threads that compute the
    // same interval agree without having to see each other's writes.
    //
    // `floorDiv`, not `/`, because the clock has an arbitrary origin and is quite possibly
    // negative; truncation towards zero would make the one interval containing zero twice as
    // wide.
    private static int logInterval() {
        // fits in an `int` over the whole range of `nanoTime()` at 10s intervals
        return (int) Math.floorDiv(System.nanoTime(), LOG_INTERVAL_NANOS);
    }
}
