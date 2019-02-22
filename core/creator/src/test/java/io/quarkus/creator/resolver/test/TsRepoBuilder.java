/*
 * Copyright 2019 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.quarkus.creator.resolver.test;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import io.quarkus.creator.AppArtifact;
import io.quarkus.creator.AppCreatorException;
import io.quarkus.creator.phase.curate.Utils;
import io.quarkus.creator.resolver.aether.AetherArtifactResolver;

/**
 *
 * @author Alexey Loubyansky
 */
public class TsRepoBuilder {

    private static void error(String message, Throwable t) {
        throw new IllegalStateException(message, t);
    }

    public static TsRepoBuilder getInstance(AetherArtifactResolver resolver, Path workDir) {
        return new TsRepoBuilder(resolver, workDir);
    }

    private final Path workDir;
    private final AetherArtifactResolver resolver;

    private TsRepoBuilder(AetherArtifactResolver resolver, Path workDir) {
        this.resolver = resolver;
        this.workDir = workDir;
    }

    public void install(TsArtifact artifact) {
        final Path pomXml = workDir.resolve(artifact.getArtifactFileName() + ".pom");
        try {
            Utils.persistModel(pomXml, artifact.getPomModel());
        } catch (AppCreatorException e) {
            error("Failed to persist pom.xml for " + artifact, e);
        }
        install(artifact.toPomArtifact().toAppArtifact(), pomXml);
        install(artifact.toAppArtifact(), newTxt(artifact));
    }

    protected void install(AppArtifact artifact, Path file) {
        try {
            resolver.install(artifact, file);
        } catch (AppCreatorException e) {
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

}
