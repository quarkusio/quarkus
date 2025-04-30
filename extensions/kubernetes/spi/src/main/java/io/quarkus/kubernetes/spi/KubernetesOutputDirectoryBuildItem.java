package io.quarkus.kubernetes.spi;

import java.nio.file.Path;

import io.quarkus.builder.item.SimpleBuildItem;

/**
 * Contains the effective output directory where to find the generated kubernetes resources.
 */
public final class KubernetesOutputDirectoryBuildItem extends SimpleBuildItem {
    private final Path outputDirectory;

    public KubernetesOutputDirectoryBuildItem(Path outputDirectory) {
        this.outputDirectory = outputDirectory;
    }

    public Path getOutputDirectory() {
        return outputDirectory;
    }
}
