package io.quarkus.deployment.dev;

import static java.util.Objects.requireNonNull;

import java.nio.file.Path;

/**
 * One normalized file-system change within a declared build-output root.
 *
 * @param outputRoot root whose contents are observed by the dev-mode receiver
 * @param changedPath changed path, which must be lexically at or below
 *        {@code outputRoot} after normalization
 * @param kind type of change
 */
public record BuildOutputPathChange(Path outputRoot, Path changedPath, BuildOutputChangeKind kind) {

    /**
     * Creates, normalizes, and validates lexical path containment. This does
     * not resolve symbolic links or canonicalize filesystem paths.
     */
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
