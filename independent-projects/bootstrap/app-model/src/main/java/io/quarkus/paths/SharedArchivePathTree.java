package io.quarkus.paths;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.jar.Manifest;

/**
 * While {@link ArchivePathTree} implementation is thread-safe, this implementation
 * provides a way for multiple concurrent users to share the same instance of an open archive,
 * instead of every user opening its own copy of it.
 */
class SharedArchivePathTree extends ArchivePathTree {

    // It would probably be better to have a weak hash map based cache,
    // however as the first iteration it'll already be more efficient than before
    private static final Map<Path, SharedArchivePathTree> CACHE = new ConcurrentHashMap<>();

    /**
     * Returns an instance of {@link ArchivePathTree} for the {@code path} either from a cache
     * or creates a new instance for it and puts it in the cache.
     *
     * @param path path to an archive
     * @return instance of {@link ArchivePathTree}, never null
     */
    static ArchivePathTree forPath(Path path) {
        return CACHE.computeIfAbsent(path, SharedArchivePathTree::new);
    }

    /**
     * Removes an entry for {@code path} from the path tree cache. If the cache
     * does not contain an entry for the {@code path}, the method will return silently.
     *
     * @param path path to remove from the cache
     */
    static void removeFromCache(Path path) {
        CACHE.remove(path);
    }

    private final AtomicInteger openCount = new AtomicInteger();
    private volatile SharedOpenArchivePathTree lastOpen;

    SharedArchivePathTree(Path archive) {
        super(archive);
    }

    @Override
    public OpenPathTree open() {
        var lastOpen = this.lastOpen;
        if (lastOpen != null && lastOpen.acquire()) {
            return new CallerOpenPathTree(lastOpen);
        }
        try {
            this.lastOpen = new SharedOpenArchivePathTree(openFs());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return new CallerOpenPathTree(this.lastOpen);
    }

    private class SharedOpenArchivePathTree extends OpenArchivePathTree {

        private final AtomicInteger users = new AtomicInteger(1);

        protected SharedOpenArchivePathTree(FileSystem fs) {
            super(fs);
            openCount.incrementAndGet();
        }

        private boolean acquire() {
            readLock().lock();
            final boolean result = lastOpen == this && isOpen();
            if (result) {
                users.incrementAndGet();
            }
            readLock().unlock();
            return result;
        }

        @Override
        public void close() throws IOException {
            writeLock().lock();
            final boolean close = users.decrementAndGet() == 0;
            try {
                if (close) {
                    if (lastOpen == this) {
                        lastOpen = null;
                    }
                    if (openCount.decrementAndGet() == 0) {
                        removeFromCache(archive);
                    }
                    super.close();
                }
            } finally {
                writeLock().unlock();
            }
        }
    }

    /**
     * This is a caller "view" of an underlying {@link OpenPathTree} instance that
     * delegates only the first {@link #close()} call by the caller to the underlying {@link OpenPathTree} instance
     * with subsequent {@link #close()} calls ignored.
     */
    private static class CallerOpenPathTree implements OpenPathTree {

        private final SharedOpenArchivePathTree delegate;
        private volatile boolean closed;

        private CallerOpenPathTree(SharedOpenArchivePathTree delegate) {
            this.delegate = delegate;
        }

        @Override
        public PathTree getOriginalTree() {
            return delegate.getOriginalTree();
        }

        @Override
        public boolean isOpen() {
            return !closed && delegate.isOpen();
        }

        @Override
        public Path getPath(String relativePath) {
            return delegate.getPath(relativePath);
        }

        @Override
        public Collection<Path> getRoots() {
            return delegate.getRoots();
        }

        @Override
        public Manifest getManifest() {
            return delegate.getManifest();
        }

        @Override
        public void walk(PathVisitor visitor) {
            delegate.walk(visitor);
        }

        @Override
        public <T> T apply(String relativePath, Function<PathVisit, T> func) {
            return delegate.apply(relativePath, func);
        }

        @Override
        public void accept(String relativePath, Consumer<PathVisit> consumer) {
            delegate.accept(relativePath, consumer);
        }

        @Override
        public boolean contains(String relativePath) {
            return delegate.contains(relativePath);
        }

        @Override
        public OpenPathTree open() {
            return delegate.open();
        }

        @Override
        public void close() throws IOException {
            if (closed) {
                return;
            }
            delegate.writeLock().lock();
            try {
                if (!closed) {
                    closed = true;
                    delegate.close();
                }
            } finally {
                delegate.writeLock().unlock();
            }
        }
    }
}
