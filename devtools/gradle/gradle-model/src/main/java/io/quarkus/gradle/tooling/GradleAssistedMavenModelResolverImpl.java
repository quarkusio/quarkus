/*
 * This file is part of CycloneDX Gradle Plugin.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (c) OWASP Foundation. All Rights Reserved.
 */
package io.quarkus.gradle.tooling;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Parent;
import org.apache.maven.model.Repository;
import org.apache.maven.model.building.ModelSource2;
import org.apache.maven.model.resolution.ModelResolver;
import org.apache.maven.model.resolution.UnresolvableModelException;
import org.gradle.api.Project;
import org.gradle.api.artifacts.result.ResolvedArtifactResult;
import org.gradle.maven.MavenModule;
import org.gradle.maven.MavenPomArtifact;
import org.jspecify.annotations.Nullable;

/**
 * A Maven ModelResolver implementation that uses Gradle's ArtifactResolutionQuery
 * to resolve POM files. This avoids creating detached configurations and leverages
 * Gradle's artifact resolution infrastructure consistently.
 */
public class GradleAssistedMavenModelResolverImpl implements ModelResolver {
    private final Project project;
    // Cache resolved POMs to avoid redundant resolution queries
    private final Map<String, File> pomCache = new ConcurrentHashMap<>();

    public GradleAssistedMavenModelResolverImpl(Project project) {
        this.project = project;
    }

    /**
     * Pre-populate the cache with an already-resolved POM file.
     * Use this when the POM has already been resolved via ArtifactResolutionQuery
     * to avoid resolving it again when building the effective model.
     */
    public void cachePom(String groupId, String artifactId, String version, File pomFile) {
        pomCache.put(cacheKey(groupId, artifactId, version), pomFile);
    }

    private static String cacheKey(String groupId, String artifactId, String version) {
        return groupId + ":" + artifactId + ":" + version;
    }

    @Override
    public ModelSource2 resolveModel(String groupId, String artifactId, String version)
            throws UnresolvableModelException {
        String key = cacheKey(groupId, artifactId, version);

        // Check cache first
        File pomFile = pomCache.get(key);
        if (pomFile == null) {
            // Resolve using ArtifactResolutionQuery
            pomFile = resolvePomViaQuery(groupId, artifactId, version);
            if (pomFile != null) {
                pomCache.put(key, pomFile);
            }
        }

        if (pomFile == null) {
            throw new UnresolvableModelException(
                    "Could not resolve POM for " + groupId + ":" + artifactId + ":" + version,
                    groupId, artifactId, version);
        }

        final File resolvedPom = pomFile;
        return new ModelSource2() {
            @Override
            public InputStream getInputStream() throws IOException {
                return new FileInputStream(resolvedPom);
            }

            @Override
            public String getLocation() {
                return resolvedPom.getAbsolutePath();
            }

            @Override
            public @Nullable ModelSource2 getRelatedSource(String relPath) {
                return null;
            }

            @Override
            public URI getLocationURI() {
                return resolvedPom.toURI();
            }
        };
    }

    /**
     * Resolve a POM file using Gradle's ArtifactResolutionQuery.
     * This is the preferred method as it uses Gradle's resolution infrastructure.
     */
    private @Nullable File resolvePomViaQuery(String groupId, String artifactId, String version) {
        // Create a component identifier for the artifact
        @SuppressWarnings("unchecked")
        var componentId = project.getDependencies()
                .createArtifactResolutionQuery()
                .forModule(groupId, artifactId, version)
                .withArtifacts(MavenModule.class, MavenPomArtifact.class)
                .execute();

        for (var component : componentId.getResolvedComponents()) {
            for (var artifactResult : component.getArtifacts(MavenPomArtifact.class)) {
                if (artifactResult instanceof ResolvedArtifactResult resolved) {
                    return resolved.getFile();
                }
            }
        }
        return null;
    }

    @Override
    public ModelSource2 resolveModel(Parent parent) throws UnresolvableModelException {
        return resolveModel(parent.getGroupId(), parent.getArtifactId(), parent.getVersion());
    }

    @Override
    public ModelSource2 resolveModel(Dependency dependency) throws UnresolvableModelException {
        return resolveModel(dependency.getGroupId(), dependency.getArtifactId(), dependency.getVersion());
    }

    @Override
    public void addRepository(Repository repository) {
        // Gradle handles repositories at project level, ignore
    }

    @Override
    public void addRepository(Repository repository, boolean replace) {
        // Gradle handles repositories at project level, ignore
    }

    @Override
    public ModelResolver newCopy() {
        // Return same instance - cache is shared and thread-safe
        return this;
    }
}