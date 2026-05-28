package io.quarkus.paths;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.jar.Manifest;

import io.quarkus.fs.util.ZipUtils;
import io.quarkus.fs.util.rozip.ReadOnlyZipFileSystem;

public class ArchivePathTree extends PathTreeWithManifest implements PathTree {

    private static final String DISABLE_JAR_CACHE_PROPERTY = "quarkus.bootstrap.disable-jar-cache";
    private static final boolean ENABLE_SHARING = !Boolean.getBoolean(DISABLE_JAR_CACHE_PROPERTY);

    /**
     * Returns an instance of {@link ArchivePathTree} for the {@code path} either from a cache
     * if sharing is enabled or a new instance.
     *
     * @param path path to an archive
     * @return instance of {@link ArchivePathTree}, never null
     */
    static ArchivePathTree forPath(Path path) {
        return forPath(path, null, true);
    }

    /**
     * Caching of archive path trees with {@link PathFilter}'s isn't currently supported.
     * If {@code filter} argument is not null, the method will return a non-cacheable implementation
     * of {@link ArchivePathTree}.
     * <p>
     * Otherwise, the method returns an instance of {@link ArchivePathTree} for the {@code path} either from a cache
     * if sharing is enabled or a new instance.
     *
     * @param path path to an archive
     * @param filter filter to apply
     * @return instance of {@link ArchivePathTree}, never null
     */
    static ArchivePathTree forPath(Path path, PathFilter filter) {
        return forPath(path, filter, true);
    }

    /**
     * Caching of archive path trees with {@link PathFilter}'s isn't currently supported.
     * If {@code filter} argument is not null, the method will return a non-cacheable implementation
     * of {@link ArchivePathTree}.
     * <p>
     * Otherwise, the method returns an instance of {@link ArchivePathTree} for the {@code path} either from a cache
     * if sharing is enabled or a new instance.
     *
     * @param path path to an archive
     * @param filter filter to apply
     * @param manifestEnabled if reading the manifest is enabled, always true if sharing is enabled
     * @return instance of {@link ArchivePathTree}, never null
     */
    static ArchivePathTree forPath(Path path, PathFilter filter, boolean manifestEnabled) {
        if (filter != null || !ENABLE_SHARING) {
            return new ArchivePathTree(path, filter, manifestEnabled);
        }

        return SharedArchivePathTree.forPath(path);
    }

    protected final Path archive;
    private final PathFilter pathFilter;
    private transient volatile Set<String> resourceNames;

    ArchivePathTree(Path archive) {
        this(archive, null);
    }

    ArchivePathTree(Path archive, PathFilter pathFilter) {
        this(archive, pathFilter, true);
    }

    ArchivePathTree(Path archive, PathFilter pathFilter, boolean manifestEnabled) {
        super(manifestEnabled);
        this.archive = archive;
        this.pathFilter = pathFilter;
    }

    @Override
    public boolean isArchiveOrigin() {
        return true;
    }

    @Override
    public Collection<Path> getRoots() {
        return List.of(archive);
    }

    @Override
    public void walk(PathVisitor visitor) {
        try (OpenPathTree open = open()) {
            open.walk(visitor);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read " + archive, e);
        }
    }

    @Override
    public void walkRaw(PathVisitor visitor) {
        try (OpenPathTree open = open()) {
            open.walkRaw(visitor);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read " + archive, e);
        }
    }

    @Override
    public void walkIfContains(String resourceDirName, PathVisitor visitor) {
        ensureResourcePath(resourceDirName);
        if (!PathFilter.isVisible(pathFilter, resourceDirName)) {
            return;
        }
        try (OpenPathTree open = open()) {
            open.walkIfContains(resourceDirName, visitor);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read " + archive, e);
        }
    }

    private void ensureResourcePath(String path) {
        PathTreeVisit.ensureResourcePath(archive.getFileSystem(), path);
    }

    @Override
    public Set<String> getResourceNames() {
        return resourceNames == null ? resourceNames = super.getResourceNames() : resourceNames;
    }

    @Override
    protected <T> T apply(String resourceName, Function<PathVisit, T> func, boolean manifestEnabled) {
        ensureResourcePath(resourceName);
        if (!PathFilter.isVisible(pathFilter, resourceName)) {
            return func.apply(null);
        }
        try (OpenPathTree open = open()) {
            return open.apply(resourceName, func);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read " + archive, e);
        }
    }

    @Override
    public void accept(String resourceName, Consumer<PathVisit> consumer) {
        ensureResourcePath(resourceName);
        if (!PathFilter.isVisible(pathFilter, resourceName)) {
            consumer.accept(null);
            return;
        }
        try (OpenPathTree open = open()) {
            open.accept(resourceName, consumer);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read " + archive, e);
        }
    }

    @Override
    public boolean contains(String resourceName) {
        ensureResourcePath(resourceName);
        if (!PathFilter.isVisible(pathFilter, resourceName)) {
            return false;
        }
        try (OpenPathTree open = open()) {
            return open.contains(resourceName);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read " + archive, e);
        }
    }

    protected ReadOnlyZipFileSystem openFs() throws IOException {
        return (ReadOnlyZipFileSystem) ZipUtils.openReadOnly(archive);
    }

    @Override
    public OpenPathTree open() {
        try {
            return new OpenArchivePathTree(openFs());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(archive, pathFilter, manifestEnabled);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        ArchivePathTree other = (ArchivePathTree) obj;
        return Objects.equals(archive, other.archive) && Objects.equals(pathFilter, other.pathFilter)
                && manifestEnabled == other.manifestEnabled;
    }

    @Override
    public String toString() {
        return archive.toString();
    }

    /**
     * Thread-safe view of an open archive backed by a {@link ReadOnlyZipFileSystem}.
     * <p>
     * {@link ReadOnlyZipFileSystem} uses {@code RandomAccessFile} instead of
     * {@code FileChannel}, making it immune to the JDK issue (JDK-8316882) where an
     * interrupted thread closes the shared {@code FileChannel} (via
     * {@code InterruptibleChannel}), breaking the entire filesystem for all other threads.
     * Because {@code RandomAccessFile} is not interruptible, the thread's interrupt flag is
     * naturally preserved without explicit clearing or restoring.
     */
    protected class OpenArchivePathTree extends OpenContainerPathTree {

        private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

        // we don't make these fields final as we want to nullify them on close
        private ReadOnlyZipFileSystem fs;
        private Path rootPath;

        private volatile boolean open = true;

        protected OpenArchivePathTree(ReadOnlyZipFileSystem fs) {
            super(ArchivePathTree.this.pathFilter, ArchivePathTree.this);
            this.fs = fs;
            this.rootPath = fs.getPath("/");
        }

        @Override
        public boolean isArchiveOrigin() {
            return true;
        }

        @Override
        protected Path getContainerPath() {
            return ArchivePathTree.this.archive;
        }

        @Override
        protected Path getRootPath() {
            return rootPath;
        }

        protected ReentrantReadWriteLock.ReadLock readLock() {
            return lock.readLock();
        }

        protected ReentrantReadWriteLock.WriteLock writeLock() {
            return lock.writeLock();
        }

        @Override
        protected void initManifest(Manifest m) {
            super.initManifest(m);
            ArchivePathTree.this.manifestReadLock().lock();
            try {
                if (!ArchivePathTree.this.manifestInitialized) {
                    ArchivePathTree.this.manifestReadLock().unlock();
                    ArchivePathTree.this.manifestWriteLock().lock();
                    ArchivePathTree.this.initManifest(m);
                    ArchivePathTree.this.manifestReadLock().lock();
                    ArchivePathTree.this.manifestWriteLock().unlock();
                }
            } finally {
                ArchivePathTree.this.manifestReadLock().unlock();
            }
        }

        @Override
        protected void initMultiReleaseMapping(Map<String, String> mrMapping) {
            super.initMultiReleaseMapping(mrMapping);
            if (ArchivePathTree.this.multiReleaseMapping == null) {
                ArchivePathTree.this.initMultiReleaseMapping(mrMapping);
            }
        }

        @Override
        public boolean isOpen() {
            lock.readLock().lock();
            try {
                return open;
            } finally {
                lock.readLock().unlock();
            }
        }

        /**
         * Validates the resource name and resolves it to an entry name accounting for
         * multi-release JARs, or returns {@code null} if the resource is filtered out.
         * Must be called under the read lock.
         */
        private String resolveEntryName(String resourceName) {
            PathTreeVisit.ensureResourcePath(fs, resourceName);
            if (!PathFilter.isVisible(pathFilter, resourceName)) {
                return null;
            }
            return manifestEnabled ? toMultiReleaseResourceName(resourceName) : resourceName;
        }

        private Path resolveEntryPath(String resourceName) {
            String entryName = resolveEntryName(resourceName);
            return entryName != null && fs.entryExists(entryName) ? rootPath.resolve(entryName) : null;
        }

        @Override
        protected <T> T apply(String resourceName, Function<PathVisit, T> func, boolean manifestEnabled) {
            lock.readLock().lock();
            try {
                ensureOpen();
                Path entry = resolveEntryPath(resourceName);
                if (entry == null) {
                    return func.apply(null);
                }
                return PathTreeVisit.process(getContainerPath(), rootPath, entry, pathFilter, func);
            } finally {
                lock.readLock().unlock();
            }
        }

        @Override
        public void accept(String resourceName, Consumer<PathVisit> consumer) {
            lock.readLock().lock();
            try {
                ensureOpen();
                Path entry = resolveEntryPath(resourceName);
                if (entry == null) {
                    consumer.accept(null);
                    return;
                }
                PathTreeVisit.consume(getContainerPath(), rootPath, entry, pathFilter, consumer);
            } finally {
                lock.readLock().unlock();
            }
        }

        @Override
        public void walk(PathVisitor visitor) {
            lock.readLock().lock();
            try {
                ensureOpen();
                super.walk(visitor);
            } finally {
                lock.readLock().unlock();
            }
        }

        @Override
        public void walkRaw(PathVisitor visitor) {
            lock.readLock().lock();
            try {
                ensureOpen();
                super.walkRaw(visitor);
            } finally {
                lock.readLock().unlock();
            }
        }

        @Override
        public void walkIfContains(String resourceDirName, PathVisitor visitor) {
            lock.readLock().lock();
            try {
                ensureOpen();
                super.walkIfContains(resourceDirName, visitor);
            } finally {
                lock.readLock().unlock();
            }
        }

        @Override
        public Set<String> getResourceNames() {
            return resourceNames == null ? resourceNames = super.getResourceNames() : resourceNames;
        }

        @Override
        public boolean contains(String resourceName) {
            lock.readLock().lock();
            try {
                ensureOpen();
                String entryName = resolveEntryName(resourceName);
                return entryName != null && fs.entryExists(entryName);
            } finally {
                lock.readLock().unlock();
            }
        }

        @Override
        public Path getPath(String resourceName) {
            lock.readLock().lock();
            try {
                ensureOpen();
                return resolveEntryPath(resourceName);
            } finally {
                lock.readLock().unlock();
            }
        }

        /**
         * Make sure you use this method inside a lock.
         */
        private void ensureOpen() {
            // let's not use isOpen() as ensureOpen() is always used inside a read lock
            if (open) {
                return;
            }
            throw new RuntimeException("Failed to access " + ArchivePathTree.this.getRoots()
                    + " because the FileSystem has been closed");
        }

        @Override
        public void close() throws IOException {
            lock.writeLock().lock();
            try {
                open = false;
                rootPath = null;
                fs.close();
            } catch (IOException e) {
                throw e;
            } finally {
                // even when we close the fs, everything is kept as is in the fs instance
                // and typically the cen, which is quite large
                // let's make sure the fs is nullified for it to be garbage collected
                fs = null;
                lock.writeLock().unlock();
            }
        }

        @Override
        public PathTree getOriginalTree() {
            return ArchivePathTree.this;
        }

        @Override
        public String toString() {
            return ArchivePathTree.this.toString();
        }
    }
}
