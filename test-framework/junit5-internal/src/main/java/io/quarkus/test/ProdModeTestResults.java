package io.quarkus.test;

import java.nio.file.Path;
import java.util.List;
import java.util.logging.LogRecord;

import io.quarkus.bootstrap.app.ArtifactResult;

public class ProdModeTestResults {

    private final Path buildDir;
    private final Path builtArtifactPath;
    private final List<ArtifactResult> results;
    private final List<LogRecord> retainedBuildLogRecords;

    public ProdModeTestResults(Path buildDir, Path builtArtifactPath, List<ArtifactResult> results,
            List<LogRecord> retainedBuildLogRecords) {
        this.buildDir = buildDir;
        this.builtArtifactPath = builtArtifactPath;
        this.results = results;
        this.retainedBuildLogRecords = retainedBuildLogRecords;
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

    public List<LogRecord> getRetainedBuildLogRecords() {
        return retainedBuildLogRecords;
    }
}
