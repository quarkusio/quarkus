package io.quarkus.test;

import java.nio.file.Path;
import java.util.List;

import io.quarkus.bootstrap.app.ArtifactResult;

public class ProdModeTestResults {

    private final Path buildDir;
    private final Path builtArtifactPath;
    private final List<ArtifactResult> results;

    public ProdModeTestResults(Path buildDir, Path builtArtifactPath, List<ArtifactResult> results) {
        this.buildDir = buildDir;
        this.builtArtifactPath = builtArtifactPath;
        this.results = results;
    }

    public Path getBuildDir() {
        return buildDir;
    }

    public Path getBuiltArtifactPath() {
        return builtArtifactPath;
    }

    public List<ArtifactResult> getResults() {
        return results;
    }
}
