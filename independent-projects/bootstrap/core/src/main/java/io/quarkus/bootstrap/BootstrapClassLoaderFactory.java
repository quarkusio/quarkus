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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import io.quarkus.bootstrap.model.AppDependency;
import io.quarkus.bootstrap.model.AppModel;
import io.quarkus.bootstrap.resolver.AppModelResolverException;
import io.quarkus.bootstrap.resolver.BootstrapAppModelResolver;
import io.quarkus.bootstrap.resolver.maven.MavenArtifactResolver;
import io.quarkus.bootstrap.resolver.maven.workspace.LocalProject;

/**
 *
 * @author Alexey Loubyansky
 */
public class BootstrapClassLoaderFactory {

    private static final String DOT_QUARKUS = ".quarkus";
    private static final String BOOTSTRAP = "bootstrap";
    private static final String DEPLOYMENT_CP = "cp.deployment";

    public static BootstrapClassLoaderFactory newInstance() {
        return new BootstrapClassLoaderFactory();
    }

    public static URLClassLoader newClassLoader(ClassLoader parent, List<AppDependency> deps, Path... extraPaths) {
        final URL[] urls = new URL[deps.size() + extraPaths.length];
        try {
            int i = 0;
            while (i < deps.size()) {
                urls[i] = deps.get(i).getArtifact().getPath().toUri().toURL();
                ++i;
            }
            for(Path p : extraPaths) {
                if(p == null) {
                    continue;
                }
                urls[i++] = p.toUri().toURL();
            }
        } catch (MalformedURLException e) {
            throw new IllegalStateException("Failed to create a URL", e);
        }
        return new URLClassLoader(urls, parent);
    }

    private static Path resolveCachedCpPath(LocalProject project) {
        return Paths.get(System.getProperty("user.home"))
                .resolve(DOT_QUARKUS)
                .resolve(BOOTSTRAP)
                .resolve(project.getGroupId())
                .resolve(project.getArtifactId())
                .resolve(project.getVersion())
                .resolve(DEPLOYMENT_CP);
    }

    private ClassLoader parent;
    private Path appClasses;
    private Path frameworkClasses;
    private boolean localProjectsDiscovery;
    private boolean offline = true;
    private boolean enableClassLoaderCache;

    private BootstrapClassLoaderFactory() {
    }

    public BootstrapClassLoaderFactory setParent(ClassLoader parent) {
        this.parent = parent;
        return this;
    }

    public BootstrapClassLoaderFactory setAppClasses(Path appClasses) {
        this.appClasses = appClasses;
        return this;
    }

    public BootstrapClassLoaderFactory setFrameworkClasses(Path frameworkClasses) {
        this.frameworkClasses = frameworkClasses;
        return this;
    }

    public BootstrapClassLoaderFactory setLocalProjectsDiscovery(boolean localProjectsDiscovery) {
        this.localProjectsDiscovery = localProjectsDiscovery;
        return this;
    }

    public BootstrapClassLoaderFactory setOffline(boolean offline) {
        this.offline = offline;
        return this;
    }

    public BootstrapClassLoaderFactory setClassLoaderCache(boolean enable) {
        this.enableClassLoaderCache = enable;
        return this;
    }

    public URLClassLoader newAllInclusiveClassLoader(boolean hierarchical) throws BootstrapException {
        if (appClasses == null) {
            throw new IllegalArgumentException("Application classes path has not been set");
        }
        try {
            final MavenArtifactResolver.Builder mvnBuilder = MavenArtifactResolver.builder().setOffline(offline);
            final LocalProject localProject;
            if (localProjectsDiscovery) {
                localProject = LocalProject.resolveLocalProjectWithWorkspace(LocalProject.locateCurrentProjectDir(appClasses));
                mvnBuilder.setWorkspace(localProject.getWorkspace());
            } else {
                localProject = LocalProject.resolveLocalProject(LocalProject.locateCurrentProjectDir(appClasses));
            }
            final AppModel appModel = new BootstrapAppModelResolver(mvnBuilder.build()).resolveModel(localProject.getAppArtifact());
            if (hierarchical) {
                final URLClassLoader cl = newClassLoader(parent, appModel.getUserDependencies(), appClasses, frameworkClasses);
                try {
                    return newClassLoader(cl, appModel.getDeploymentDependencies());
                } catch (Throwable e) {
                    try {
                        cl.close();
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
                    throw e;
                }
            }
            return newClassLoader(parent, appModel.getAllDependencies(), appClasses, frameworkClasses);
        } catch (AppModelResolverException e) {
            throw new BootstrapException("Failed to init application classloader", e);
        }
    }

    public URLClassLoader newDeploymentClassLoader() throws BootstrapException {
        if (appClasses == null) {
            throw new IllegalArgumentException("Application classes path has not been set");
        }
        final URLClassLoader ucl;
        Path cachedCpPath = null;
        long lastUpdated = 0;
        try {
            final LocalProject localProject;
            if (localProjectsDiscovery) {
                localProject = LocalProject.resolveLocalProjectWithWorkspace(LocalProject.locateCurrentProjectDir(appClasses));
                if(enableClassLoaderCache) {
                    lastUpdated = localProject.getWorkspace().getLastModified();
                    cachedCpPath = resolveCachedCpPath(localProject);
                    if (Files.exists(cachedCpPath)) {
                        try (BufferedReader reader = Files.newBufferedReader(cachedCpPath)) {
                            String line = reader.readLine();
                            if (Long.valueOf(line) == lastUpdated) {
                                line = reader.readLine();
                                final List<URL> urls = new ArrayList<>();
                                while (line != null) {
                                    urls.add(new URL(line));
                                    line = reader.readLine();
                                }
                                System.out.println("re-created from cache");
                                return new URLClassLoader(urls.toArray(new URL[urls.size()]), parent);
                            } else {
                                System.out.println("cache expired");
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            } else {
                localProject = LocalProject.resolveLocalProject(LocalProject.locateCurrentProjectDir(appClasses));
            }
            final MavenArtifactResolver.Builder mvn = MavenArtifactResolver.builder()
                    .setOffline(offline)
                    .setWorkspace(localProject.getWorkspace());

            ucl = newClassLoader(parent, new BootstrapAppModelResolver(mvn.build()).resolveModel(localProject.getAppArtifact()).getDeploymentDependencies());
        } catch (AppModelResolverException e) {
            throw new BootstrapException("Failed to init application classloader", e);
        }
        if(cachedCpPath != null) {
            try {
                Files.createDirectories(cachedCpPath.getParent());
                try(BufferedWriter writer = Files.newBufferedWriter(cachedCpPath)) {
                    writer.write(Long.toString(lastUpdated));
                    writer.newLine();
                    for(URL url : ucl.getURLs()) {
                        writer.write(url.toExternalForm());
                        writer.newLine();
                    }
                }
                System.out.println("cached");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return ucl;
    }
}
