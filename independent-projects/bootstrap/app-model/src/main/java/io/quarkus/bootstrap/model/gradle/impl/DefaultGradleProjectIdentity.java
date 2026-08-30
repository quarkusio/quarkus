package io.quarkus.bootstrap.model.gradle.impl;

import java.util.Objects;

import io.quarkus.bootstrap.model.gradle.GradleProjectIdentity;

/**
 * Default serializable value used by Gradle producers to transport a {@link GradleProjectIdentity}.
 */
public final class DefaultGradleProjectIdentity implements GradleProjectIdentity {

    private static final long serialVersionUID = -3169557076250064908L;

    private final String buildPath;
    private final String projectPath;
    private final String buildTreePath;

    /**
     * Creates a composite-build-safe Gradle project identity.
     *
     * @param buildPath Gradle build path; must not be {@code null}
     * @param projectPath project path within its owning build; must not be {@code null}
     * @param buildTreePath project path across the Gradle build tree; must not be {@code null}
     * @throws NullPointerException if an argument is {@code null}
     */
    public DefaultGradleProjectIdentity(String buildPath, String projectPath, String buildTreePath) {
        this.buildPath = Objects.requireNonNull(buildPath, "buildPath");
        this.projectPath = Objects.requireNonNull(projectPath, "projectPath");
        this.buildTreePath = Objects.requireNonNull(buildTreePath, "buildTreePath");
    }

    @Override
    public String getBuildPath() {
        return buildPath;
    }

    @Override
    public String getProjectPath() {
        return projectPath;
    }

    @Override
    public String getBuildTreePath() {
        return buildTreePath;
    }
}
