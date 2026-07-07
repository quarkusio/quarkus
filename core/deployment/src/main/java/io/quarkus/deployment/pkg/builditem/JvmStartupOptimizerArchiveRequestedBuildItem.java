package io.quarkus.deployment.pkg.builditem;

import java.nio.file.Path;

import io.quarkus.builder.item.SimpleBuildItem;

/**
 * Requests generation of a typed JVM startup optimizer archive during augmentation.
 */
public final class JvmStartupOptimizerArchiveRequestedBuildItem extends SimpleBuildItem {

    /**
     * Directory where intermediate files needed for archive generation reside.
     */
    private final Path dir;
    private final JvmStartupOptimizerArchiveType type;

    /**
     * @param dir the working directory for archive-generation intermediate files
     * @param type the archive type to generate
     */
    public JvmStartupOptimizerArchiveRequestedBuildItem(Path dir, JvmStartupOptimizerArchiveType type) {
        this.dir = dir;
        this.type = type;
    }

    /**
     * @return the working directory for archive-generation intermediate files
     */
    public Path getDir() {
        return dir;
    }

    /**
     * @return the archive type to generate
     */
    public JvmStartupOptimizerArchiveType getType() {
        return type;
    }
}
