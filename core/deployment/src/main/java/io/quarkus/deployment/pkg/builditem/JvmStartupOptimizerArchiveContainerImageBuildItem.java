package io.quarkus.deployment.pkg.builditem;

import java.util.List;
import java.util.Optional;

import io.quarkus.builder.item.SimpleBuildItem;

/**
 * Selects the container image whose JVM generates a startup optimizer archive.
 * <p>
 * The optional JVM arguments supplement the archive recording command when the selected archive type supports
 * recording arguments.
 */
@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public final class JvmStartupOptimizerArchiveContainerImageBuildItem extends SimpleBuildItem {

    private final String containerImage;
    private final Optional<List<String>> additionalJvmArgs;

    /**
     * Creates a container-JVM selection without additional recording arguments.
     *
     * @param containerImage the image whose JVM generates the archive
     */
    @Deprecated(forRemoval = true, since = "3.34")
    public JvmStartupOptimizerArchiveContainerImageBuildItem(String containerImage) {
        this(containerImage, Optional.empty());
    }

    /**
     * @param containerImage the image whose JVM generates the archive
     * @param additionalJvmArgs additional JVM arguments for archive recording, or empty when none are required
     */
    public JvmStartupOptimizerArchiveContainerImageBuildItem(String containerImage, Optional<List<String>> additionalJvmArgs) {
        this.containerImage = containerImage;
        this.additionalJvmArgs = additionalJvmArgs;
    }

    /**
     * @return the image whose JVM generates the archive
     */
    public String getContainerImage() {
        return containerImage;
    }

    /**
     * @return additional JVM arguments for archive recording
     */
    public Optional<List<String>> getAdditionalJvmArgs() {
        return additionalJvmArgs;
    }
}
