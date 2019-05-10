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

package io.quarkus.bootstrap.resolver.maven;

import java.nio.file.Path;
import java.util.List;

import io.quarkus.bootstrap.BootstrapDependencyProcessingException;
import io.quarkus.bootstrap.BootstrapException;
import io.quarkus.bootstrap.model.AppDependency;
import io.quarkus.bootstrap.resolver.AppModelResolverException;
import io.quarkus.bootstrap.resolver.BootstrapAppModelResolver;
import io.quarkus.bootstrap.resolver.BuilderInfo;
import io.quarkus.bootstrap.resolver.LocalProject;
import io.quarkus.bootstrap.resolver.maven.workspace.LocalMavenProject;

/**
 * Provides build information from Maven.
 */
public class MavenBuilderInfo implements BuilderInfo {
    private final Path projectDir;
    private boolean caching;
    private boolean localProjectsDiscovery;
    private LocalMavenProject localProject;

    public MavenBuilderInfo(Path projectDir) {
        this.projectDir = projectDir;
    }

    @Override
    public LocalProject getLocalProject() throws BootstrapException {
        if (localProject == null) {
            localProject = caching || localProjectsDiscovery
                    ? LocalMavenProject.loadWorkspace(projectDir)
                    : LocalMavenProject.load(projectDir);
        }
        return localProject;
    }

    @Override
    public BuilderInfo withClasspathCaching(boolean classpathCaching) {
        this.caching = classpathCaching;
        return this;
    }

    @Override
    public BuilderInfo withLocalProjectsDiscovery(boolean localProjectsDiscovery) {
        this.localProjectsDiscovery = localProjectsDiscovery;
        return this;
    }

    @Override
    public void close() throws BootstrapException {
    }

    @Override
    public List<AppDependency> getDeploymentDependencies(boolean offline)
            throws BootstrapDependencyProcessingException, AppModelResolverException {
        final MavenArtifactResolver.Builder mvn = MavenArtifactResolver.builder()
                .setWorkspace(localProject.getWorkspace());
        mvn.setOffline(offline);
        return new BootstrapAppModelResolver(mvn.build()).resolveModel(localProject.getAppArtifact())
                .getDeploymentDependencies();
    }
}
