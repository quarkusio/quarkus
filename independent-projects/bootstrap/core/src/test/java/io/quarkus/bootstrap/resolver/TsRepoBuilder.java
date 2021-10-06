package io.quarkus.bootstrap.resolver;

import io.quarkus.bootstrap.resolver.maven.workspace.ModelUtils;
import io.quarkus.maven.dependency.ArtifactCoords;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

/**
 *
 * @author Alexey Loubyansky
 */
public class TsRepoBuilder {

    private static void error(String message, Throwable t) {
        throw new IllegalStateException(message, t);
    }

    public static TsRepoBuilder getInstance(BootstrapAppModelResolver resolver, Path workDir) {
        return new TsRepoBuilder(resolver, workDir);
    }

    protected final Path workDir;
    private final BootstrapAppModelResolver resolver;

    private TsRepoBuilder(BootstrapAppModelResolver resolver, Path workDir) {
        this.resolver = resolver;
        this.workDir = workDir;
    }

    public void install(TsArtifact artifact) {
        try {
            install(artifact, artifact.content == null ? null : artifact.content.getPath(workDir));
        } catch (IOException e) {
            throw new IllegalStateException("Failed to initialize content for " + artifact, e);
        }
    }

    public void install(TsArtifact artifact, Path p) {
        final Path pomXml = workDir.resolve(artifact.getArtifactFileName() + ".pom");
        if (Files.exists(pomXml)) {
            // assume it's already installed
            return;
        }
        try {
            ModelUtils.persistModel(pomXml, artifact.getPomModel());
        } catch (Exception e) {
            error("Failed to persist pom.xml for " + artifact, e);
        }
        install(artifact.toPomArtifact().toArtifact(), pomXml);
        if (p == null) {
            switch (artifact.type) {
                case TsArtifact.TYPE_JAR:
                    try {
                        p = newJar()
                                .addMavenMetadata(artifact, pomXml)
                                .getPath(workDir);
                    } catch (IOException e) {
                        throw new IllegalStateException("Failed to install " + artifact, e);
                    }
                    break;
                case TsArtifact.TYPE_TXT:
                    p = newTxt(artifact);
                    break;
                case TsArtifact.TYPE_POM:
                    break;
                default:
                    throw new IllegalStateException("Unsupported artifact type " + artifact.type);
            }
        }
        if (p != null) {
            install(artifact.toArtifact(), p);
        }
    }

    protected void install(ArtifactCoords artifact, Path file) {
        try {
            resolver.install(artifact, file);
        } catch (AppModelResolverException e) {
            error("Failed to install " + artifact, e);
        }
    }

    protected Path newTxt(TsArtifact artifact) {
        final Path tmpFile = workDir.resolve(artifact.getArtifactFileName());
        if (Files.exists(tmpFile)) {
            throw new IllegalStateException("File already exists " + tmpFile);
        }
        try (BufferedWriter writer = Files.newBufferedWriter(tmpFile)) {
            writer.write(tmpFile.getFileName().toString());
        } catch (IOException e) {
            throw new IllegalStateException("Failed to create file " + tmpFile, e);
        }
        return tmpFile;
    }

    public TsJar newJar() {
        return new TsJar(workDir.resolve(UUID.randomUUID().toString()));
    }
}
