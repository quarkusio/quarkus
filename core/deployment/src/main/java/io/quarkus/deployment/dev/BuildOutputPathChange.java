package io.quarkus.deployment.dev;

import static java.util.Objects.requireNonNull;

import java.nio.file.Path;

public record BuildOutputPathChange(Path outputRoot, Path changedPath, BuildOutputChangeKind kind) {

    public BuildOutputPathChange {
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
