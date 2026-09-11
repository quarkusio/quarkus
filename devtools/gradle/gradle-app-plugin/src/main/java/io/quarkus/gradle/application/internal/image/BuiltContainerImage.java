package io.quarkus.gradle.application.internal.image;

import static java.util.Objects.requireNonNull;

import java.util.Optional;

import io.quarkus.gradle.application.model.QuarkusApplicationImageBuilder;

public record BuiltContainerImage(String resultType, Optional<QuarkusApplicationImageBuilder> builder, boolean pushed,
        Optional<String> reference, Optional<String> digest, Optional<String> imageId, Optional<Boolean> pullRequired,
        Optional<String> workingDirectory, Optional<String> outputDirectory) {

    public static final String SCHEMA_VERSION = "1";

    public BuiltContainerImage {
        if (resultType == null || resultType.isBlank()) {
            throw new IllegalArgumentException("Container image result type must not be empty");
        }
        requireNonNull(builder, "builder");
        requireNonNull(reference, "reference");
        requireNonNull(digest, "digest");
        requireNonNull(imageId, "imageId");
        requireNonNull(pullRequired, "pullRequired");
        requireNonNull(workingDirectory, "workingDirectory");
        requireNonNull(outputDirectory, "outputDirectory");
    }
}
