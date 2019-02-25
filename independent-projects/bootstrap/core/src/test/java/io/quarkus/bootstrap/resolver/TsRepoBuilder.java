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

package io.quarkus.bootstrap.resolver;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import io.quarkus.bootstrap.model.AppArtifact;
import io.quarkus.bootstrap.resolver.AppModelResolverException;
import io.quarkus.bootstrap.resolver.maven.workspace.ModelUtils;

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

    private final Path workDir;
    private final BootstrapAppModelResolver resolver;

    private TsRepoBuilder(BootstrapAppModelResolver resolver, Path workDir) {
        this.resolver = resolver;
        this.workDir = workDir;
    }

    public void install(TsArtifact artifact) {
        install(artifact, null);
    }

    public void install(TsArtifact artifact, Path p) {
        final Path pomXml = workDir.resolve(artifact.getArtifactFileName() + ".pom");
        try {
            ModelUtils.persistModel(pomXml, artifact.getPomModel());
        } catch (Exception e) {
            error("Failed to persist pom.xml for " + artifact, e);
        }
        install(artifact.toPomArtifact().toAppArtifact(), pomXml);
        install(artifact.toAppArtifact(), p == null ? newTxt(artifact) : p);
    }

    protected void install(AppArtifact artifact, Path file) {
        try {
            resolver.install(artifact, file);
        } catch (AppModelResolverException e) {
            error("Failed to install " + artifact, e);
        }
    }

    protected Path newTxt(TsArtifact artifact) {
        final Path tmpFile = workDir.resolve(artifact.getArtifactFileName());
        if(Files.exists(tmpFile)) {
            throw new IllegalStateException("File already exists " + tmpFile);
        }
        try(BufferedWriter writer = Files.newBufferedWriter(tmpFile)) {
            writer.write(tmpFile.getFileName().toString());
        } catch (IOException e) {
            throw new IllegalStateException("Failed to create file " + tmpFile, e);
        }
        return tmpFile;
    }

}
