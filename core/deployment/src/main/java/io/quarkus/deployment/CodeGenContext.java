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
    private final boolean test;

    public CodeGenContext(ApplicationModel model, Path outDir, Path workDir, Path inputDir, boolean redirectIO,
            Config config, boolean test) {
        this.model = model;
        this.outDir = outDir;
        this.workDir = workDir;
        this.inputDir = inputDir;
        this.redirectIO = redirectIO;
        this.config = config;
        this.test = test;
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

    public boolean test() {
        return test;
    }
}
