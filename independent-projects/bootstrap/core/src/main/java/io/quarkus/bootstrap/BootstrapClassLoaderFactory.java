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
import java.util.Collections;
import java.util.List;

import org.jboss.logging.Logger;

import io.quarkus.bootstrap.model.AppArtifact;
import io.quarkus.bootstrap.model.AppDependency;
import io.quarkus.bootstrap.model.AppModel;
import io.quarkus.bootstrap.resolver.AppModelResolverException;
import io.quarkus.bootstrap.resolver.BootstrapAppModelResolver;
import io.quarkus.bootstrap.resolver.maven.MavenArtifactResolver;
import io.quarkus.bootstrap.resolver.maven.workspace.LocalProject;
import io.quarkus.bootstrap.resolver.maven.workspace.ModelUtils;

/**
 *
 * @author Alexey Loubyansky
 */
public class BootstrapClassLoaderFactory {

    private static final String QUARKUS = "quarkus";
    private static final String BOOTSTRAP = "bootstrap";
    private static final String DEPLOYMENT_CP = "deployment.cp";

    public static final String PROP_CP_CACHE = "quarkus-classpath-cache";
    public static final String PROP_DEPLOYMENT_CP = "quarkus-deployment-cp";
    public static final String PROP_OFFLINE = "quarkus-bootstrap-offline";
    public static final String PROP_WS_DISCOVERY = "quarkus-workspace-discovery";

    private static final int CP_CACHE_FORMAT_ID = 1;

    private static final Logger log = Logger.getLogger(BootstrapClassLoaderFactory.class);

    public static BootstrapClassLoaderFactory newInstance() {
        return new BootstrapClassLoaderFactory();
    }

    private static URL[] toURLs(List<AppDependency> deps) throws BootstrapException {
        final URL[] urls = new URL[deps.size()];
        addDeps(urls, 0, deps);
        return urls;
    }

    private static URL toURL(Path p) throws BootstrapException {
        try {
            return p.toUri().toURL();
        } catch (MalformedURLException e) {
            throw new BootstrapException("Failed to create a URL for " + p, e);
        }
    }

    private static int addDeps(URL[] urls, int offset, List<AppDependency> deps) throws BootstrapException {
        assertCapacity(urls, offset, deps.size());
        int i = 0;
        while(i < deps.size()) {
            urls[offset + i] = toURL(deps.get(i++).getArtifact().getPath());
        }
        return i + offset;
    }

    private static int addPaths(URL[] urls, int offset, List<Path> deps) throws BootstrapException {
        assertCapacity(urls, offset, deps.size());
        int i = 0;
        while(i < deps.size()) {
            urls[offset + i] = toURL(deps.get(i++));
        }
        return i + offset;
    }

    private static void assertCapacity(URL[] urls, int offset, int deps) throws BootstrapException {
        if(urls.length < offset + deps) {
            throw new BootstrapException("Failed to add dependency URLs: the target array of length " + urls.length
                    + " is not big enough to add " + deps + " dependencies with offset " + offset);
        }
    }

    private static Path resolveCachedCpPath(LocalProject project) {
        return project.getOutputDir().resolve(QUARKUS).resolve(BOOTSTRAP).resolve(DEPLOYMENT_CP);
    }

    private static void persistCp(LocalProject project, URL[] urls, int limit, Path p) {
        try {
            Files.createDirectories(p.getParent());
            try (BufferedWriter writer = Files.newBufferedWriter(p)) {
                writer.write(Integer.toString(CP_CACHE_FORMAT_ID));
                writer.newLine();
                writer.write(Integer.toString(project.getWorkspace().getId()));
                writer.newLine();
                for (int i = 0; i < limit; ++i) {
                    writer.write(urls[i].toExternalForm());
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
    private List<Path> appCp = new ArrayList<>(0);
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
            final AppArtifact appArtifact;
            if (Files.isDirectory(appClasses)) {
                if (localProjectsDiscovery) {
                    localProject = LocalProject.loadWorkspace(appClasses);
                    mvnBuilder.setWorkspace(localProject.getWorkspace());
                } else {
                    localProject = LocalProject.load(appClasses);
                }
                appArtifact = localProject.getAppArtifact();
            } else {
                localProject = localProjectsDiscovery ? LocalProject.loadWorkspace(Paths.get("").normalize().toAbsolutePath(), false) : null;
                if(localProject != null) {
                    mvnBuilder.setWorkspace(localProject.getWorkspace());
                }
                appArtifact = ModelUtils.resolveAppArtifact(appClasses);
            }
            final BootstrapAppModelResolver appModelResolver = new BootstrapAppModelResolver(mvnBuilder.build());
            final AppModel appModel = appModelResolver.resolveManagedModel(appArtifact, Collections.emptyList(),
                    localProject == null ? null : localProject.getAppArtifact());
            if (hierarchical) {
                final URLClassLoader cl = initAppCp(appModel.getUserDependencies());
                try {
                    return new URLClassLoader(toURLs(appModel.getDeploymentDependencies()), cl);
                } catch (Throwable e) {
                    try {
                        cl.close();
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
                    throw e;
                }
            }
            return initAppCp(appModel.getAllDependencies());
        } catch (AppModelResolverException | IOException e) {
            throw new BootstrapException("Failed to init application classloader", e);
        }
    }

    private URLClassLoader initAppCp(final List<AppDependency> deps) throws BootstrapException {
        final URL[] urls = new URL[deps.size() + appCp.size() + 1];
        urls[0] = toURL(appClasses);
        int offset = addDeps(urls, 1, deps);
        if(!appCp.isEmpty()) {
            addPaths(urls, offset, appCp);
        }
        return new URLClassLoader(urls, parent);
    }

    public URLClassLoader newDeploymentClassLoader() throws BootstrapException {
        if (appClasses == null) {
            throw new IllegalArgumentException("Application classes path has not been set");
        }

        if(!Files.isDirectory(appClasses)) {
            final MavenArtifactResolver.Builder mvnBuilder = MavenArtifactResolver.builder();
            if (offline != null) {
                mvnBuilder.setOffline(offline);
            }
            final LocalProject localProject = localProjectsDiscovery ? LocalProject.loadWorkspace(Paths.get("").normalize().toAbsolutePath(), false) : null;
            if(localProject != null) {
                mvnBuilder.setWorkspace(localProject.getWorkspace());
            }
            final MavenArtifactResolver mvn;
            try {
                mvn = mvnBuilder.build();
            } catch (AppModelResolverException e) {
                throw new BootstrapException("Failed to initialize bootstrap Maven artifact resolver", e);
            }

            final List<AppDependency> deploymentDeps;
            try {
                final BootstrapAppModelResolver appModelResolver = new BootstrapAppModelResolver(mvn);
                final AppArtifact appArtifact = ModelUtils.resolveAppArtifact(appClasses);
                deploymentDeps = appModelResolver
                        .resolveManagedModel(appArtifact, Collections.emptyList(),
                                localProject == null ? null : localProject.getAppArtifact())
                        .getDeploymentDependencies();
            } catch (Exception e) {
                throw new BootstrapException("Failed to resolve deployment dependencies for " + appClasses, e);
            }

            final URL[] urls;
            if(appCp.isEmpty()) {
                urls = toURLs(deploymentDeps);
            } else {
                urls = new URL[deploymentDeps.size() + appCp.size()];
                addDeps(urls,
                        addPaths(urls, 0, appCp),
                        deploymentDeps);
            }
            return new URLClassLoader(urls, parent);
        }

        final URLClassLoader ucl;
        Path cachedCpPath = null;
        final LocalProject localProject = localProjectsDiscovery || enableClasspathCache
                ? LocalProject.loadWorkspace(appClasses)
                : LocalProject.load(appClasses);
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
                                final URL[] arr;
                                if(appCp.isEmpty()) {
                                    arr = urls.toArray(new URL[urls.size()]);
                                } else {
                                    arr = new URL[urls.size() + appCp.size()];
                                    int i = 0;
                                    while(i < urls.size()) {
                                        arr[i] = urls.get(i++);
                                    }
                                    addPaths(arr, i, appCp);
                                }
                                return new URLClassLoader(arr, parent);
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
            final List<AppDependency> deploymentDeps = new BootstrapAppModelResolver(mvn.build()).resolveModel(localProject.getAppArtifact()).getDeploymentDependencies();
            final URL[] urls;
            if(appCp.isEmpty()) {
                urls = toURLs(deploymentDeps);
            } else {
                urls = new URL[deploymentDeps.size() + appCp.size()];
                addDeps(urls,
                        addPaths(urls, 0, appCp),
                        deploymentDeps);
            }
            if(cachedCpPath != null) {
                persistCp(localProject, urls, deploymentDeps.size(), cachedCpPath);
            }
            ucl = new URLClassLoader(urls, parent);
        } catch (AppModelResolverException e) {
            throw new BootstrapException("Failed to create the deployment classloader for " + localProject.getAppArtifact(), e);
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
