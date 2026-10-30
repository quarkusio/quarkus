package io.quarkus.paths;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import java.util.function.Function;

import io.quarkus.fs.util.rozip.ReadOnlyZipFileSystem;

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
    private final AtomicReference<SharedOpenArchivePathTree> lastOpen = new AtomicReference<>();
    /**
     * Guards creation of new open archive trees. It is not a monitor, since a new archive file system is opened
     * while holding it, which would pin virtual threads on JDK versions before 24.
     */
    private final ReentrantLock openLock = new ReentrantLock();

    SharedArchivePathTree(Path archive) {
        super(archive);
    }

    @Override
    public OpenPathTree open() {
        var acquired = acquireLastOpen();
        if (acquired != null) {
            return acquired;
        }
        // Creating a new open archive tree is guarded by a lock to make sure there is at most one open instance
        // of this archive at any time. Otherwise, concurrent callers could each create their own instance,
        // only one of which would be referenced by lastOpen. Once that one is closed, subsequent callers
        // would end up opening new instances instead of sharing the ones that remain open.
        openLock.lock();
        try {
            acquired = acquireLastOpen();
            if (acquired != null) {
                return acquired;
            }
            final SharedOpenArchivePathTree newOpen;
            try {
                newOpen = new SharedOpenArchivePathTree(openFs());
            } catch (Exception e) {
                throw new RuntimeException("Failed to read " + archive, e);
            }
            lastOpen.set(newOpen);
            return new CallerOpenPathTree(newOpen);
        } finally {
            openLock.unlock();
        }
    }

    private CallerOpenPathTree acquireLastOpen() {
        var current = lastOpen.get();
        return current == null ? null : current.acquire();
    }

    private class SharedOpenArchivePathTree extends OpenArchivePathTree {

        private final AtomicInteger users = new AtomicInteger(1);

        protected SharedOpenArchivePathTree(ReadOnlyZipFileSystem fs) {
            super(fs);
            openCount.incrementAndGet();
        }

        /**
         * Returns a new handle for this open archive tree to the caller
         * as long as this open archive tree is still open and is still
         * the last one that was open for this archive. Otherwise, the method
         * will return null.
         *
         * @return a new instance of {@link CallerOpenPathTree} or null,
         *         if the current open archive tree has been closed or another open
         *         archive tree has been created for this archive
         */
        private CallerOpenPathTree acquire() {
            readLock().lock();
            try {
                final boolean result = lastOpen.get() == this && isOpen();
                if (result) {
                    users.incrementAndGet();
                    return new CallerOpenPathTree(this);
                }
            } finally {
                readLock().unlock();
            }
            return null;
        }

        @Override
        public OpenPathTree open() {
            return SharedArchivePathTree.this.open();
        }

        @Override
        public void close() throws IOException {
            // Fast path: if other users remain, no cleanup needed — avoid the write lock entirely.
            if (users.decrementAndGet() > 0) {
                return;
            }
            // Slow path: we may be the last user. Try to acquire the write lock to perform cleanup.
            // Re-check under the lock because between our decrement and lock acquisition,
            // another thread could have called acquire() (incrementing users) and then closed
            // (decrementing back to 0), meaning two threads both saw 0 and race for the lock.
            // The !isOpen() guard handles this: the first closer sets open=false via super.close(),
            // so the second closer sees !isOpen() and skips the duplicate cleanup.
            //
            // The write lock is never waited for, since waiting could deadlock if a reader holding the read lock
            // needed a lock held by the caller of this method. If the write lock is not available, either
            // - another closer holds it, which re-checks the users after releasing the lock, or
            // - a thread in acquire() holds the read lock, which will become a user that will close its handle later.
            while (writeLock().tryLock()) {
                try {
                    if (!isOpen()) {
                        return;
                    }
                    if (users.get() == 0) {
                        // clears lastOpen only if it still references this instance
                        lastOpen.compareAndSet(this, null);
                        if (openCount.decrementAndGet() == 0) {
                            removeFromCache(archive);
                        }
                        super.close();
                        return;
                    }
                } finally {
                    writeLock().unlock();
                }
                // While we were holding the lock, a user could have closed its handle and given up on the lock,
                // in which case the cleanup is up to us.
                if (users.get() > 0) {
                    return;
                }
            }
        }

        @Override
        public String toString() {
            return SharedArchivePathTree.this.toString();
        }
    }

    /**
     * This is a caller "view" of an underlying {@link OpenPathTree} instance that
     * delegates only the first {@link #close()} call by the caller to the underlying {@link OpenPathTree} instance
     * with subsequent {@link #close()} calls ignored.
     */
    private static class CallerOpenPathTree implements OpenPathTree {

        private final SharedOpenArchivePathTree delegate;
        private final AtomicBoolean closed = new AtomicBoolean();

        private CallerOpenPathTree(SharedOpenArchivePathTree delegate) {
            this.delegate = delegate;
        }

        @Override
        public boolean isArchiveOrigin() {
            return delegate.isArchiveOrigin();
        }

        @Override
        public PathTree getOriginalTree() {
            return delegate.getOriginalTree();
        }

        @Override
        public boolean isOpen() {
            return !closed.get() && delegate.isOpen();
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
        public ManifestAttributes getManifestAttributes() {
            return delegate.getManifestAttributes();
        }

        @Override
        public void walk(PathVisitor visitor) {
            delegate.walk(visitor);
        }

        @Override
        public void walkRaw(PathVisitor visitor) {
            delegate.walkRaw(visitor);
        }

        @Override
        public void walkIfContains(String resourceDirName, PathVisitor visitor) {
            delegate.walkIfContains(resourceDirName, visitor);
        }

        @Override
        public Set<String> getResourceNames() {
            return delegate.getResourceNames();
        }

        @Override
        public <T> T apply(String resourceName, Function<PathVisit, T> func) {
            return delegate.apply(resourceName, func);
        }

        @Override
        public void accept(String resourceName, Consumer<PathVisit> consumer) {
            delegate.accept(resourceName, consumer);
        }

        @Override
        public void acceptAll(String resourceName, Consumer<PathVisit> consumer) {
            delegate.acceptAll(resourceName, consumer);
        }

        @Override
        public boolean contains(String resourceName) {
            return delegate.contains(resourceName);
        }

        @Override
        public OpenPathTree open() {
            return delegate.open();
        }

        @Override
        public void close() throws IOException {
            if (!closed.compareAndSet(false, true)) {
                return;
            }
            delegate.close();
        }

        @Override
        public String toString() {
            return delegate.toString();
        }
    }
}
