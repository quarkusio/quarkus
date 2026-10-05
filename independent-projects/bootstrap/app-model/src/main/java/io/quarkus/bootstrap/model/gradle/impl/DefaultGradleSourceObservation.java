package io.quarkus.bootstrap.model.gradle.impl;

import java.util.Objects;

import io.quarkus.bootstrap.model.gradle.GradleSourceObservation;

public final class DefaultGradleSourceObservation implements GradleSourceObservation {

    private static final long serialVersionUID = 2428088945457434848L;

    private final String path;
    private final Role role;
    private final String logicalOutputIdentity;

    public DefaultGradleSourceObservation(String path, Role role, String logicalOutputIdentity) {
        this.path = Objects.requireNonNull(path, "path");
        this.role = Objects.requireNonNull(role, "role");
        this.logicalOutputIdentity = logicalOutputIdentity;
    }

    @Override
    public String getPath() {
        return path;
    }

    @Override
    public Role getRole() {
        return role;
    }

    @Override
    public String getLogicalOutputIdentity() {
        return logicalOutputIdentity;
    }
}
