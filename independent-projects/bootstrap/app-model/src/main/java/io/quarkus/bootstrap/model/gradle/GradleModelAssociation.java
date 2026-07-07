package io.quarkus.bootstrap.model.gradle;

import java.io.Serializable;

/**
 * Unambiguous association between a Gradle logical output and an exact occurrence in the corresponding application
 * model.
 * <p>
 * Producers report {@link Kind#UNKNOWN} instead of guessing when coordinates collide, an output occurs more than once,
 * or no single resolved-path occurrence can be proven. Consumers must preserve that uncertainty.
 */
public interface GradleModelAssociation extends Serializable {

    /** @return whether the occurrence belongs to the application, a dependency, or is unknown; never {@code null} */
    Kind getKind();

    /**
     * Returns the surviving application/dependency coordinates, or {@code null} for an unknown association.
     * <p>
     * The coordinate string uses the application model's GACTV representation and is non-{@code null} for a known
     * association.
     */
    String getArtifactCoordinates();

    /**
     * Returns the exact resolved path occurrence, or {@code null} for an unknown association. The value is
     * non-{@code null} for a known association.
     */
    String getResolvedPath();

    /**
     * Returns the producer-published classifier, or {@code null} when it is unknown.
     */
    String getClassifier();

    /** @return whether the producer established an application or dependency association */
    default boolean isKnown() {
        return getKind() != Kind.UNKNOWN;
    }

    /**
     * Returns whether the association is sufficiently specific for a consumer to replace the associated model path with
     * the logical output.
     * <p>
     * The current schema makes every known association eligible.
     */
    default boolean isEligibleForOverlayReplacement() {
        return isKnown();
    }

    /** Location of the associated occurrence in the application model. */
    enum Kind {
        /** No unique association was established; all other association fields are absent. */
        UNKNOWN,
        /** The logical output matches the application artifact. */
        APPLICATION,
        /** The logical output matches a resolved dependency. */
        DEPENDENCY
    }
}
