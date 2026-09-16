package io.quarkus.bootstrap.model.gradle;

import java.io.Serializable;

/**
 * Composite-build-safe identity of a Gradle project.
 * <p>
 * All values use Gradle's project identity syntax and are copied from the producer. Consumers should use the build-tree
 * path when they need one identity that distinguishes projects across included builds.
 */
public interface GradleProjectIdentity extends Serializable {

    /** @return Gradle build path, such as {@code :} for the main build; never {@code null} */
    String getBuildPath();

    /** @return project path within its owning Gradle build; never {@code null} */
    String getProjectPath();

    /** @return Gradle build-tree path that identifies the project across included builds; never {@code null} */
    String getBuildTreePath();
}
