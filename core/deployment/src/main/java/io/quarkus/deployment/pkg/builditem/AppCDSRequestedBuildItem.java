package io.quarkus.deployment.pkg.builditem;

import java.nio.file.Path;

import io.quarkus.builder.item.SimpleBuildItem;

/**
 * Build item to indicate to the various steps that AppCDS generation
 * has been requested
 */
public final class AppCDSRequestedBuildItem extends SimpleBuildItem {

    /**
     * Directory where various files needed for AppCDS generation will reside
     */
    private final Path dir;
    private final JvmStartupOptimizerArchiveType type;

    public AppCDSRequestedBuildItem(Path dir) {
        this(dir, JvmStartupOptimizerArchiveType.AppCDS);
    }

    public AppCDSRequestedBuildItem(Path dir, JvmStartupOptimizerArchiveType type) {
        this.dir = dir;
        this.type = type;
    }

    public Path getAppCDSDir() {
        return dir;
    }

    public JvmStartupOptimizerArchiveType getType() {
        return type;
    }
}
