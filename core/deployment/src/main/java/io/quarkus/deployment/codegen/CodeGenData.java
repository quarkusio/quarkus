package io.quarkus.deployment.codegen;

import java.nio.file.Path;

import io.quarkus.deployment.CodeGenProvider;

public class CodeGenData {
    public final CodeGenProvider provider;
    public final Path outPath;
    public final Path sourceDir;
    public final Path buildDir;

    public CodeGenData(CodeGenProvider provider, Path outPath, Path sourceDir, Path buildDir) {
        this.provider = provider;
        this.outPath = outPath;
        this.sourceDir = sourceDir;
        this.buildDir = buildDir;
    }
}
