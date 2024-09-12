package io.quarkus.bootstrap.runner;

import static io.quarkus.bootstrap.runner.VirtualThreadSupport.isVirtualThread;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.jar.JarFile;

import io.smallrye.common.io.jar.JarFiles;

public class JarFileReference {

    // This is required to perform cleanup of JarResource::jarFileReference without breaking racy updates
    private CompletableFuture<JarFileReference> completedFuture;
    // Guarded by an atomic reader counter that emulate the behaviour of a read/write lock.
    // To enable virtual threads compatibility and avoid pinning it is not possible to use an explicit read/write lock
    // because the jarFile access may happen inside a native call (for example triggered by the RunnerClassLoader)
    // and then it is necessary to avoid blocking on it.
    private final JarFile jarFile;

    // The referenceCounter - 1 represents the number of effective readers (#aqcuire - #release), while the first
    // reference is used to determine if a close has been required.
    // The JarFileReference is created as already acquired and that's why the referenceCounter starts from 2
    private final AtomicInteger referenceCounter = new AtomicInteger(2);

    private JarFileReference(JarFile jarFile) {
        this.jarFile = jarFile;
    }

    public static JarFileReference completeWith(CompletableFuture<JarFileReference> completableFuture, JarFile jarFile) {
        Objects.requireNonNull(completableFuture);
        var jarFileRef = new JarFileReference(jarFile);
        jarFileRef.completedFuture = completableFuture;
        completableFuture.complete(jarFileRef);
        return jarFileRef;
    }

    public static CompletableFuture<JarFileReference> completedWith(JarFile jarFile) {
        var jarFileRef = new JarFileReference(jarFile);
        jarFileRef.completedFuture = CompletableFuture.completedFuture(jarFileRef);
        return jarFileRef.completedFuture;
    }

    /**
     * Increase the readers counter of the jarFile.
     *
     * @return true if the acquiring succeeded: it's now safe to access and use the inner jarFile.
     *         false if the jar reference is going to be closed and then no longer usable.
     */
    private boolean acquire() {
        while (true) {
            final int count = referenceCounter.get();
            // acquire can increase the counter absolute value, only if it's not 0
            if (count == 0) {
                return false;
            }
            if (referenceCounter.compareAndSet(count, addCount(count, 1))) {
                return true;
            }
        }
    }

    /**
     * This is not allowed to change the sign of count (unless put it to 0)
     */
    private static int addCount(final int count, int delta) {
        assert count != 0;
        if (count < 0) {
            delta = -delta;
        }
        return count + delta;
    }

    /**
     * Decrease the readers counter of the jarFile.
     * If the counter drops to 0 and a release has been requested also closes the jarFile.
     *
     * @return true if the release also closes the underlying jarFile.
     */
    private boolean release(JarResource jarResource) {
        while (true) {
            final int count = referenceCounter.get();
            // Both 1 and 0 are invalid states, because:
            // - count = 1 means that we're trying to release a jarFile not yet marked for closing
            // - count = 0 means that the jarFile has been already closed
            if (count == 1 || count == 0) {
                throw new IllegalStateException("Duplicate release? The reference counter cannot be " + count);
            }
            if (referenceCounter.compareAndSet(count, addCount(count, -1))) {
                if (count == -1) {
                    silentCloseJarResources(jarResource);
                    return true;
                }
                return false;
            }
        }
    }

    private void silentCloseJarResources(JarResource jarResource) {
        // we need to make sure we're not deleting others state
        jarResource.jarFileReference.compareAndSet(completedFuture, null);
        try {
            jarFile.close();
        } catch (IOException e) {
            // ignore
        }
    }

    /**
     * Ask to close this reference.
     * If there are no readers currently accessing the jarFile also close it, otherwise defer the closing when the last reader
     * will leave.
     */
    void markForClosing(JarResource jarResource) {
        while (true) {
            int count = referenceCounter.get();
            if (count <= 0) {
                // we're relaxed in case of multiple close requests
                return;
            }
            // close must change the value into a negative one or zeroing
            if (referenceCounter.compareAndSet(count, addCount(-count, -1))) {
                if (count == 1) {
                    silentCloseJarResources(jarResource);
                }
            }
        }
    }

    @FunctionalInterface
    interface JarFileConsumer<T> {
        T apply(JarFile jarFile, Path jarPath, String resource);
    }

    static <T> T withJarFile(JarResource jarResource, String resource, JarFileConsumer<T> fileConsumer) {

        // Happy path: the jar reference already exists and it's ready to be used
        final var localJarFileRefFuture = jarResource.jarFileReference.get();
        if (localJarFileRefFuture != null && localJarFileRefFuture.isDone()) {
            JarFileReference jarFileReference = localJarFileRefFuture.join();
            if (jarFileReference.acquire()) {
                return consumeSharedJarFile(jarFileReference, jarResource, resource, fileConsumer);
            }
        }

        // There's no valid jar reference, so load a new one

        // Platform threads can load the jarfile asynchronously and eventually blocking till not ready
        // to avoid loading the same jarfile multiple times in parallel
        if (!isVirtualThread()) {
            // It's ok to eventually block on a join() here since we're sure this is used only by platform thread
            return consumeSharedJarFile(asyncLoadAcquiredJarFile(jarResource), jarResource, resource, fileConsumer);
        }

        // Virtual threads needs to load the jarfile synchronously to avoid blocking. This means that eventually
        // multiple threads could load the same jarfile in parallel and this duplication has to be reconciled
        final var newJarFileRef = syncLoadAcquiredJarFile(jarResource);
        // We can help an in progress close to get rid of the previous jarFileReference, because
        // JarFileReference::silentCloseJarResources verify first if this hasn't changed in the meantime
        if (jarResource.jarFileReference.compareAndSet(localJarFileRefFuture, newJarFileRef) ||
                jarResource.jarFileReference.compareAndSet(null, newJarFileRef)) {
            // The new file reference has been successfully published and can be used by the current and other threads
            // The join() cannot be blocking here since the CompletableFuture has been created already completed
            return consumeSharedJarFile(newJarFileRef.join(), jarResource, resource, fileConsumer);
        }

        // The newly created file reference hasn't been published, so it can be used exclusively by the current virtual thread
        return consumeUnsharedJarFile(newJarFileRef, jarResource, resource, fileConsumer);
    }

    private static <T> T consumeSharedJarFile(JarFileReference jarFileReference,
            JarResource jarResource, String resource, JarFileConsumer<T> fileConsumer) {
        try {
            return fileConsumer.apply(jarFileReference.jarFile, jarResource.jarPath, resource);
        } finally {
            jarFileReference.release(jarResource);
        }
    }

    private static <T> T consumeUnsharedJarFile(CompletableFuture<JarFileReference> jarFileReferenceFuture,
            JarResource jarResource, String resource, JarFileConsumer<T> fileConsumer) {
        JarFileReference jarFileReference = jarFileReferenceFuture.join();
        try {
            return fileConsumer.apply(jarFileReference.jarFile, jarResource.jarPath, resource);
        } finally {
            boolean closed = jarFileReference.release(jarResource);
            assert !closed;
            // Check one last time if the file reference can be published and reused by other threads, otherwise close it
            if (!jarResource.jarFileReference.compareAndSet(null, jarFileReferenceFuture)) {
                jarFileReference.markForClosing(jarResource);
            }
        }
    }

    private static CompletableFuture<JarFileReference> syncLoadAcquiredJarFile(JarResource jarResource) {
        try {
            return JarFileReference.completedWith(JarFiles.create(jarResource.jarPath.toFile()));
        } catch (IOException e) {
            throw new RuntimeException("Failed to open " + jarResource.jarPath, e);
        }
    }

    private static JarFileReference asyncLoadAcquiredJarFile(JarResource jarResource) {
        CompletableFuture<JarFileReference> newJarRefFuture = new CompletableFuture<>();
        CompletableFuture<JarFileReference> existingJarRefFuture = null;
        JarFileReference existingJarRef = null;

        do {
            if (jarResource.jarFileReference.compareAndSet(null, newJarRefFuture)) {
                try {
                    return JarFileReference.completeWith(newJarRefFuture, JarFiles.create(jarResource.jarPath.toFile()));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
            existingJarRefFuture = jarResource.jarFileReference.get();
            existingJarRef = existingJarRefFuture == null ? null : existingJarRefFuture.join();
        } while (existingJarRef == null || !existingJarRef.acquire());
        return existingJarRef;
    }
}
