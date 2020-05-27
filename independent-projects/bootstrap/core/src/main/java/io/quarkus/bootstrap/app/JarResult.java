package io.quarkus.bootstrap.app;

import java.nio.file.Path;

public final class JarResult {

    private final Path path;
    private final Path originalArtifact;
    private final Path libraryDir;
    private final String type;

    public JarResult(Path path, Path originalArtifact, Path libraryDir, String type) {
        this.path = path;
        this.originalArtifact = originalArtifact;
        this.libraryDir = libraryDir;
        this.type = type;
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

    public String getType() {
        return type;
    }

}
