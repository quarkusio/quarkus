package io.quarkus.deployment.pkg.builditem;

import java.nio.file.Path;
import java.util.Objects;

import io.quarkus.builder.item.SimpleBuildItem;

/**
 * Requests a container-image provider to derive an image that contains and consumes a JVM startup optimizer archive.
 * <p>
 * Despite the historical AOT-specific class name, the request can carry any
 * {@link JvmStartupOptimizerArchiveType}. The archive path must have the filesystem shape described by that type, and
 * providers place it below the supplied container working directory.
 */
public final class BuildAotOptimizedContainerImageRequestBuildItem extends SimpleBuildItem {

    private final String originalContainerImage;
    private final String containerWorkingDirectory;
    private final JvmStartupOptimizerArchiveType archiveType;
    private final Path archive;

    /**
     * Creates a request for an OpenJDK AOT cache.
     *
     * @param originalContainerImage the image to enhance
     * @param containerWorkingDirectory the working directory in the image, below which the archive is placed
     * @param aotFile the AOT cache file on the build host
     */
    public BuildAotOptimizedContainerImageRequestBuildItem(String originalContainerImage,
            String containerWorkingDirectory,
            Path aotFile) {
        this(originalContainerImage, containerWorkingDirectory, JvmStartupOptimizerArchiveType.AOT, aotFile);
    }

    /**
     * Creates a typed startup-archive image request.
     *
     * @param originalContainerImage the image to enhance
     * @param containerWorkingDirectory the working directory in the image, below which the archive is placed
     * @param archiveType the archive type and expected filesystem shape
     * @param archive the archive file or directory on the build host
     */
    public BuildAotOptimizedContainerImageRequestBuildItem(String originalContainerImage,
            String containerWorkingDirectory,
            JvmStartupOptimizerArchiveType archiveType,
            Path archive) {
        this.originalContainerImage = originalContainerImage;
        this.containerWorkingDirectory = containerWorkingDirectory;
        this.archiveType = Objects.requireNonNull(archiveType, "archiveType");
        this.archive = archive;
    }

    /**
     * @return the image to enhance
     */
    public String getOriginalContainerImage() {
        return originalContainerImage;
    }

    /**
     * @return the working directory in the image, below which the archive is placed
     */
    public String getContainerWorkingDirectory() {
        return containerWorkingDirectory;
    }

    /**
     * Returns the archive through the historical AOT-specific accessor.
     * <p>
     * For a typed request this method returns the same path as {@link #getArchive()}, including when the archive is a
     * directory.
     *
     * @return the startup optimizer archive
     */
    public Path getAotFile() {
        return archive;
    }

    /**
     * @return the type of archive to add to the image
     */
    public JvmStartupOptimizerArchiveType getArchiveType() {
        return archiveType;
    }

    /**
     * @return the archive file or directory on the build host
     */
    public Path getArchive() {
        return archive;
    }

    /**
     * @return the filesystem shape required for {@link #getArchive()}
     */
    public JvmStartupOptimizerArchiveKind getArtifactKind() {
        return archiveType.getArtifactKind();
    }
}
