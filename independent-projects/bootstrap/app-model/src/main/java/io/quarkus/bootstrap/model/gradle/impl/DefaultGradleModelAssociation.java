package io.quarkus.bootstrap.model.gradle.impl;

import java.util.Objects;

import io.quarkus.bootstrap.model.gradle.GradleModelAssociation;

/**
 * Default serializable value used by Gradle producers to transport a {@link GradleModelAssociation}.
 * <p>
 * Unknown associations carry no coordinates, path, or classifier. Known associations identify one exact resolved-path
 * occurrence and may omit only the classifier.
 */
public final class DefaultGradleModelAssociation implements GradleModelAssociation {

    private static final long serialVersionUID = -7371983253634892542L;

    private final Kind kind;
    private final String artifactCoordinates;
    private final String resolvedPath;
    private final String classifier;

    /**
     * Creates a model association.
     *
     * @param kind association kind; must not be {@code null}
     * @param artifactCoordinates application-model GACTV coordinates; required for known associations and otherwise
     *        {@code null}
     * @param resolvedPath exact resolved path occurrence; required for known associations and otherwise {@code null}
     * @param classifier producer-published classifier for a known association, or {@code null}; must be {@code null}
     *        for an unknown association
     * @throws NullPointerException if {@code kind}, or a required known-association field, is {@code null}
     * @throws IllegalArgumentException if an unknown association contains coordinates, a path, or a classifier
     */
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

    /** @return an explicit unknown association with no coordinates, path, or classifier */
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
