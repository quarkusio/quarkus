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

package io.quarkus.bootstrap;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import io.quarkus.bootstrap.resolver.AppArtifact;
import io.quarkus.bootstrap.resolver.AppArtifactResolverException;
import io.quarkus.bootstrap.resolver.AppDependency;
import io.quarkus.bootstrap.resolver.aether.BootstrapAppDependencies;
import io.quarkus.bootstrap.resolver.aether.BootstrapArtifactResolver;
import io.quarkus.bootstrap.resolver.workspace.LocalProject;

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
        /*
        if (frameworkClasses == null) {
            throw new IllegalArgumentException("Framework classes path has not been set");
        }
        */

        try {
            final BootstrapArtifactResolver.Builder resolverBuilder = BootstrapArtifactResolver.builder().setOffline(offline);
            final LocalProject localProject;
            if (localProjectsDiscovery) {
                localProject = LocalProject.resolveLocalProjectWithWorkspace(LocalProject.locateCurrentProjectDir(appClasses));
                resolverBuilder.setWorkspace(localProject.getWorkspace());
            } else {
                localProject = LocalProject.resolveLocalProject(LocalProject.locateCurrentProjectDir(appClasses));
            }

            final AppArtifact appArtifact = new AppArtifact(localProject.getGroupId(), localProject.getArtifactId(), "",
                    localProject.getRawModel().getPackaging(), localProject.getVersion());
            final BootstrapArtifactResolver resolver = resolverBuilder.build();
            BootstrapAppDependencies appDeps = ((BootstrapAppDependencies)resolver.collectRuntimeDependencies(appArtifact));
            List<AppDependency> deps = appDeps.getAppClasspath();
            List<URL> urlList = new ArrayList<>();
            try {
                int i = 0;
                while (i < deps.size()) {
                    urlList.add(resolver.resolve(deps.get(i).getArtifact()).toUri().toURL());
                    i++;
                }
                urlList.add(appClasses.toUri().toURL());
                if(frameworkClasses != null) {
                    urlList.add(frameworkClasses.toUri().toURL());
                }
            } catch (MalformedURLException e) {
                throw new IllegalStateException("Failed to create a URL", e);
            }

            final URLClassLoader cl = new URLClassLoader(urlList.toArray(new URL[urlList.size()]), parent);
            urlList.clear();
            deps = appDeps.getDeploymentClasspath();
            try {
                int i = 0;
                while (i < deps.size()) {
                    final AppDependency appDep = deps.get(i++);
                    final Path path = resolver.resolve(appDep.getArtifact());
                    urlList.add(path.toUri().toURL());
                }
            } catch (MalformedURLException e) {
                try {
                    cl.close();
                } catch (IOException e1) {
                    // TODO Auto-generated catch block
                    e1.printStackTrace();
                }
                throw new IllegalStateException("Failed to create a URL", e);
            }
            return new URLClassLoader(urlList.toArray(new URL[urlList.size()]), cl);
        } catch (AppArtifactResolverException e) {
            throw new BootstrapException("Failed to init application classloader", e);
        }
    }

    public URLClassLoader buildDeploymentCl() throws BootstrapException {
        if (appClasses == null) {
            throw new IllegalArgumentException("Application classes path has not been set");
        }

        try {
            final BootstrapArtifactResolver.Builder resolverBuilder = BootstrapArtifactResolver.builder().setOffline(offline);
            final LocalProject localProject;
            if (localProjectsDiscovery) {
                localProject = LocalProject.resolveLocalProjectWithWorkspace(LocalProject.locateCurrentProjectDir(appClasses));
                resolverBuilder.setWorkspace(localProject.getWorkspace());
            } else {
                localProject = LocalProject.resolveLocalProject(LocalProject.locateCurrentProjectDir(appClasses));
            }

            final AppArtifact appArtifact = new AppArtifact(localProject.getGroupId(), localProject.getArtifactId(), "",
                    localProject.getRawModel().getPackaging(), localProject.getVersion());
            final BootstrapArtifactResolver resolver = resolverBuilder.build();
            BootstrapAppDependencies appDeps = ((BootstrapAppDependencies)resolver.collectRuntimeDependencies(appArtifact));
            List<URL> urlList = new ArrayList<>();
            List<AppDependency> deps = appDeps.getDeploymentClasspath();
            try {
                int i = 0;
                while (i < deps.size()) {
                    final AppDependency appDep = deps.get(i++);
                    final Path path = resolver.resolve(appDep.getArtifact());
                    urlList.add(path.toUri().toURL());
                }
            } catch (MalformedURLException e) {
                throw new IllegalStateException("Failed to create a URL", e);
            }
            return new URLClassLoader(urlList.toArray(new URL[urlList.size()]), parent);
        } catch (AppArtifactResolverException e) {
            throw new BootstrapException("Failed to init application classloader", e);
        }
    }
}
