package io.quarkus.builder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class BuildStepBuilderReshuffler {

    private static final String LOGGING_RUNTIME_SETUP_STEP = "io.quarkus.deployment.logging.LoggingResourceProcessor#setupLoggingRuntimeInit";
    private static final String LOGGING_STATIC_SETUP_STEP = "io.quarkus.deployment.logging.LoggingResourceProcessor#setupLoggingStaticInit";

    static void reshuffle(final Set<BuildStepBuilder> included,
            final Map<BuildStepBuilder, Set<BuildStepBuilder>> dependents) {
        BuildStepBuilder loggingRuntimeBuilder = findLoggingBuilder(included, LOGGING_RUNTIME_SETUP_STEP);

        if (loggingRuntimeBuilder != null) {
            List<BuildStepBuilder> reSorted = new ArrayList<>();
            List<BuildStepBuilder> pullsInLogging = new ArrayList<>();

            for (BuildStepBuilder b : included) {
                if (b == loggingRuntimeBuilder) {
                    continue;
                }

                // Does this step eventually lead to logging?
                if (leadsTo(b, loggingRuntimeBuilder, dependents, new HashSet<>())) {
                    pullsInLogging.add(b);
                } else {
                    reSorted.add(b);
                }
            }

            reSorted.addAll(pullsInLogging);
            reSorted.add(loggingRuntimeBuilder);

            included.clear();
            included.addAll(reSorted); // preserves this specific order
        }
    }

    private static BuildStepBuilder findLoggingBuilder(Set<BuildStepBuilder> included, String loggingStep) {
        BuildStepBuilder loggingBuilder = null;
        for (BuildStepBuilder b : included) {
            if (loggingStep.equals(b.getBuildStep().getId())) {
                loggingBuilder = b;
                break;
            }
        }
        return loggingBuilder;
    }

    // Helper to check transitive dependency path
    private static boolean leadsTo(BuildStepBuilder current, BuildStepBuilder target,
            Map<BuildStepBuilder, Set<BuildStepBuilder>> dependents, Set<BuildStepBuilder> visited) {
        if (current == target) {
            return true;
        }
        if (!visited.add(current)) {
            return false;
        }
        for (BuildStepBuilder dep : dependents.getOrDefault(current, Collections.emptySet())) {
            if (leadsTo(dep, target, dependents, visited)) {
                return true;
            }
        }
        return false;
    }
}
