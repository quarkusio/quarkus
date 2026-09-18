package io.quarkus.load.shedding.runtime;

import java.util.List;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

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

    @Inject
    PriorityLoadShedding(LoadSheddingRuntimeConfig config) {
        enabled = config.priority().enabled();
        max = RequestPriority.values().length * RequestClassifier.numCohorts();
    }

    // when this is called, we know we're overloaded; `overloadDepth` says by how much,
    // as a number between 0.0 and 1.0
    public boolean shedLoad(Object request, double overloadDepth) {
        if (!enabled) {
            return true;
        }

        // the deeper the overload, the lower the threshold and the more request groups
        // end up above it; the curve is deliberately flat near 0, so that a slight excess
        // over the concurrency limit only sheds the least important requests
        double threshold = max * (1.0 - overloadDepth * overloadDepth * overloadDepth);

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
