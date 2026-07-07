package io.quarkus.gradle.application.internal.planning;

import java.nio.file.Path;
import java.util.List;

public record CompatibilityMaterializationPlan(List<Path> legacyOutputPaths) {
    public CompatibilityMaterializationPlan {
        legacyOutputPaths = List.copyOf(legacyOutputPaths);
    }
}
