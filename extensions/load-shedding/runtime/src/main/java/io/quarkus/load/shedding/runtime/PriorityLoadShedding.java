package io.quarkus.load.shedding.runtime;

import java.util.List;
import java.util.concurrent.TimeUnit;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.jboss.logging.Logger;

import io.quarkus.arc.All;
import io.quarkus.load.shedding.RequestClassifier;
import io.quarkus.load.shedding.RequestPrioritizer;
import io.quarkus.load.shedding.RequestPriority;

@Singleton
public class PriorityLoadShedding {
    private static final Logger LOG = Logger.getLogger(PriorityLoadShedding.class);

    private static final int LOG_INTERVAL_SECONDS = 10;

    // Failing prioritizer or classifier can easily fail for every request, and this runs once per
    // request for as long as the overload lasts, so reporting each failure would make load
    // shedding a source of load itself. One report per interval is enough, because the next
    // failure is most likely the same failure from the same implementation.
    private static final long LOG_INTERVAL_NANOS = TimeUnit.SECONDS.toNanos(LOG_INTERVAL_SECONDS);

    @Inject
    @All
    List<RequestPrioritizer<?>> requestPrioritizers;

    @Inject
    @All
    List<RequestClassifier<?>> requestClassifiers;

    private final boolean enabled;

    private final int max;

    // Which clock interval was last reported, see `logInterval()`. Deliberately neither
    // volatile nor compared-and-set: this only has to keep the log from filling up,
    // so the worst a lost update can do is let one more message through per CPU core.
    // A plain `int` also cannot word-tear, while a plain `long` is allowed to.
    private int lastLogInterval;

    @Inject
    PriorityLoadShedding(LoadSheddingRuntimeConfig config) {
        enabled = config.priority().enabled();
        max = RequestPriority.levels().size() * RequestClassifier.numCohorts();

        // so that the first failure is always reported, whatever interval the clock starts in
        lastLogInterval = logInterval() - 1;
    }

    // when this is called, we know we're overloaded; `overloadDepth` says by how much,
    // as a number between 0.0 and 1.0
    public boolean shedLoad(Object request, double overloadDepth) {
        if (!enabled) {
            return true;
        }

        RequestPriority priority = priorityOf(request);
        if (priority == RequestPriority.NEVER) {
            return false;
        }

        int cohort = cohortOf(request) & RequestClassifier.maxCohort();

        // the deeper the overload, the lower the threshold and the more request groups
        // end up above it; the curve is deliberately flat near 0, so that a slight excess
        // over the concurrency limit only sheds the least important requests
        double threshold = max * (1.0 - overloadDepth * overloadDepth * overloadDepth);

        return priority.cohortBaseline() + cohort >= threshold;
    }

    // broken `RequestPrioritizer` and `RequestClassifier` implementations are treated
    // as not applying to the request, so that the decision falls through to the next one
    // and ultimately to the defaults

    private RequestPriority priorityOf(Object request) {
        for (RequestPrioritizer requestPrioritizer : requestPrioritizers) {
            try {
                if (requestPrioritizer.appliesTo(request)) {
                    RequestPriority priority = requestPrioritizer.priority(request);
                    if (priority != null) {
                        return priority;
                    }
                    logFailure(requestPrioritizer, new NullPointerException("priority() returned null"));
                }
            } catch (RuntimeException e) {
                // `appliesTo()` that is more permissive than the type parameter of the implementation
                // may easily result in a `ClassCastException`
                logFailure(requestPrioritizer, e);
            }
        }
        return RequestPriority.NORMAL;
    }

    private int cohortOf(Object request) {
        for (RequestClassifier requestClassifier : requestClassifiers) {
            try {
                if (requestClassifier.appliesTo(request)) {
                    return requestClassifier.cohort(request);
                }
            } catch (RuntimeException e) {
                logFailure(requestClassifier, e);
            }
        }
        return RequestClassifier.numCohorts() / 2; // in the middle of the interval
    }

    private void logFailure(Object implementation, RuntimeException error) {
        int interval = logInterval();
        if (interval == lastLogInterval) {
            return;
        }
        lastLogInterval = interval;

        LOG.warnf(error, "Ignoring '%s' for this request because it failed."
                + " Further failures are only reported once per %d seconds.",
                implementation.getClass().getName(), LOG_INTERVAL_SECONDS);
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
