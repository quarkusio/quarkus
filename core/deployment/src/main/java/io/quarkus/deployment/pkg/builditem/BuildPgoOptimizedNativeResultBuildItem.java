package io.quarkus.deployment.pkg.builditem;

import java.nio.file.Path;

import io.quarkus.builder.item.SimpleBuildItem;

/**
 * A build item that represents the result of building a PGO-optimized native image
 */
public final class BuildPgoOptimizedNativeResultBuildItem extends SimpleBuildItem {

    private final Path optimizedBinaryPath;

    public BuildPgoOptimizedNativeResultBuildItem(Path optimizedBinaryPath) {
        this.optimizedBinaryPath = optimizedBinaryPath;
    }

    public Path getOptimizedBinaryPath() {
        return optimizedBinaryPath;
    }
}
