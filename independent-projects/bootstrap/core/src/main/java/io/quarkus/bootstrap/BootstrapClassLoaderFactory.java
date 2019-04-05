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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.jboss.logging.Logger;

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

    private static final String QUARKUS = "quarkus";
    private static final String BOOTSTRAP = "bootstrap";
    private static final String DEPLOYMENT_CP = "deployment.cp";

    public static final String PROP_CP_CACHE = "quarkus-classpath-cache";
    public static final String PROP_WS_DISCOVERY = "quarkus-workspace-discovery";
    public static final String PROP_OFFLINE = "quarkus-bootstrap-offline";

    private static final int CP_CACHE_FORMAT_ID = 1;

    private static final Logger log = Logger.getLogger(BootstrapClassLoaderFactory.class);

    public static BootstrapClassLoaderFactory newInstance() {
        return new BootstrapClassLoaderFactory();
    }

    private static URL[] toURLs(List<AppDependency> deps, List<Path> extraPaths) {
        final URL[] urls = new URL[deps.size() + extraPaths.size()];
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
            return i != urls.length ? Arrays.copyOf(urls, i) : urls;
        } catch (MalformedURLException e) {
            throw new IllegalStateException("Failed to create a URL", e);
        }
    }

    private static Path resolveCachedCpPath(LocalProject project) {
        return project.getOutputDir().resolve(QUARKUS).resolve(BOOTSTRAP).resolve(DEPLOYMENT_CP);
    }

    private static void persistCp(LocalProject project, URL[] urls, Path p) {
        try {
            Files.createDirectories(p.getParent());
            try (BufferedWriter writer = Files.newBufferedWriter(p)) {
                writer.write(Integer.toString(CP_CACHE_FORMAT_ID));
                writer.newLine();
                writer.write(Integer.toString(project.getWorkspace().getId()));
                writer.newLine();
                for (URL url : urls) {
                    writer.write(url.toExternalForm());
                    writer.newLine();
                }
            }
            debug("Deployment classpath for %s was cached in %s", project.getAppArtifact(), p);
        } catch (IOException e) {
            log.warn("Failed to persist deployment classpath cache in " + p + " for " + project.getAppArtifact(), e);
        }
    }

    private ClassLoader parent;
    private Path appClasses;
    private List<Path> appCp = new ArrayList<>(1);
    private boolean localProjectsDiscovery;
    private Boolean offline;
    private boolean enableClasspathCache;

    private BootstrapClassLoaderFactory() {
    }

    public BootstrapClassLoaderFactory setParent(ClassLoader parent) {
        this.parent = parent;
        return this;
    }

    public BootstrapClassLoaderFactory setAppClasses(Path appClasses) {
        this.appClasses = appClasses;
        addToClassPath(appClasses);
        return this;
    }

    public BootstrapClassLoaderFactory addToClassPath(Path path) {
        this.appCp.add(path);
        return this;
    }

    public BootstrapClassLoaderFactory setLocalProjectsDiscovery(boolean localProjectsDiscovery) {
        this.localProjectsDiscovery = localProjectsDiscovery;
        return this;
    }

    public BootstrapClassLoaderFactory setOffline(Boolean offline) {
        this.offline = offline;
        return this;
    }

    public BootstrapClassLoaderFactory setEnableClasspathCache(boolean enable) {
        this.enableClasspathCache = enable;
        return this;
    }

    /**
     * WARNING: this method is creating a classloader by resolving all the dependencies on every call,
     * without consulting the cache.
     *
     * @param hierarchical  whether the deployment classloader should use the classloader built using
     * the user-defined application dependencies as its parent or all the dependencies should be loaded
     * by the same classloader
     * @return  classloader that is able to load both user-defined and deployment dependencies
     * @throws BootstrapException  in case of a failure
     */
    public URLClassLoader newAllInclusiveClassLoader(boolean hierarchical) throws BootstrapException {
        if (appClasses == null) {
            throw new IllegalArgumentException("Application classes path has not been set");
        }
        try {
            final MavenArtifactResolver.Builder mvnBuilder = MavenArtifactResolver.builder();
            if(offline != null) {
                mvnBuilder.setOffline(offline);
            }
            final LocalProject localProject;
            if (localProjectsDiscovery) {
                localProject = LocalProject.resolveLocalProjectWithWorkspace(LocalProject.locateCurrentProjectDir(appClasses));
                mvnBuilder.setWorkspace(localProject.getWorkspace());
            } else {
                localProject = LocalProject.resolveLocalProject(LocalProject.locateCurrentProjectDir(appClasses));
            }
            final AppModel appModel = new BootstrapAppModelResolver(mvnBuilder.build()).resolveModel(localProject.getAppArtifact());
            if (hierarchical) {
                final URLClassLoader cl = new URLClassLoader(toURLs(appModel.getUserDependencies(), appCp), parent);
                try {
                    return new URLClassLoader(toURLs(appModel.getDeploymentDependencies(), Collections.emptyList()), cl);
                } catch (Throwable e) {
                    try {
                        cl.close();
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
                    throw e;
                }
            }
            return new URLClassLoader(toURLs(appModel.getAllDependencies(), appCp), parent);
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
        final LocalProject localProject = localProjectsDiscovery || enableClasspathCache
                ? LocalProject.resolveLocalProjectWithWorkspace(LocalProject.locateCurrentProjectDir(appClasses))
                : LocalProject.resolveLocalProject(LocalProject.locateCurrentProjectDir(appClasses));
        try {
            if (enableClasspathCache) {
                cachedCpPath = resolveCachedCpPath(localProject);
                if (Files.exists(cachedCpPath)) {
                    try (BufferedReader reader = Files.newBufferedReader(cachedCpPath)) {
                        if (matchesInt(reader.readLine(), CP_CACHE_FORMAT_ID)) {
                            if (matchesInt(reader.readLine(), localProject.getWorkspace().getId())) {
                                final List<URL> urls = new ArrayList<>();
                                String line = reader.readLine();
                                while (line != null) {
                                    urls.add(new URL(line));
                                    line = reader.readLine();
                                }
                                debug("Deployment classloader for %s was re-created from the classpath cache",
                                        localProject.getAppArtifact());
                                return new URLClassLoader(urls.toArray(new URL[urls.size()]), parent);
                            } else {
                                debug("Cached deployment classpath has expired for %s", localProject.getAppArtifact());
                            }
                        } else {
                            debug("Unsupported classpath cache format in %s for %s", cachedCpPath,
                                    localProject.getAppArtifact());
                        }
                    } catch (IOException e) {
                        log.warn("Failed to read deployment classpath cache from " + cachedCpPath + " for " + localProject.getAppArtifact(), e);
                    }
                }
            }
            final MavenArtifactResolver.Builder mvn = MavenArtifactResolver.builder()
                    .setWorkspace(localProject.getWorkspace());
            if (offline != null) {
                mvn.setOffline(offline);
            }
            final URL[] urls = toURLs(new BootstrapAppModelResolver(mvn.build()).resolveModel(localProject.getAppArtifact()).getDeploymentDependencies(), Collections.emptyList());
            if(cachedCpPath != null) {
                persistCp(localProject, urls, cachedCpPath);
            }
            ucl = new URLClassLoader(urls, parent);
        } catch (AppModelResolverException e) {
            throw new BootstrapException("Failed to create a deployment classloader for " + localProject.getAppArtifact(), e);
        }
        return ucl;
    }

    private static boolean matchesInt(String line, int value) {
        if(line == null) {
            return false;
        }
        try {
            return Integer.parseInt(line) == value;
        } catch(NumberFormatException e) {
            // does not match
        }
        return false;
    }

    private static void debug(String msg, Object... args) {
        if(log.isDebugEnabled()) {
            log.debug(String.format(msg, args));
        }
    }
}
