package io.quarkus.gradle.application.internal.dev;

import static java.util.Objects.requireNonNull;

import java.nio.file.Path;

import io.quarkus.deployment.dev.BuildOutputChangeKind;

public record GradleDevFileChange(
        GradleDevOutputScope scope,
        Path outputRoot,
        Path changedPath,
        BuildOutputChangeKind kind) {

    public GradleDevFileChange {
        requireNonNull(scope, "scope");
        requireNonNull(outputRoot, "outputRoot");
        requireNonNull(changedPath, "changedPath");
        requireNonNull(kind, "kind");
        outputRoot = outputRoot.normalize();
        changedPath = changedPath.normalize();
        if (!changedPath.startsWith(outputRoot)) {
            throw new IllegalArgumentException("Changed path must be under output root");
        }
    }
}
