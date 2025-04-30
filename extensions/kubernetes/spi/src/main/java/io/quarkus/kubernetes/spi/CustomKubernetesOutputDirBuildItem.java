package io.quarkus.kubernetes.spi;

import java.nio.file.Path;

import io.quarkus.builder.item.SimpleBuildItem;

/**
 * Build item that allows us to supply a custom output dir instead of defaulting to {project.target.dir}/kubernetes
 * It's different from the {@link KubernetesOutputDirBuildItem} as it's used to communicate the intention to override the dir
 * while {@link KubernetesOutputDirBuildItem} is used to communicate the effective output dir.
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
