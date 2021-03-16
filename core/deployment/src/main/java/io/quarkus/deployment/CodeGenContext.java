package io.quarkus.deployment;

import java.nio.file.Path;
import java.util.Map;

import io.quarkus.bootstrap.model.AppModel;

public class CodeGenContext {
    private final AppModel model;
    private final Path outDir;
    private final Path workDir;
    private final Path inputDir;
    private final boolean redirectIO;
    private final Map<String, String> properties;

    public CodeGenContext(AppModel model, Path outDir, Path workDir, Path inputDir, boolean redirectIO,
            Map<String, String> properties) {
        this.model = model;
        this.outDir = outDir;
        this.workDir = workDir;
        this.inputDir = inputDir;
        this.redirectIO = redirectIO;
        this.properties = properties;
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

    public boolean shouldRedirectIO() {
        return redirectIO;
    }

    public Map<String, String> properties() {
        return properties;
    }
}
