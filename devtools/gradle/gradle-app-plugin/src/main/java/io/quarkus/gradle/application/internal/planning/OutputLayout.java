package io.quarkus.gradle.application.internal.planning;

import java.nio.file.Path;
import java.util.Optional;

public record OutputLayout(Path rootDirectory, Path generatedDirectory, Path appDirectory,
        Optional<Path> dependencyFragmentDirectory) {
}
