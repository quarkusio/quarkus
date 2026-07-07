package io.quarkus.deployment.pkg.builditem;

import java.nio.file.Path;

import io.quarkus.builder.item.SimpleBuildItem;

/**
 * Reports the typed JVM startup optimizer archive generated during augmentation.
 * <p>
 * The reported path is a regular file or directory according to {@link JvmStartupOptimizerArchiveType#getArtifactKind()}.
 */
public final class JvmStartupOptimizerArchiveResultBuildItem extends SimpleBuildItem {

    /**
     * The file or directory containing the generated archive.
     */
    private final Path archive;
    /**
     * The type of archive generated
     */
    private final JvmStartupOptimizerArchiveType type;

    /**
     * Creates an AppCDS result for compatibility with the original archive contract.
     *
     * @param archive the generated AppCDS archive file
     */
    public JvmStartupOptimizerArchiveResultBuildItem(Path archive) {
        this(archive, JvmStartupOptimizerArchiveType.AppCDS);
    }

    /**
     * @param archive the generated archive file or directory
     * @param type the generated archive type
     */
    public JvmStartupOptimizerArchiveResultBuildItem(Path archive, JvmStartupOptimizerArchiveType type) {
        this.archive = archive;
        this.type = type;
    }

    /**
     * @return the generated archive file or directory
     */
    public Path getArchive() {
        return archive;
    }

    /**
     * @return the generated archive type
     */
    public JvmStartupOptimizerArchiveType getType() {
        return type;
    }
}
