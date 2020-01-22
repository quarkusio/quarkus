package io.quarkus.bootstrap.app;

import java.nio.file.Path;

public final class JarResult {

    private final Path path;
    private final Path originalArtifact;
    private final Path libraryDir;

    public JarResult(Path path, Path originalArtifact, Path libraryDir) {
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
}
