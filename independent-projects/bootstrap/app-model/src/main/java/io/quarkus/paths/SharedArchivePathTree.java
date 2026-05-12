package io.quarkus.paths;

import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;

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
        if (lastOpen != null) {
            var acquired = lastOpen.acquire();
            if (acquired != null) {
                return acquired;
            }
        }
        try {
            lastOpen = this.lastOpen = new SharedOpenArchivePathTree(openFs());
        } catch (Exception e) {
            throw new RuntimeException("Failed to read " + archive, e);
        }
        return new CallerOpenPathTree(lastOpen);
    }

    private class SharedOpenArchivePathTree extends OpenArchivePathTree {

        private final AtomicInteger users = new AtomicInteger(1);

        protected SharedOpenArchivePathTree(FileSystem fs) {
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
                final boolean result = lastOpen == this && isOpen();
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
            // Slow path: we may be the last user. Acquire the write lock to perform cleanup.
            // Re-check under the lock because between our decrement and lock acquisition,
            // another thread could have called acquire() (incrementing users) and then closed
            // (decrementing back to 0), meaning two threads both saw 0 and race for the lock.
            // The !isOpen() guard handles this: the first closer sets open=false via super.close(),
            // so the second closer sees !isOpen() and skips the duplicate cleanup.
            writeLock().lock();
            try {
                if (users.get() > 0 || !isOpen()) {
                    return;
                }
                if (lastOpen == this) {
                    lastOpen = null;
                }
                if (openCount.decrementAndGet() == 0) {
                    removeFromCache(archive);
                }
                super.close();
            } finally {
                writeLock().unlock();
            }
        }

        /**
         * Walks the cause chain of {@code t} looking for a {@link ClosedChannelException}.
         * If one is found and this instance is still the current {@link #lastOpen},
         * the reference is cleared so that the next call to {@link SharedArchivePathTree#open()}
         * creates a fresh {@link java.nio.file.FileSystem} instead of handing out this
         * now-broken instance.
         * <p>
         * This provides recovery from the mid-read interrupt scenario of JDK-8316882:
         * when a thread is interrupted while reading from the shared {@code FileChannel},
         * the channel is closed and the entire {@code ZipFileSystem} becomes unusable.
         *
         * @param t the exception thrown during a content operation
         */
        private void invalidateIfBroken(Throwable t) {
            while (t != null) {
                if (t instanceof ClosedChannelException) {
                    if (lastOpen == this) {
                        lastOpen = null;
                    }
                    return;
                }
                t = t.getCause();
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
            try {
                return delegate.getPath(relativePath);
            } catch (RuntimeException e) {
                invalidateIfBroken(e);
                throw e;
            }
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
            try {
                delegate.walk(visitor);
            } catch (RuntimeException e) {
                invalidateIfBroken(e);
                throw e;
            }
        }

        @Override
        public void walkRaw(PathVisitor visitor) {
            try {
                delegate.walkRaw(visitor);
            } catch (RuntimeException e) {
                invalidateIfBroken(e);
                throw e;
            }
        }

        @Override
        public void walkIfContains(String resourceDirName, PathVisitor visitor) {
            try {
                delegate.walkIfContains(resourceDirName, visitor);
            } catch (RuntimeException e) {
                invalidateIfBroken(e);
                throw e;
            }
        }

        @Override
        public Set<String> getResourceNames() {
            try {
                return delegate.getResourceNames();
            } catch (RuntimeException e) {
                invalidateIfBroken(e);
                throw e;
            }
        }

        @Override
        public <T> T apply(String resourceName, Function<PathVisit, T> func) {
            try {
                return delegate.apply(resourceName, func);
            } catch (RuntimeException e) {
                invalidateIfBroken(e);
                throw e;
            }
        }

        @Override
        public void accept(String resourceName, Consumer<PathVisit> consumer) {
            try {
                delegate.accept(resourceName, consumer);
            } catch (RuntimeException e) {
                invalidateIfBroken(e);
                throw e;
            }
        }

        @Override
        public void acceptAll(String resourceName, Consumer<PathVisit> consumer) {
            try {
                delegate.acceptAll(resourceName, consumer);
            } catch (RuntimeException e) {
                invalidateIfBroken(e);
                throw e;
            }
        }

        @Override
        public boolean contains(String resourceName) {
            try {
                return delegate.contains(resourceName);
            } catch (RuntimeException e) {
                invalidateIfBroken(e);
                throw e;
            }
        }

        private void invalidateIfBroken(Throwable t) {
            delegate.invalidateIfBroken(t);
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
