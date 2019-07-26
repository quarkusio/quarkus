package io.quarkus.deployment.pkg.builditem;

import java.nio.file.Path;

import io.quarkus.builder.item.SimpleBuildItem;

/**
 * The location that output artifacts should be created in
 *
 * TODO: should we just create them in temp directories, and leave it up to the integration to move them where they want?
 */
public final class OutputTargetBuildItem extends SimpleBuildItem {

    private final Path outputDirectory;
    private final String baseName;

    public OutputTargetBuildItem(Path outputDirectory, String baseName) {
        this.outputDirectory = outputDirectory;
        this.baseName = baseName;
    }

    public Path getOutputDirectory() {
        return outputDirectory;
    }

    public String getBaseName() {
        return baseName;
    }
}
