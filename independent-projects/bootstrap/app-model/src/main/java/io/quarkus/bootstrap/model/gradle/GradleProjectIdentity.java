package io.quarkus.bootstrap.model.gradle;

import java.io.Serializable;

/**
 * Composite-build-safe identity of a Gradle project.
 */
public interface GradleProjectIdentity extends Serializable {

    String getBuildPath();

    String getProjectPath();

    String getBuildTreePath();
}
