package io.quarkus.deployment.pkg.builditem;

import java.nio.file.Path;

import io.quarkus.bootstrap.app.JarResult;
import io.quarkus.builder.item.SimpleBuildItem;

public final class JarBuildItem extends SimpleBuildItem {

    private final Path path;
    private final Path originalArtifact;
    private final Path libraryDir;
    private final String type;
    private final String classifier;

    public JarBuildItem(Path path, Path originalArtifact, Path libraryDir, String type, String classifier) {
        this.path = path;
        this.originalArtifact = originalArtifact;
        this.libraryDir = libraryDir;
        this.type = type;
        this.classifier = classifier;
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

    public String getClassifier() {
        return classifier;
    }

    public JarResult toJarResult() {
        return new JarResult(path, originalArtifact, libraryDir, type, classifier);
    }
}
