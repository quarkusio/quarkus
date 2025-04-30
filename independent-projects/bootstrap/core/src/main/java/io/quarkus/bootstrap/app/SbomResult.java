package io.quarkus.bootstrap.app;

import java.nio.file.Path;

public class SbomResult {

    private final Path sbomFile;
    private final String sbomSpec;
    private final String sbomSpecVersion;
    private final String format;
    private final String classifier;
    private final Path appRunner;

    public SbomResult(Path sbomFile, String sbomSpec, String sbomSpecVersion, String format, String classifier,
            Path appRunner) {
        this.sbomFile = sbomFile;
        this.sbomSpec = sbomSpec;
        this.sbomSpecVersion = sbomSpecVersion;
        this.format = format;
        this.classifier = classifier;
        this.appRunner = appRunner;
    }

    public Path getSbomFile() {
        return sbomFile;
    }

    public String getSbomSpec() {
        return sbomSpec;
    }

    public String getSbomSpecVersion() {
        return sbomSpecVersion;
    }

    public String getFormat() {
        return format;
    }

    public String getClassifier() {
        return classifier;
    }

    public Path getApplicationRunner() {
        return appRunner;
    }
}
