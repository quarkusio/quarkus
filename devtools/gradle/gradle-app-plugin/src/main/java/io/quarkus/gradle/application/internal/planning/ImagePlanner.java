package io.quarkus.gradle.application.internal.planning;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public final class ImagePlanner {

    public void validateEffectiveReferences(Collection<ImagePlan> selectedPlans) {
        Map<String, ImagePlan> seen = new HashMap<>();
        for (ImagePlan plan : selectedPlans) {
            String reference = plan.image().effectiveReference();
            ImagePlan previous = seen.putIfAbsent(reference, plan);
            if (previous != null && !isAllowedOrderedReplacement(previous, plan)) {
                throw new IllegalArgumentException("Multiple selected Quarkus image tasks use image reference '" + reference
                        + "' without an explicit ordered owner flow");
            }
        }
    }

    private static boolean isAllowedOrderedReplacement(ImagePlan first,
            ImagePlan second) {
        return first.owner().name().equals(second.owner().name())
                && (first.orderedReplacement() || second.orderedReplacement());
    }
}
