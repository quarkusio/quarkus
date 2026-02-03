package io.quarkus.deployment.pkg.builditem;

import java.nio.file.Path;

import io.quarkus.builder.item.SimpleBuildItem;

/**
 * A build item that represents a request to build an AOT enhanced container image
 */
public final class BuildAotOptimizedContainerImageRequestBuildItem extends SimpleBuildItem {

    private final String originalContainerImage;
    private final String containerWorkingDirectory;
    private final Path aotFile;

    public BuildAotOptimizedContainerImageRequestBuildItem(String originalContainerImage,
            String containerWorkingDirectory,
            Path aotFile) {
        this.originalContainerImage = originalContainerImage;
        this.containerWorkingDirectory = containerWorkingDirectory;
        this.aotFile = aotFile;
    }

    public String getOriginalContainerImage() {
        return originalContainerImage;
    }

    public String getContainerWorkingDirectory() {
        return containerWorkingDirectory;
    }

    public Path getAotFile() {
        return aotFile;
    }
}
