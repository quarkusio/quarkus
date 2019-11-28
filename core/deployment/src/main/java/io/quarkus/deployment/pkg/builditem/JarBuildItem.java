package io.quarkus.deployment.pkg.builditem;

import java.nio.file.Path;

import io.quarkus.bootstrap.app.JarResult;
import io.quarkus.builder.item.SimpleBuildItem;

public final class JarBuildItem extends SimpleBuildItem {

    private final Path path;
    private final Path originalArtifact;
    private final Path libraryDir;

    public JarBuildItem(Path path, Path originalArtifact, Path libraryDir) {
        this.path = path;
        this.originalArtifact = originalArtifact;
        this.libraryDir = libraryDir;
    }

    public boolean isUberJar() {
        return libraryDir == null;
    }

    public Path getPath() {
        return path;
    }

    public Path getLibraryDir() {
        return libraryDir;
    }

    public Path getOriginalArtifact() {
        return originalArtifact;
    }

    public JarResult toJarResult() {
        return new JarResult(path, originalArtifact, libraryDir);
    }
}
