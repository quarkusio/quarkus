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

    int getSchemaVersion();

    GradleApplicationModelSidecar.Mode getMode();

    String getTargetBuildTreePath();

    /**
     * Returns the sorted canonical application-model graph projection.
     * Consumers recompute these facts from the application model.
     */
    List<String> getCanonicalGraphFacts();
}
