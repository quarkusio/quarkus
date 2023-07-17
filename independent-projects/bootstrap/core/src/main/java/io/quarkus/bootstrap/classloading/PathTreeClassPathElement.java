package io.quarkus.bootstrap.classloading;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.channels.ClosedChannelException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.security.cert.Certificate;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Function;
import java.util.jar.Manifest;

import org.jboss.logging.Logger;

import io.quarkus.maven.dependency.ArtifactKey;
import io.quarkus.paths.OpenPathTree;
import io.quarkus.paths.PathTree;
import io.quarkus.paths.PathVisit;
import io.quarkus.paths.PathVisitor;

public class PathTreeClassPathElement extends AbstractClassPathElement {

    private static final Logger log = Logger.getLogger(PathTreeClassPathElement.class);

    private final ReadWriteLock lock;
    private final OpenPathTree pathTree;
    private final boolean runtime;
    private final ArtifactKey dependencyKey;
    private volatile Set<String> resources;

    public PathTreeClassPathElement(PathTree pathTree, boolean runtime) {
        this(pathTree, runtime, null);
    }

    public PathTreeClassPathElement(PathTree pathTree, boolean runtime, ArtifactKey dependencyKey) {
        this.pathTree = Objects.requireNonNull(pathTree, "Path tree is null").open();
        this.lock = new ReentrantReadWriteLock();
        this.runtime = runtime;
        this.dependencyKey = dependencyKey;
    }

    @Override
    public boolean isRuntime() {
        return runtime;
    }

    @Override
    public ArtifactKey getDependencyKey() {
        return dependencyKey;
    }

    @Override
    public Path getRoot() {
        return pathTree.getOriginalTree().getRoots().iterator().next();
    }

    /**
     * In case the root path is configured, Vert.x may be looking for resources like 'META-INF/resources//index.html'.
     * This method will make sure there are no duplicate separators.
     *
     * @param path path to santize
     * @return sanitized path
     */
    private static String sanitize(String path) {
        var i = path.indexOf("//");
        if (i < 0) {
            return path;
        }
        final StringBuilder sb = new StringBuilder(path.length());
        sb.append(path, 0, ++i);
        while (i < path.length()) {
            final char c = path.charAt(i);
            if (c == '/') {
                ++i;
                continue;
            }
            var j = path.indexOf("//", i + 1);
            if (j < 0) {
                sb.append(path, i, path.length());
                break;
            }
            sb.append(path, i, ++j);
            i = j;
        }
        return sb.toString();
    }

    @Override
    public ClassPathResource getResource(String name) {
        final String sanitized = sanitize(name);
        final Set<String> resources = this.resources;
        if (resources != null && !resources.contains(sanitized)) {
            return null;
        }
        return apply(tree -> tree.apply(sanitized, visit -> visit == null ? null : new Resource(visit)));
    }

    @Override
    public <T> T apply(Function<OpenPathTree, T> func) {
        lock.readLock().lock();
        try {
            if (pathTree.isOpen()) {
                return func.apply(pathTree);
            }
            //we still need this to work if it is closed, so shutdown hooks work
            //once it is closed it simply does not hold on to any resources
            final boolean interrupted = Thread.interrupted();
            try (OpenPathTree openTree = pathTree.getOriginalTree().open()) {
                return func.apply(openTree);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            } finally {
                if (interrupted) {
                    Thread.currentThread().interrupt();
                }
            }
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Set<String> getProvidedResources() {
        Set<String> resources = this.resources;
        if (resources == null) {
            resources = apply(tree -> {
                final Set<String> relativePaths = new HashSet<>();
                tree.walk(new PathVisitor() {
                    @Override
                    public void visitPath(PathVisit visit) {
                        final String relativePath = visit.getRelativePath("/");
                        if (relativePath.isEmpty()) {
                            return;
                        }
                        relativePaths.add(relativePath);
                    }
                });
                return relativePaths;
            });
            this.resources = resources;
        }
        return resources;
    }

    @Override
    protected Manifest readManifest() {
        return apply(OpenPathTree::getManifest);
    }

    @Override
    public ProtectionDomain getProtectionDomain(ClassLoader classLoader) {
        URL url = null;
        final Path root = getRoot();
        try {
            url = root.toUri().toURL();
        } catch (MalformedURLException e) {
            throw new RuntimeException("Unable to create protection domain for " + root, e);
        }
        CodeSource codesource = new CodeSource(url, (Certificate[]) null);
        ProtectionDomain protectionDomain = new ProtectionDomain(codesource, null, classLoader, null);
        return protectionDomain;
    }

    @Override
    public void close() throws IOException {
        lock.writeLock().lock();
        resources = null;
        try {
            pathTree.close();
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append(getClass().getName()).append("[");
        if (getDependencyKey() != null) {
            sb.append(getDependencyKey().toGacString()).append(" ");
        }
        final Iterator<Path> i = pathTree.getRoots().iterator();
        if (i.hasNext()) {
            sb.append(i.next());
            while (i.hasNext()) {
                sb.append(",").append(i.next());
            }
        }
        sb.append(" runtime=").append(isRuntime());
        final Set<String> resources = this.resources;
        sb.append(" resources=").append(resources == null ? "null" : resources.size());
        return sb.append(']').toString();
    }

    private class Resource implements ClassPathResource {

        private final String name;
        private final Path path;
        private volatile URL url;

        private Resource(PathVisit visit) {
            name = visit.getRelativePath("/");
            path = visit.getPath();
        }

        @Override
        public ClassPathElement getContainingElement() {
            return PathTreeClassPathElement.this;
        }

        @Override
        public String getPath() {
            return name;
        }

        @Override
        public URL getUrl() {
            if (url != null) {
                return url;
            }
            lock.readLock().lock();
            try {
                if (pathTree.isOpen()) {
                    URI uri = path.toUri();
                    URL url;
                    // the URLClassLoader doesn't add trailing slashes to directories, so we make sure we return
                    // the same URL as it would to avoid having QuarkusClassLoader return different URLs
                    // (one with a trailing slash and one without) for same resource
                    if (uri.getPath() != null && uri.getPath().endsWith("/")) {
                        String uriStr = uri.toString();
                        url = new URL(uriStr.substring(0, uriStr.length() - 1));
                    } else {
                        url = uri.toURL();
                    }
                    if (url.getQuery() != null) {
                        //huge hack to work around a JDK bug. ZipPath does not escape properly, so if there is a ?
                        //in the path then it ends up in the query string and everything is screwy.
                        //if there are multiple question marks the extra ones end up in the path and not the query string as you would expect
                        url = new URL(url.getProtocol(), url.getHost(), url.getPort(),
                                url.getPath().replaceAll("\\?", "%3F") + "%3F" + url.getQuery());
                    }
                    return this.url = url;
                }
                return url = apply(tree -> tree.apply(name, visit -> visit == null ? null : visit.getUrl()));
            } catch (MalformedURLException e) {
                throw new RuntimeException("Failed to translate " + path + " to URL", e);
            } finally {
                lock.readLock().unlock();
            }
        }

        @Override
        public byte[] getData() {
            lock.readLock().lock();
            try {
                if (pathTree.isOpen()) {
                    try {
                        try {
                            return Files.readAllBytes(path);
                        } catch (InterruptedIOException e) {
                            // if we are interrupted reading data we finish the op, then just re-interrupt
                            // the thread state
                            byte[] bytes = Files.readAllBytes(path);
                            Thread.currentThread().interrupt();
                            return bytes;
                        } catch (ClosedChannelException e) {
                            // This could happen in tests or dev mode when the application is being terminated
                            // while some threads are still trying to load classes.
                            // Reset the interrupted status and try completing the operation by reading from a newly open archive
                            lock.readLock().unlock();
                            try {
                                close();
                            } finally {
                                lock.readLock().lock();
                            }
                        }
                    } catch (IOException e) {
                        if (pathTree.isOpen()) {
                            //this is a weird corner case, that should not really affect end users, but is super annoying
                            //if you are actually working on Quarkus. If you rebuild quarkus while you have an application
                            //running reading from the rebuilt zip file will fail, but some of these classes are needed
                            //for a clean shutdown, so the Java process hangs and needs to be forcibly killed
                            //this effectively attempts to reopen the file, allowing shutdown to work.
                            //we need to do this here as close needs a write lock while withJarFile takes a readLock
                            log.error("Failed to read " + name
                                    + " attempting to re-open the zip file. It is likely a jar file changed on disk, you should shutdown your application",
                                    e);
                            lock.readLock().unlock();
                            try {
                                close();
                            } catch (IOException ignore) {
                                throw new RuntimeException("Unable to read " + name, e.getCause());
                            } finally {
                                lock.readLock().lock();
                            }
                        } else {
                            throw new RuntimeException("Unable to read " + name, e);
                        }
                    }
                }
                // reading the entry directly from the JAR
                final boolean interrupted = Thread.interrupted();
                try {
                    return pathTree.getOriginalTree().apply(name, visit -> {
                        if (visit == null) {
                            return null;
                        }
                        try {
                            return Files.readAllBytes(visit.getPath());
                        } catch (IOException e) {
                            throw new RuntimeException("Unable to read " + name, e);
                        }
                    });
                } finally {
                    if (interrupted) {
                        Thread.currentThread().interrupt();
                    }
                }
            } finally {
                lock.readLock().unlock();
            }
        }

        @Override
        public boolean isDirectory() {
            lock.readLock().lock();
            try {
                if (pathTree.isOpen()) {
                    return Files.isDirectory(path);
                }
                return apply(tree -> tree.apply(name, visit -> visit == null ? null : Files.isDirectory(visit.getPath())));
            } finally {
                lock.readLock().unlock();
            }
        }
    }
}
