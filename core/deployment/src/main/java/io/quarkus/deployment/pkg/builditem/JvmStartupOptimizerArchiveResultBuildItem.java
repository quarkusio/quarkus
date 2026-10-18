package io.quarkus.deployment.pkg.builditem;

import java.nio.file.Path;

import io.quarkus.builder.item.SimpleBuildItem;

/**
 * A build item containing the result of the JVM startup archive generation process
 */
public final class JvmStartupOptimizerArchiveResultBuildItem extends SimpleBuildItem {

    /**
     * The file containing the generated archive
     */
    private final Path archive;
    /**
     * The type of archive generated
     */
    private final JvmStartupOptimizerArchiveType type;

    public JvmStartupOptimizerArchiveResultBuildItem(Path archive) {
        this(archive, JvmStartupOptimizerArchiveType.AppCDS);
    }

    public JvmStartupOptimizerArchiveResultBuildItem(Path archive, JvmStartupOptimizerArchiveType type) {
        this.archive = archive;
        this.type = type;
    }

    public Path getArchive() {
        return archive;
    }

    public JvmStartupOptimizerArchiveType getType() {
        return type;
    }
}
