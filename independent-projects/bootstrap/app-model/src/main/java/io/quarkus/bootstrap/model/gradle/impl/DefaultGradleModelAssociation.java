package io.quarkus.bootstrap.model.gradle.impl;

import java.util.Objects;

import io.quarkus.bootstrap.model.gradle.GradleModelAssociation;

public final class DefaultGradleModelAssociation implements GradleModelAssociation {

    private static final long serialVersionUID = -7371983253634892542L;

    private final Kind kind;
    private final String artifactCoordinates;
    private final String resolvedPath;
    private final String classifier;

    public DefaultGradleModelAssociation(Kind kind, String artifactCoordinates, String resolvedPath, String classifier) {
        this.kind = Objects.requireNonNull(kind, "kind");
        if (kind == Kind.UNKNOWN) {
            if (artifactCoordinates != null || resolvedPath != null || classifier != null) {
                throw new IllegalArgumentException("An unknown model association cannot contain model coordinates or paths");
            }
        } else {
            Objects.requireNonNull(artifactCoordinates, "artifactCoordinates");
            Objects.requireNonNull(resolvedPath, "resolvedPath");
        }
        this.artifactCoordinates = artifactCoordinates;
        this.resolvedPath = resolvedPath;
        this.classifier = classifier;
    }

    public static DefaultGradleModelAssociation unknown() {
        return new DefaultGradleModelAssociation(Kind.UNKNOWN, null, null, null);
    }

    @Override
    public Kind getKind() {
        return kind;
    }

    @Override
    public String getArtifactCoordinates() {
        return artifactCoordinates;
    }

    @Override
    public String getResolvedPath() {
        return resolvedPath;
    }

    @Override
    public String getClassifier() {
        return classifier;
    }
}
