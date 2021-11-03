package io.quarkus.deployment.pkg.builditem;

import java.nio.file.Path;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;

import io.quarkus.builder.item.SimpleBuildItem;
import io.quarkus.maven.dependency.ArtifactKey;

/**
 * The location that output artifacts should be created in
 *
 * TODO: should we just create them in temp directories, and leave it up to the integration to move them where they want?
 */
public final class OutputTargetBuildItem extends SimpleBuildItem {

    private final Path outputDirectory;
    private final String baseName;
    private final boolean rebuild;
    private final Properties buildSystemProperties;
    private final Optional<Set<ArtifactKey>> includedOptionalDependencies;

    public OutputTargetBuildItem(Path outputDirectory, String baseName, boolean rebuild, Properties buildSystemProperties,
            Optional<Set<ArtifactKey>> includedOptionalDependencies) {
        this.outputDirectory = outputDirectory;
        this.baseName = baseName;
        this.rebuild = rebuild;
        this.buildSystemProperties = buildSystemProperties;
        this.includedOptionalDependencies = includedOptionalDependencies;
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

    public Properties getBuildSystemProperties() {
        return buildSystemProperties;
    }

    public Optional<Set<ArtifactKey>> getIncludedOptionalDependencies() {
        return includedOptionalDependencies;
    }
}
