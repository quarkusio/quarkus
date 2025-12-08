package io.quarkus.kubernetes.spi;

import java.nio.file.Path;

import io.quarkus.builder.item.SimpleBuildItem;

/**
 * Build item that allows us to supply a custom output dir instead of defaulting to {@code {project.target.dir}/kubernetes}. It
 * differs from {@link KubernetesOutputDirectoryBuildItem} in that it communicates intent to override the output directory while
 * {@link KubernetesOutputDirectoryBuildItem} communicates the effective output directory.
 */
public final class CustomKubernetesOutputDirBuildItem extends SimpleBuildItem {

    private final Path outputDir;

    public CustomKubernetesOutputDirBuildItem(Path outputDir) {
        this.outputDir = outputDir;
    }

    public Path getOutputDir() {
        return outputDir;
    }
}
