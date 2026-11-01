package io.quarkus.bootstrap.model.gradle;

import java.io.Serializable;
import java.util.List;

/**
 * Gradle-specific project and output metadata corresponding to an
 * {@code ApplicationModel} obtained by the same Tooling API request.
 * <p>
 * Paths in this model are Gradle-declared paths. Consumers may use them for
 * correlation, but must not treat them as IDE output paths.
 */
public interface GradleApplicationModelSidecar extends Serializable {

    int CURRENT_SCHEMA_VERSION = 1;

    GradleModelCorrelation getCorrelation();

    GradleProjectIdentity getTargetProject();

    List<? extends GradleProjectComponent> getProjectComponents();

    enum Mode {
        NORMAL,
        DEVELOPMENT,
        TEST
    }
}
