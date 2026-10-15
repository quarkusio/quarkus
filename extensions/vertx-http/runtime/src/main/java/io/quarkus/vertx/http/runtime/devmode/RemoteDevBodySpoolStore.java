package io.quarkus.vertx.http.runtime.devmode;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.EnumSet;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.RejectedExecutionException;

import io.vertx.core.Future;
import io.vertx.core.Promise;

/**
 * Owns the temporary directory and spool files for one {@link RemoteSyncHandler}.
 * <p>
 * A spool is created before request input is resumed, closed before it is read, and deleted when its completed body is
 * closed. The last spool cleanup removes the directory; {@link #close()} prevents new spools while a restart is in
 * progress.
 */
final class RemoteDevBodySpoolStore {

    private static final Set<PosixFilePermission> DIRECTORY_PERMISSIONS = EnumSet.of(
            PosixFilePermission.OWNER_READ,
            PosixFilePermission.OWNER_WRITE,
            PosixFilePermission.OWNER_EXECUTE);
    private static final Set<PosixFilePermission> FILE_PERMISSIONS = EnumSet.of(
            PosixFilePermission.OWNER_READ,
            PosixFilePermission.OWNER_WRITE);

    private final RemoteSyncHandler owner;
    private Path directory;
    private boolean closed;
    private int pendingSpoolCleanups;

    RemoteDevBodySpoolStore(RemoteSyncHandler owner) {
        this.owner = owner;
    }

    /**
     * Creates a writable spool file, using owner-only permissions when the file system supports POSIX permissions.
     */
    Future<Spool> create() {
        try {
            return owner.executeBlocking(() -> {
                synchronized (this) {
                    if (closed) {
                        throw new IOException("Remote-dev body spool store is closed");
                    }
                    if (directory == null) {
                        directory = createTempDirectory();
                    }
                    Path file = createTempFile(directory);
                    return new Spool(file);
                }
            }, false);
        } catch (RejectedExecutionException e) {
            return Future.failedFuture(e);
        }
    }

    /**
     * Deletes a spool and removes the directory when it no longer contains active spools.
     */
    Future<Void> delete(Spool spool) {
        synchronized (this) {
            pendingSpoolCleanups++;
        }
        return executeCleanup(() -> {
            deleteSpool(spool);
            return null;
        });
    }

    synchronized Path directory() {
        return directory;
    }

    /**
     * Prevents new spools and makes a best-effort attempt to remove an already idle directory.
     */
    void close() {
        synchronized (this) {
            closed = true;
        }
        try {
            // This is a single best-effort directory operation. Collector cleanup closes active spool files asynchronously.
            deleteDirectoryIfIdle();
        } catch (IOException ignored) {
            owner.bodyCleanupFailed();
        }
    }

    private Future<Void> executeCleanup(Callable<Void> cleanup) {
        try {
            return owner.executeBlocking(cleanup, false)
                    .recover(failure -> failure instanceof RejectedExecutionException
                            ? executeFallbackCleanup(cleanup)
                            : Future.failedFuture(failure));
        } catch (RejectedExecutionException e) {
            return executeFallbackCleanup(cleanup);
        }
    }

    private static Future<Void> executeFallbackCleanup(Callable<Void> cleanup) {
        // A classpath-change restart can stop the Vert.x worker executor before closing this handler.
        // Keep that lifecycle thread and any event loop nonblocking while the old handler releases its files.
        Promise<Void> result = Promise.promise();
        Thread thread = new Thread(() -> {
            try {
                cleanup.call();
                result.complete();
            } catch (Exception failure) {
                result.fail(failure);
            }
        }, "Quarkus remote-dev body cleanup");
        thread.setDaemon(true);
        try {
            thread.start();
        } catch (RuntimeException failure) {
            result.tryFail(failure);
        }
        return result.future();
    }

    private void deleteSpool(Spool spool) throws IOException {
        IOException failure = null;
        try {
            spool.delete();
        } catch (IOException e) {
            failure = e;
        }
        try {
            spoolCleanupCompleted();
        } catch (IOException e) {
            if (failure == null) {
                failure = e;
            } else {
                failure.addSuppressed(e);
            }
        }
        if (failure != null) {
            throw failure;
        }
    }

    private void spoolCleanupCompleted() throws IOException {
        synchronized (this) {
            pendingSpoolCleanups--;
        }
        deleteDirectoryIfIdle();
    }

    private synchronized void deleteDirectoryIfIdle() throws IOException {
        if (pendingSpoolCleanups != 0 || directory == null) {
            return;
        }
        try {
            Files.deleteIfExists(directory);
            directory = null;
        } catch (DirectoryNotEmptyException ignored) {
            // An active collector still owns a spool. The last spool cleanup retries the directory deletion.
        }
    }

    private static Path createTempDirectory() throws IOException {
        if (Path.of("").getFileSystem().supportedFileAttributeViews().contains("posix")) {
            return Files.createTempDirectory("quarkus-remote-dev-",
                    PosixFilePermissions.asFileAttribute(DIRECTORY_PERMISSIONS));
        }
        return Files.createTempDirectory("quarkus-remote-dev-");
    }

    private static Path createTempFile(Path directory) throws IOException {
        if (directory.getFileSystem().supportedFileAttributeViews().contains("posix")) {
            return Files.createTempFile(directory, "request-", ".body",
                    PosixFilePermissions.asFileAttribute(FILE_PERMISSIONS));
        }
        return Files.createTempFile(directory, "request-", ".body");
    }

    /**
     * One request body spool.
     * <p>
     * It is writable until {@link #closeForReading()} and remains readable until {@link #delete()}.
     */
    static final class Spool {

        private final Path path;
        private FileChannel channel;
        private boolean deleted;

        private Spool(Path path) throws IOException {
            this.path = path;
            channel = FileChannel.open(path, StandardOpenOption.WRITE);
        }

        /**
         * Appends an inbound request chunk.
         */
        synchronized void write(byte[] bytes) throws IOException {
            if (channel == null) {
                throw new IOException("Remote-dev body spool is closed");
            }
            ByteBuffer buffer = ByteBuffer.wrap(bytes);
            while (buffer.hasRemaining()) {
                channel.write(buffer);
            }
        }

        /**
         * Closes the write channel so the spool can be read.
         */
        synchronized void closeForReading() throws IOException {
            closeChannel();
        }

        /**
         * Opens a new stream for reading the completed body.
         */
        synchronized InputStream openInputStream() throws IOException {
            if (channel != null) {
                throw new IOException("Remote-dev body spool is still being written");
            }
            if (deleted) {
                throw new IOException("Remote-dev body spool is deleted");
            }
            return Files.newInputStream(path);
        }

        /**
         * Reads the completed body for operations that require its complete byte array.
         */
        synchronized byte[] readAllBytes() throws IOException {
            if (channel != null) {
                throw new IOException("Remote-dev body spool is still being written");
            }
            if (deleted) {
                throw new IOException("Remote-dev body spool is deleted");
            }
            return Files.readAllBytes(path);
        }

        /**
         * Closes the spool and deletes its file.
         */
        synchronized void delete() throws IOException {
            IOException closeFailure = null;
            try {
                closeChannel();
            } catch (IOException e) {
                closeFailure = e;
            }
            if (!deleted) {
                try {
                    Files.deleteIfExists(path);
                    deleted = true;
                } catch (IOException e) {
                    if (closeFailure != null) {
                        e.addSuppressed(closeFailure);
                    }
                    throw e;
                }
            }
            if (closeFailure != null) {
                throw closeFailure;
            }
        }

        private void closeChannel() throws IOException {
            if (channel != null) {
                try {
                    channel.close();
                } finally {
                    channel = null;
                }
            }
        }
    }
}
