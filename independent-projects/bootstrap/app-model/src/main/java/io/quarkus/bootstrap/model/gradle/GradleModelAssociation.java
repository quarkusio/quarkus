package io.quarkus.bootstrap.model.gradle;

import java.io.Serializable;

/**
 * Unambiguous association between a Gradle logical output and an occurrence in
 * the corresponding application model.
 */
public interface GradleModelAssociation extends Serializable {

    Kind getKind();

    /**
     * Returns the surviving application/dependency coordinates, or
     * {@code null} for an unknown association.
     */
    String getArtifactCoordinates();

    /**
     * Returns the exact resolved path occurrence, or {@code null} for an
     * unknown association.
     */
    String getResolvedPath();

    /**
     * Returns the producer-published classifier, or {@code null} when it is
     * unknown.
     */
    String getClassifier();

    default boolean isKnown() {
        return getKind() != Kind.UNKNOWN;
    }

    default boolean isEligibleForOverlayReplacement() {
        return isKnown();
    }

    enum Kind {
        UNKNOWN,
        APPLICATION,
        DEPENDENCY
    }
}
