package io.quarkus.deployment.ide;

import java.util.Set;

import io.quarkus.builder.item.SimpleBuildItem;

/**
 * Contains the set of IDEs that could be potentially linked to project based
 * on the files present in the project
 */
final class IdeFileBuildItem extends SimpleBuildItem {

    private final Set<Ide> detectedIDEs;

    IdeFileBuildItem(Set<Ide> detectedIDEs) {
        this.detectedIDEs = detectedIDEs;
    }

    Set<Ide> getDetectedIDEs() {
        return detectedIDEs;
    }
}
