package io.quarkus.load.shedding.runtime;

import java.lang.management.ManagementFactory;
import java.util.List;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import com.sun.management.OperatingSystemMXBean;

import io.quarkus.arc.All;
import io.quarkus.load.shedding.RequestClassifier;
import io.quarkus.load.shedding.RequestPrioritizer;
import io.quarkus.load.shedding.RequestPriority;

@Singleton
public class PriorityLoadShedding {
    @Inject
    @All
    List<RequestPrioritizer<?>> requestPrioritizers;

    @Inject
    @All
    List<RequestClassifier<?>> requestClassifiers;

    private final boolean enabled;

    private final int max;

    private final OperatingSystemMXBean os;

    private double lastThreshold;

    private long lastThresholdTime;

    @Inject
    PriorityLoadShedding(LoadSheddingRuntimeConfig config) {
        enabled = config.priority().enabled();
        max = RequestPriority.values().length * RequestClassifier.MAX_COHORT;
        os = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
    }

    // when this is called, we know we're overloaded
    public boolean shedLoad(Object request) {
        if (!enabled) {
            return true;
        }

        long now = System.currentTimeMillis();
        synchronized (this) {
            if (now - lastThresholdTime > 1_000) {
                double load = os.getCpuLoad();
                if (load < 0) {
                    lastThreshold = -1;
                } else {
                    lastThreshold = max * (1.0 - load * load * load);
                }
                lastThresholdTime = now;
            }
        }
        double threshold = lastThreshold;
        if (threshold < 0) {
            return true;
        }

        RequestPriority priority = RequestPriority.NORMAL;
        for (RequestPrioritizer requestPrioritizer : requestPrioritizers) {
            if (requestPrioritizer.appliesTo(request)) {
                priority = requestPrioritizer.priority(request);
                break;
            }
        }

        int cohort = 64; // in the middle of the [1,128] interval
        for (RequestClassifier requestClassifier : requestClassifiers) {
            if (requestClassifier.appliesTo(request)) {
                cohort = requestClassifier.cohort(request);
                break;
            }
        }
        if (cohort == Integer.MIN_VALUE) {
            cohort = RequestClassifier.MAX_COHORT;
        } else if (cohort < 0) {
            cohort = (-cohort) % RequestClassifier.MAX_COHORT + 1;
        } else if (cohort == 0) {
            cohort = RequestClassifier.MIN_COHORT;
        } else if (cohort > RequestClassifier.MAX_COHORT) {
            cohort = cohort % RequestClassifier.MAX_COHORT + 1;
        }

        return priority.cohortBaseline() + cohort > threshold;
    }
}
