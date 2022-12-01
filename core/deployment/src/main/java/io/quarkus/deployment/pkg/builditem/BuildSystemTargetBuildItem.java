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
    private final String originalBaseName;
    private final boolean rebuild;
    private final Properties buildSystemProps;

    /**
     * @deprecated in favor of {@link #BuildSystemTargetBuildItem(Path, String, String, boolean, Properties)}
     *
     * @param outputDirectory build output directory
     * @param baseName base runner name
     * @param rebuild indicates whether the application is being re-built
     * @param buildSystemProps build system properties
     */
    @Deprecated(forRemoval = true)
    public BuildSystemTargetBuildItem(Path outputDirectory, String baseName, boolean rebuild, Properties buildSystemProps) {
        this(outputDirectory, baseName, baseName, rebuild, buildSystemProps);
    }

    public BuildSystemTargetBuildItem(Path outputDirectory, String baseName, String originalBaseName, boolean rebuild,
            Properties buildSystemProps) {
        this.outputDirectory = outputDirectory;
        this.baseName = baseName;
        this.originalBaseName = originalBaseName;
        this.rebuild = rebuild;
        this.buildSystemProps = buildSystemProps;
    }

    public Path getOutputDirectory() {
        return outputDirectory;
    }

    public String getBaseName() {
        return baseName;
    }

    public String getOriginalBaseName() {
        return originalBaseName;
    }

    public boolean isRebuild() {
        return rebuild;
    }

    public Properties getBuildSystemProps() {
        return buildSystemProps;
    }
}
