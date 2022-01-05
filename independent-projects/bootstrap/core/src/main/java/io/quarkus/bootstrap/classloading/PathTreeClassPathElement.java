package io.quarkus.bootstrap.classloading;

import io.quarkus.maven.dependency.ArtifactKey;
import io.quarkus.paths.OpenPathTree;
import io.quarkus.paths.PathTree;
import io.quarkus.paths.PathVisit;
import io.quarkus.paths.PathVisitor;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.security.cert.Certificate;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Function;
import java.util.jar.Manifest;

public class PathTreeClassPathElement extends AbstractClassPathElement {

    static class ZipFileMayHaveChangedException extends RuntimeException {
        public ZipFileMayHaveChangedException(Throwable cause) {
            super(cause);
        }
    }

    static {
        //force this class to be loaded
        //if quarkus is recompiled it needs to have already
        //been loaded
        //this is just a convenience for quarkus devs that means exit
        //should work properly if you recompile while quarkus is running
        new ZipFileMayHaveChangedException(null);
    }

    private final ReadWriteLock lock;
    private final OpenPathTree pathTree;
    private final boolean runtime;
    private final ArtifactKey dependencyKey;
    private volatile boolean closed;
    private volatile Set<String> resources;

    public PathTreeClassPathElement(PathTree pathTree, boolean runtime) {
        this(pathTree, runtime, null);
    }

    public PathTreeClassPathElement(PathTree pathTree, boolean runtime, ArtifactKey dependencyKey) {
        this.pathTree = Objects.requireNonNull(pathTree, "Path tree is null").openTree();
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
     * Sometimes Vert.x may be looking for a resource like 'META-INF/resources//index.html'.
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
        return withOpenTree(
                tree -> tree.processPath(sanitized, visit -> visit == null ? null : new PathTreeClassPathResource(visit)));
    }

    @Override
    public <T> T withOpenTree(Function<OpenPathTree, T> func) {
        lock.readLock().lock();
        try {
            if (closed) {
                //we still need this to work if it is closed, so shutdown hooks work
                //once it is closed it simply does not hold on to any resources
                try (OpenPathTree openTree = pathTree.getOriginalTree().openTree()) {
                    return func.apply(openTree);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            } else {
                return func.apply(pathTree);
            }
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Set<String> getProvidedResources() {
        Set<String> resources = this.resources;
        if (resources == null) {
            resources = withOpenTree(tree -> {
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
        return withOpenTree(OpenPathTree::getManifest);
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
            closed = true;
        } finally {
            lock.writeLock().unlock();
        }
    }

    private class PathTreeClassPathResource implements ClassPathResource {

        private final String name;
        private final Path path;
        private volatile URL url;

        private PathTreeClassPathResource(PathVisit visit) {
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
                if (closed) {
                    return url = withOpenTree(
                            tree -> tree.processPath(name, visit -> visit == null ? null : visit.getUrl()));
                }
                return url = path.toUri().toURL();
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
                if (closed) {
                    return withOpenTree(
                            tree -> tree.processPath(name, visit -> visit == null ? null : readPath(visit.getPath())));
                }
                return readPath(path);
            } finally {
                lock.readLock().unlock();
            }
        }

        @Override
        public boolean isDirectory() {
            lock.readLock().lock();
            try {
                if (closed) {
                    return withOpenTree(tree -> tree.processPath(name,
                            visit -> visit == null ? null : Files.isDirectory(visit.getPath())));
                }
                return Files.isDirectory(path);
            } finally {
                lock.readLock().unlock();
            }
        }

        private byte[] readPath(Path path) {
            try {
                try {
                    return Files.readAllBytes(path);
                } catch (InterruptedIOException e) {
                    // if we are interrupted reading data we finish the op, then just
                    // re-interrupt the thread state
                    final byte[] bytes = Files.readAllBytes(path);
                    Thread.currentThread().interrupt();
                    return bytes;
                }
            } catch (IOException e) {
                if (!closed) {
                    throw new ZipFileMayHaveChangedException(e);
                }
                throw new RuntimeException("Unable to read " + path.toUri(), e);
            }
        }
    }
}
