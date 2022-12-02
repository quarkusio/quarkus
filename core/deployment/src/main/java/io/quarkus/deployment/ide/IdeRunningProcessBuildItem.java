package io.quarkus.deployment.ide;

import java.util.Set;

import io.quarkus.builder.item.SimpleBuildItem;

/**
 * Contains the set of IDEs that are running as processes
 */
final class IdeRunningProcessBuildItem extends SimpleBuildItem {

    private final Set<Ide> detectedIDEs;

    IdeRunningProcessBuildItem(Set<Ide> detectedIDEs) {
        this.detectedIDEs = detectedIDEs;
    }

    Set<Ide> getDetectedIDEs() {
        return detectedIDEs;
    }
}
