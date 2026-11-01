package io.quarkus.bootstrap.model.gradle.impl;

import java.util.Objects;

import io.quarkus.bootstrap.model.gradle.GradleProjectIdentity;

public final class DefaultGradleProjectIdentity implements GradleProjectIdentity {

    private static final long serialVersionUID = -3169557076250064908L;

    private final String buildPath;
    private final String projectPath;
    private final String buildTreePath;

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
