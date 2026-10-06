package io.quarkus.gradle.application.internal.planning;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

public record AugmentationFacts(List<Path> artifactPaths, Optional<Path> jarPath,
        Optional<Path> libraryDirectory, Optional<Path> nativeResult, boolean requiresLayoutInference) {

    public AugmentationFacts {
        artifactPaths = List.copyOf(artifactPaths);
        jarPath = jarPath == null ? Optional.empty() : jarPath;
        libraryDirectory = libraryDirectory == null ? Optional.empty() : libraryDirectory;
        nativeResult = nativeResult == null ? Optional.empty() : nativeResult;
    }
}
