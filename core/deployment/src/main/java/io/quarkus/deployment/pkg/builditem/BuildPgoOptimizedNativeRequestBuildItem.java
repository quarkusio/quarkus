package io.quarkus.deployment.pkg.builditem;

import java.nio.file.Path;

import io.quarkus.builder.item.SimpleBuildItem;

/**
 * A build item that represents a request to build a PGO-optimized native image
 */
public final class BuildPgoOptimizedNativeRequestBuildItem extends SimpleBuildItem {

    private final Path profilePath;

    public BuildPgoOptimizedNativeRequestBuildItem(Path profilePath) {
        this.profilePath = profilePath;
    }

    public Path getProfilePath() {
        return profilePath;
    }
}
