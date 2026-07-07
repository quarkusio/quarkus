package io.quarkus.gradle.application.internal.planning;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import io.quarkus.bootstrap.app.ArtifactResult;
import io.quarkus.bootstrap.app.AugmentResult;

public final class PackageLayoutInferencePlanner {

    public AugmentationFacts facts(AugmentResult result) {
        if (result == null) {
            return new AugmentationFacts(List.of(), Optional.empty(), Optional.empty(), Optional.empty(),
                    true);
        }

        List<Path> artifactPaths = result.getResults() == null
                ? List.of()
                : result.getResults().stream().map(ArtifactResult::getPath).toList();
        Optional<Path> jarPath = result.getJar() == null ? Optional.empty()
                : Optional.ofNullable(result.getJar().getPath());
        Optional<Path> libraryDirectory = result.getJar() == null ? Optional.empty()
                : Optional.ofNullable(result.getJar().getLibraryDir());
        Optional<Path> nativeResult = Optional.ofNullable(result.getNativeResult());

        boolean requiresLayoutInference = artifactPaths.isEmpty()
                || jarPath.isEmpty()
                || (libraryDirectory.isEmpty() && nativeResult.isEmpty());
        return new AugmentationFacts(artifactPaths, jarPath, libraryDirectory, nativeResult,
                requiresLayoutInference);
    }
}
