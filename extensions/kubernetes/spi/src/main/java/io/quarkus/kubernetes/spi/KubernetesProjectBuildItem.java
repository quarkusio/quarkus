package io.quarkus.kubernetes.spi;

import java.nio.file.Path;

import io.quarkus.builder.item.SimpleBuildItem;

public final class KubernetesProjectBuildItem extends SimpleBuildItem {

    private final Path root;
    private final String group;
    private final String name;
    private final String version;
    private final Path outputFile;

    public KubernetesProjectBuildItem(Path root, String group, String name, String version, Path outputFile) {
        this.root = root;
        this.group = group;
        this.name = name;
        this.version = version;
        this.outputFile = outputFile;
    }

    public Path getRoot() {
        return root;
    }

    public String getGroup() {
        return group;
    }

    public String getName() {
        return name;
    }

    public String getVersion() {
        return version;
    }

    public Path getOutputFile() {
        return outputFile;
    }
}
