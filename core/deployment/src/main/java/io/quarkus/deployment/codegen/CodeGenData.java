package io.quarkus.deployment.codegen;

import java.nio.file.Path;

import io.quarkus.deployment.CodeGenProvider;

/**
 * Links a {@link CodeGenProvider} instance, an input and output directories for the provider.
 */
public class CodeGenData {
    public final CodeGenProvider provider;
    public final Path projectDir;
    public final Path outPath;
    public final Path sourceDir;
    public final Path buildDir;
    public boolean redirectIO;

    /**
     * @param provider code gen provider
     * @param projectDir the project directory
     * @param outPath where the generated output should be stored
     * @param sourceDir where the input sources are
     * @param buildDir base project output directory
     */
    public CodeGenData(CodeGenProvider provider, Path projectDir, Path outPath, Path sourceDir, Path buildDir) {
        this(provider, projectDir, outPath, sourceDir, buildDir, true);
    }

    /**
     * @param provider code gen provider
     * @param projectDir the project directory
     * @param outPath where the generated output should be stored
     * @param sourceDir where the input sources are
     * @param buildDir base project output directory
     * @param redirectIO whether to redirect IO, in case a provider is logging something
     */
    public CodeGenData(CodeGenProvider provider, Path projectDir, Path outPath, Path sourceDir, Path buildDir,
            boolean redirectIO) {
        this.provider = provider;
        this.projectDir = projectDir;
        this.outPath = outPath;
        this.sourceDir = sourceDir;
        this.buildDir = buildDir.normalize();
        this.redirectIO = redirectIO;
    }

    public void setRedirectIO(boolean redirectIO) {
        this.redirectIO = redirectIO;
    }
}
