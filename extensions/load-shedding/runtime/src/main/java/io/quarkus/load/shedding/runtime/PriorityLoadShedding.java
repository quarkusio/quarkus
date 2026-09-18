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
        max = RequestPriority.values().length * RequestClassifier.numCohorts();
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

        int cohort = RequestClassifier.numCohorts() / 2; // in the middle of the interval
        for (RequestClassifier requestClassifier : requestClassifiers) {
            if (requestClassifier.appliesTo(request)) {
                cohort = requestClassifier.cohort(request);
                break;
            }
        }
        cohort &= RequestClassifier.maxCohort();

        return priority.cohortBaseline() + cohort >= threshold;
    }
}
