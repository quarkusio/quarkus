package io.quarkus.deployment.pkg.builditem;

import java.nio.file.Path;

import io.quarkus.builder.item.SimpleBuildItem;

/**
 * Build item to indicate to the various steps that generation
 * of a JVM startup archive has been requested
 */
public final class JvmStartupOptimizerArchiveRequestedBuildItem extends SimpleBuildItem {

    /**
     * Directory where various files needed for JVM startup archive generation will reside
     */
    private final Path dir;
    private final JvmStartupOptimizerArchiveType type;

    public JvmStartupOptimizerArchiveRequestedBuildItem(Path dir, JvmStartupOptimizerArchiveType type) {
        this.dir = dir;
        this.type = type;
    }

    public Path getDir() {
        return dir;
    }

    public JvmStartupOptimizerArchiveType getType() {
        return type;
    }
}
