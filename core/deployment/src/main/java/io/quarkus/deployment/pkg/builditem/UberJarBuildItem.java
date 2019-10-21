package io.quarkus.deployment.pkg.builditem;

import java.nio.file.Path;

import io.quarkus.builder.item.SimpleBuildItem;

public final class UberJarBuildItem extends SimpleBuildItem {

    private final Path path;
    private final Path originalArtifact;

    public UberJarBuildItem(Path path, Path originalArtifact) {
        this.path = path;
        this.originalArtifact = originalArtifact;
    }

    public Path getPath() {
        return path;
    }

    public Path getOriginalArtifact() {
        return originalArtifact;
    }
}
