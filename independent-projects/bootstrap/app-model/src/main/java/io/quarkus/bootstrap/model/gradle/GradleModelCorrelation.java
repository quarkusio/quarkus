package io.quarkus.bootstrap.model.gradle;

import java.io.Serializable;
import java.util.List;

/**
 * Canonical inputs used to verify that a sidecar and application model belong
 * to the same request.
 * <p>
 * This descriptor detects accidental mismatches. It is neither a cache key nor
 * a security boundary.
 */
public interface GradleModelCorrelation extends Serializable {

    /** @return positive sidecar schema version used to interpret the correlation data */
    int getSchemaVersion();

    /** @return launch mode used to produce the sidecar and application model; never {@code null} */
    GradleApplicationModelSidecar.Mode getMode();

    /** @return composite-build-safe path of the Tooling API target project; never {@code null} */
    String getTargetBuildTreePath();

    /**
     * Returns the sorted canonical application-model graph projection.
     * Consumers recompute these facts from the application model. The representation is a schema-internal correlation
     * value, not a general-purpose model serialization or cache key.
     *
     * @return non-{@code null} sorted list containing no {@code null} elements
     */
    List<String> getCanonicalGraphFacts();
}
