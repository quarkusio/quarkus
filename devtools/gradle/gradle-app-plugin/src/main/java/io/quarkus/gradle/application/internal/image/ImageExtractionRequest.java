package io.quarkus.gradle.application.internal.image;

import static java.util.Objects.requireNonNull;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import io.quarkus.bootstrap.app.ArtifactResult;
import io.quarkus.gradle.application.model.QuarkusApplicationImageBuilder;

public record ImageExtractionRequest(Optional<ContainerImageTarget> target,
        Optional<QuarkusApplicationImageBuilder> builder, boolean pushed, List<ArtifactResult> artifactResults,
        Optional<Path> jibDigestFile, Optional<Path> jibImageIdFile) {

    public ImageExtractionRequest {
        target = target == null ? Optional.empty() : target;
        builder = builder == null ? Optional.empty() : builder;
        artifactResults = List.copyOf(artifactResults);
        requireNonNull(jibDigestFile, "jibDigestFile");
        requireNonNull(jibImageIdFile, "jibImageIdFile");
    }
}
