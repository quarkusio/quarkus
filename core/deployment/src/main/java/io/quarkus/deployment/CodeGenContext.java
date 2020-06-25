package io.quarkus.deployment;

import java.nio.file.Path;

import io.quarkus.bootstrap.model.AppModel;

public class CodeGenContext {
    private final AppModel model;
    private final Path outDir;
    private final Path workDir;
    private final Path inputDir;

    public CodeGenContext(AppModel model, Path outDir, Path workDir, Path inputDir) {
        this.model = model;
        this.outDir = outDir;
        this.workDir = workDir;
        this.inputDir = inputDir;
    }

    public AppModel appModel() {
        return model;
    }

    public Path outDir() {
        return outDir;
    }

    public Path workDir() {
        return workDir;
    }

    public Path inputDir() {
        return inputDir;
    }
}
