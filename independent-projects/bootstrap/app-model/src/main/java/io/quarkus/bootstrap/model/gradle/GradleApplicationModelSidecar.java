package io.quarkus.bootstrap.model.gradle;

import java.io.Serializable;
import java.util.List;

/**
 * Gradle-specific project and output metadata corresponding to an {@code ApplicationModel} obtained by the same Tooling
 * API request.
 * <p>
 * This serializable interface is the consumer-facing Tooling API model; producers normally return the implementation
 * classes in the {@code impl} package. Paths are copied from Gradle declarations. Consumers may use them for
 * correlation, but must not treat them as IDE-owned output paths.
 * <p>
 * Consumers must validate the correlation data before pairing the sidecar with an application model. The schema
 * version allows a consumer to reject a shape it does not understand; it does not by itself promise cross-version
 * compatibility.
 */
public interface GradleApplicationModelSidecar extends Serializable {

    /** Schema version produced by this Quarkus version. */
    int CURRENT_SCHEMA_VERSION = 1;

    /** @return data used to verify the associated application model and Tooling API request; never {@code null} */
    GradleModelCorrelation getCorrelation();

    /** @return composite-build-safe identity of the requested project; never {@code null} */
    GradleProjectIdentity getTargetProject();

    /**
     * @return project components observed while resolving the target model; the default producer implementation
     *         returns an immutable, non-{@code null} list
     */
    List<? extends GradleProjectComponent> getProjectComponents();

    /**
     * Quarkus launch-mode semantics used to produce both models.
     */
    enum Mode {
        /** Production/application execution model. */
        NORMAL,
        /** Development-mode model. */
        DEVELOPMENT,
        /** Test execution model. */
        TEST
    }
}
