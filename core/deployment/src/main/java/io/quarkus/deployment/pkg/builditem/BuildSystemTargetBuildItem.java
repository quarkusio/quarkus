package io.quarkus.deployment.pkg.builditem;

import java.nio.file.Path;

import io.quarkus.builder.item.SimpleBuildItem;

/**
 * The build systems target directory. This is used to produce {@link OutputTargetBuildItem}
 */
public final class BuildSystemTargetBuildItem extends SimpleBuildItem {

    private final Path outputDirectory;
    private final String baseName;

    public BuildSystemTargetBuildItem(Path outputDirectory, String baseName) {
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
