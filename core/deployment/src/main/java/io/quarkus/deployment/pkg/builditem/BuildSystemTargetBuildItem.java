package io.quarkus.deployment.pkg.builditem;

import java.nio.file.Path;
import java.util.Properties;

import io.quarkus.builder.item.SimpleBuildItem;

/**
 * The build systems target directory. This is used to produce {@link OutputTargetBuildItem}
 */
public final class BuildSystemTargetBuildItem extends SimpleBuildItem {

    private final Path outputDirectory;
    private final String baseName;
    private final boolean rebuild;
    private final Properties buildSystemProps;

    public BuildSystemTargetBuildItem(Path outputDirectory, String baseName, boolean rebuild, Properties buildSystemProps) {
        this.outputDirectory = outputDirectory;
        this.baseName = baseName;
        this.rebuild = rebuild;
        this.buildSystemProps = buildSystemProps;
    }

    public Path getOutputDirectory() {
        return outputDirectory;
    }

    public String getBaseName() {
        return baseName;
    }

    public boolean isRebuild() {
        return rebuild;
    }

    public Properties getBuildSystemProps() {
        return buildSystemProps;
    }
}
