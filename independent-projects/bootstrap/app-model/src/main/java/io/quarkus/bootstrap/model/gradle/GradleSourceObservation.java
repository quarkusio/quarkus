package io.quarkus.bootstrap.model.gradle;

import java.io.Serializable;

/**
 * Source path published by a Gradle producer.
 * <p>
 * A source is associated with a logical output only when the producer publishes that association. Project membership
 * alone is not sufficient. Paths are copied from Gradle declarations and are not IDE-owned paths.
 */
public interface GradleSourceObservation extends Serializable {

    /** @return path exactly as copied from Gradle; never {@code null} */
    String getPath();

    /** @return source role reported by the producer, or {@link Role#UNKNOWN}; never {@code null} */
    Role getRole();

    /**
     * Returns a producer-published logical output identity, or {@code null} when the producer did not publish a
     * source-to-output mapping.
     * <p>
     * The value refers to {@link GradleLogicalOutput#getIdentity()} within the same sidecar and is opaque to consumers.
     */
    String getLogicalOutputIdentity();

    /** @return whether the producer published an explicit source-to-output association */
    default boolean hasLogicalOutputAssociation() {
        return getLogicalOutputIdentity() != null;
    }

    /** Kind of source path observed by the producer. */
    enum Role {
        /** The producer did not expose a recognized source role. */
        UNKNOWN,
        /** Java source. */
        JAVA,
        /** Kotlin source. */
        KOTLIN,
        /** Resource source. */
        RESOURCE,
        /** Generated source or resource. */
        GENERATED
    }
}
