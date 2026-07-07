package io.quarkus.gradle.application.internal.planning;

import java.nio.file.Path;
import java.util.Optional;

import io.quarkus.gradle.application.model.QuarkusApplicationBuildDescriptor;

public final class OutputLayoutPlanner {

    public OutputLayout plan(Path buildDirectory, QuarkusApplicationBuildDescriptor descriptor) {
        Path root = buildDirectory.resolve("quarkus-builds").resolve(descriptor.name()).resolve("package");
        return plan(buildDirectory, descriptor, root);
    }

    public OutputLayout plan(Path buildDirectory, QuarkusApplicationBuildDescriptor descriptor, Path root) {
        Optional<Path> dep = descriptor.type().canReuseDependencyFragment()
                ? Optional.of(buildDirectory.resolve("quarkus-build").resolve("dep"))
                : Optional.empty();
        return new OutputLayout(root, root.resolve("gen"), root.resolve("app"), dep);
    }
}
