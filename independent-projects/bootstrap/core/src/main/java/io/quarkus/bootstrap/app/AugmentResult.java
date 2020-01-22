package io.quarkus.bootstrap.app;

import java.nio.file.Path;
import java.util.List;

/**
 * The result of an augmentation that builds an application
 */
public class AugmentResult {
    private final List<ArtifactResult> results;
    private final JarResult jar;
    private final Path nativeImagePath;

    public AugmentResult(List<ArtifactResult> results, JarResult jar, Path nativeImagePath) {
        this.results = results;
        this.jar = jar;
        this.nativeImagePath = nativeImagePath;
    }

    public List<ArtifactResult> getResults() {
        return results;
    }

    public JarResult getJar() {
        return jar;
    }

    public Path getNativeResult() {
        return nativeImagePath;
    }
}
