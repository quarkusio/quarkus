package io.quarkus.deployment.pkg.builditem;

import java.util.List;
import java.util.Optional;

import io.quarkus.builder.item.SimpleBuildItem;

/**
 * Indicates that a specific container image should be used to generate the AppCDS file
 */
@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public final class JvmStartupOptimizerArchiveContainerImageBuildItem extends SimpleBuildItem {

    private final String containerImage;
    private final Optional<List<String>> additionalJvmArgs;

    @Deprecated(forRemoval = true, since = "3.34")
    public JvmStartupOptimizerArchiveContainerImageBuildItem(String containerImage) {
        this(containerImage, Optional.empty());
    }

    public JvmStartupOptimizerArchiveContainerImageBuildItem(String containerImage, Optional<List<String>> additionalJvmArgs) {
        this.containerImage = containerImage;
        this.additionalJvmArgs = additionalJvmArgs;
    }

    public String getContainerImage() {
        return containerImage;
    }

    public Optional<List<String>> getAdditionalJvmArgs() {
        return additionalJvmArgs;
    }
}
