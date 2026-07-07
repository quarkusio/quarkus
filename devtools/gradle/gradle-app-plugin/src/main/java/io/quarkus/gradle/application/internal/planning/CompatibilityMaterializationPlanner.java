package io.quarkus.gradle.application.internal.planning;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import io.quarkus.gradle.application.model.QuarkusApplicationBuildDescriptor;
import io.quarkus.gradle.application.model.QuarkusApplicationBuildType;

public final class CompatibilityMaterializationPlanner {

    public CompatibilityMaterializationPlan plan(Path buildDirectory,
            QuarkusApplicationBuildDescriptor descriptor) {
        List<Path> outputs = new ArrayList<>();
        if (descriptor.type() == QuarkusApplicationBuildType.FAST_JAR) {
            outputs.add(buildDirectory.resolve("quarkus-app"));
        }
        if (descriptor.type() == QuarkusApplicationBuildType.LEGACY_JAR) {
            outputs.add(buildDirectory.resolve("lib"));
        }
        if (descriptor.type() == QuarkusApplicationBuildType.NATIVE_EXECUTABLE
                || descriptor.type() == QuarkusApplicationBuildType.NATIVE_SOURCES) {
            outputs.add(buildDirectory.resolve("native-sources"));
        }
        return new CompatibilityMaterializationPlan(outputs);
    }
}
