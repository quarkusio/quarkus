/*
 * Copyright 2019 Red Hat, Inc.
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

package org.jboss.shamrock.bootstrap;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.List;
import org.jboss.shamrock.bootstrap.resolver.AppArtifact;
import org.jboss.shamrock.bootstrap.resolver.AppArtifactResolverException;
import org.jboss.shamrock.bootstrap.resolver.AppDependency;
import org.jboss.shamrock.bootstrap.resolver.aether.AetherArtifactResolver;
import org.jboss.shamrock.bootstrap.resolver.workspace.LocalProject;

/**
 *
 * @author Alexey Loubyansky
 */
public class BootstrapClassLoaderBuilder {

    public static BootstrapClassLoaderBuilder newInstance() {
        return new BootstrapClassLoaderBuilder();
    }

    private ClassLoader parent;
    private Path appClasses;
    private Path frameworkClasses;
    private boolean localProjectsDiscovery;
    private boolean offline = true;

    private BootstrapClassLoaderBuilder() {
    }

    public BootstrapClassLoaderBuilder setParent(ClassLoader parent) {
        this.parent = parent;
        return this;
    }

    public BootstrapClassLoaderBuilder setAppClasses(Path appClasses) {
        this.appClasses = appClasses;
        return this;
    }

    public BootstrapClassLoaderBuilder setFrameworkClasses(Path frameworkClasses) {
        this.frameworkClasses = frameworkClasses;
        return this;
    }

    public BootstrapClassLoaderBuilder setLocalProjectsDiscovery(boolean localProjectsDiscovery) {
        this.localProjectsDiscovery = localProjectsDiscovery;
        return this;
    }

    public BootstrapClassLoaderBuilder setOffline(boolean offline) {
        this.offline = offline;
        return this;
    }

    public URLClassLoader build() throws BootstrapException {
        if (appClasses == null) {
            throw new IllegalArgumentException("Application classes path has not been set");
        }
        if (frameworkClasses == null) {
            throw new IllegalArgumentException("Framework classes path has not been set");
        }

        try {
            final AetherArtifactResolver.Builder resolverBuilder = AetherArtifactResolver.builder().setOffline(offline);
            final LocalProject localProject;
            if (localProjectsDiscovery) {
                localProject = LocalProject.resolveLocalProjectWithWorkspace(LocalProject.locateCurrentProjectDir(appClasses));
                resolverBuilder.setWorkspace(localProject.getWorkspace());
            } else {
                localProject = LocalProject.resolveLocalProject(LocalProject.locateCurrentProjectDir(appClasses));
            }

            final AppArtifact appArtifact = new AppArtifact(localProject.getGroupId(), localProject.getArtifactId(), "",
                    localProject.getRawModel().getPackaging(), localProject.getVersion());
            final AetherArtifactResolver resolver = resolverBuilder.build();
            final List<AppDependency> deps = resolver.collectRuntimeDependencies(appArtifact).getBuildClasspath();
            final URL[] urls = new URL[deps.size() + 2];
            try {
                int i = 0;
                while (i < deps.size()) {
                    final AppDependency appDep = deps.get(i);
                    final Path path = resolver.resolve(appDep.getArtifact());
                    urls[i++] = path.toUri().toURL();
                }
                urls[i++] = appClasses.toUri().toURL();
                urls[i++] = frameworkClasses.toUri().toURL();
            } catch (MalformedURLException e) {
                throw new IllegalStateException("Failed to create a URL", e);
            }
            return new URLClassLoader(urls, parent);
        } catch (AppArtifactResolverException e) {
            throw new BootstrapException("Failed to init application classloader", e);
        }
    }
}
