package io.quarkus.bootstrap.model.gradle;

import java.io.Serializable;

/**
 * Source path published by a Gradle producer.
 * <p>
 * A source is associated with a logical output only when the producer
 * publishes that association. Project membership alone is not sufficient.
 */
public interface GradleSourceObservation extends Serializable {

    String getPath();

    Role getRole();

    /**
     * Returns a producer-published logical output identity, or {@code null}
     * when the producer did not publish a source-to-output mapping.
     */
    String getLogicalOutputIdentity();

    default boolean hasLogicalOutputAssociation() {
        return getLogicalOutputIdentity() != null;
    }

    enum Role {
        UNKNOWN,
        JAVA,
        KOTLIN,
        RESOURCE,
        GENERATED
    }
}
