package io.quarkus.deployment.codegen;

import java.nio.file.Path;

import io.quarkus.deployment.CodeGenProvider;

public class CodeGenData {
    public final CodeGenProvider provider;
    public final Path outPath;
    public final Path sourceDir;
    public final Path buildDir;
    public boolean redirectIO;

    public CodeGenData(CodeGenProvider provider, Path outPath, Path sourceDir, Path buildDir) {
        this(provider, outPath, sourceDir, buildDir, true);
    }

    public CodeGenData(CodeGenProvider provider, Path outPath, Path sourceDir, Path buildDir, boolean redirectIO) {
        this.provider = provider;
        this.outPath = outPath;
        this.sourceDir = sourceDir;
        this.buildDir = buildDir.normalize();
        this.redirectIO = redirectIO;
    }

    public void setRedirectIO(boolean redirectIO) {
        this.redirectIO = redirectIO;
    }
}
