package io.quarkus.deployment;

import java.nio.file.Path;

import org.eclipse.microprofile.config.Config;

import io.quarkus.bootstrap.model.ApplicationModel;

public class CodeGenContext {
    private final ApplicationModel model;
    private final Path outDir;
    private final Path workDir;
    private final Path inputDir;
    private final boolean redirectIO;
    private final Config config;

    public CodeGenContext(ApplicationModel model, Path outDir, Path workDir, Path inputDir, boolean redirectIO, Config config) {
        this.model = model;
        this.outDir = outDir;
        this.workDir = workDir;
        this.inputDir = inputDir;
        this.redirectIO = redirectIO;
        this.config = config;
    }

    public ApplicationModel applicationModel() {
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

    public Config config() {
        return config;
    }
}
