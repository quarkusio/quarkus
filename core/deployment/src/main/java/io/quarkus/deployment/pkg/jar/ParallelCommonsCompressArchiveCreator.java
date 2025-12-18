package io.quarkus.deployment.pkg.jar;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.zip.Deflater;

import org.apache.commons.compress.archivers.zip.DefaultBackingStoreSupplier;
import org.apache.commons.compress.archivers.zip.ParallelScatterZipCreator;
import org.apache.commons.compress.archivers.zip.ScatterZipOutputStream;
import org.apache.commons.compress.archivers.zip.UnixStat;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntryRequest;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.compress.parallel.InputStreamSupplier;

/**
 * This ArchiveCreator may not be used to build Uberjars.
 * <p>
 * We need the entries to be immutable here, and in the case of Uberjars, we mutate the MANIFEST.MF at the end.
 * <p>
 * It would probably not be that hard to support it though, by storing the Manifest instance,
 * and allow getting it/setting it again, and adding it first in close().
 */
public class ParallelCommonsCompressArchiveCreator implements ArchiveCreator {

    private static final Set<String> MANIFESTS = Set.of("META-INF/MANIFEST.MF", "META-INF\\MANIFEST.MF");
    private static final Set<String> VERSIONS = Set.of("META-INF/versions/", "META-INF\\versions\\");

    private static final InputStreamSupplier EMPTY_SUPPLIER = () -> new ByteArrayInputStream(new byte[0]);

    private static final int DIR_UNIX_MODE = UnixStat.DIR_FLAG | UnixStat.DEFAULT_DIR_PERM;
    private static final int FILE_UNIX_MODE = UnixStat.FILE_FLAG | UnixStat.DEFAULT_FILE_PERM;

    private final Path archivePath;
    private final FileTime entryTimestamp;
    private final ZipArchiveOutputStream archive;
    private final ParallelScatterZipCreator scatterZipCreator;
    private final ScatterZipOutputStream directories;
    private final int compressionMethod;
    private final Path tempDirectory;

    private Manifest manifest;
    private final Map<String, String> addedFiles = new HashMap<>();

    ParallelCommonsCompressArchiveCreator(Path archivePath, boolean compressed, Instant entryTimestamp, Path outputTarget,
            ExecutorService executorService) throws IOException {
        int compressionLevel;
        if (compressed) {
            compressionLevel = Deflater.DEFAULT_COMPRESSION;
            compressionMethod = ZipArchiveOutputStream.DEFLATED;
        } else {
            compressionLevel = Deflater.NO_COMPRESSION;
            compressionMethod = ZipArchiveOutputStream.STORED;
        }

        // make sure we create the parent directory before creating the archive
        if (archivePath.getParent() != null) {
            Files.createDirectories(archivePath.getParent());
        }

        this.archivePath = archivePath;
        this.entryTimestamp = entryTimestamp != null ? FileTime.from(entryTimestamp) : null;
        this.archive = new ZipArchiveOutputStream(archivePath);
        this.archive.setMethod(compressionMethod);
        this.tempDirectory = Files.createTempDirectory(outputTarget, "zip-builder-files");

        scatterZipCreator = new ParallelScatterZipCreator(
                // we need to make sure our own executor won't be shut down by Commons Compress...
                new DoNotShutdownDelegatingExecutorService(executorService),
                new DefaultBackingStoreSupplier(this.tempDirectory),
                compressionLevel);
        directories = ScatterZipOutputStream.pathBased(Files.createTempFile(outputTarget, "zip-builder-dirs", ""),
                compressionLevel);
    }

    @Override
    public void addManifest(Manifest manifest) throws IOException {
        this.manifest = manifest;
    }

    @Override
    public void addDirectory(String directory, String source) throws IOException {
        if (addedFiles.putIfAbsent(directory, source) != null) {
            return;
        }
        if (!directory.endsWith("/")) {
            directory += "/";
        }
        addDirectoryEntry(new ZipArchiveEntry(directory));
    }

    @Override
    public void addFile(Path origin, String target, String source) throws IOException {
        if (MANIFESTS.contains(target)) {
            return;
        }
        if (addedFiles.putIfAbsent(target, source) != null) {
            return;
        }
        addEntry(new ZipArchiveEntry(target), toInputStreamSupplier(origin));
    }

    @Override
    public void addFile(byte[] bytes, String target, String source) throws IOException {
        if (MANIFESTS.contains(target)) {
            return;
        }
        addEntry(new ZipArchiveEntry(target), toInputStreamSupplier(bytes));
        addedFiles.put(target, source);
    }

    @Override
    public void addFileIfNotExists(Path origin, String target, String source) throws IOException {
        if (MANIFESTS.contains(target)) {
            return;
        }
        if (addedFiles.containsKey(target)) {
            return;
        }

        addFile(origin, target, source);
    }

    @Override
    public void addFileIfNotExists(byte[] bytes, String target, String source) throws IOException {
        if (MANIFESTS.contains(target)) {
            return;
        }
        if (addedFiles.containsKey(target)) {
            return;
        }

        addFile(bytes, target, source);
    }

    @Override
    public void addFile(List<byte[]> bytes, String target, String source) throws IOException {
        if (MANIFESTS.contains(target)) {
            return;
        }
        addFile(joinWithNewlines(bytes), target, source);
    }

    @Override
    public boolean isMultiVersion() {
        for (String addedFile : addedFiles.keySet()) {
            for (String version : VERSIONS) {
                if (addedFile.startsWith(version)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public void makeMultiVersion() throws IOException {
        if (manifest == null) {
            manifest = new Manifest();
        }
        manifest.getMainAttributes().put(Attributes.Name.MULTI_RELEASE, "true");
    }

    private void addDirectoryEntry(final ZipArchiveEntry zipArchiveEntry) throws IOException {
        if (!zipArchiveEntry.isDirectory() || zipArchiveEntry.isUnixSymlink()) {
            return;
        }
        normalizeTimestampsAndPermissions(zipArchiveEntry);
        zipArchiveEntry.setMethod(compressionMethod);
        directories.addArchiveEntry(ZipArchiveEntryRequest.createZipArchiveEntryRequest(zipArchiveEntry, EMPTY_SUPPLIER));
    }

    private void addEntry(final ZipArchiveEntry zipArchiveEntry, final InputStreamSupplier streamSupplier) throws IOException {
        normalizeTimestampsAndPermissions(zipArchiveEntry);
        zipArchiveEntry.setMethod(compressionMethod);
        if (zipArchiveEntry.isDirectory() && !zipArchiveEntry.isUnixSymlink()) {
            directories.addArchiveEntry(ZipArchiveEntryRequest.createZipArchiveEntryRequest(zipArchiveEntry, streamSupplier));
        } else {
            scatterZipCreator.addArchiveEntry(zipArchiveEntry, streamSupplier);
        }
    }

    private static byte[] joinWithNewlines(List<byte[]> lines) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            for (byte[] line : lines) {
                out.write(line);
                out.write('\n');
            }
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Error joining byte arrays", e);
        }
    }

    private static InputStreamSupplier toInputStreamSupplier(byte[] data) {
        return () -> new ByteArrayInputStream(data);
    }

    private static InputStreamSupplier toInputStreamSupplier(Path path) {
        return () -> {
            try {
                return Files.newInputStream(path);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        };
    }

    private void normalizeTimestampsAndPermissions(ZipArchiveEntry archiveEntry) {
        if (entryTimestamp == null) {
            return;
        }

        archiveEntry.setTime(entryTimestamp.toMillis());
        archiveEntry.setCreationTime(entryTimestamp);
        archiveEntry.setLastModifiedTime(entryTimestamp);
        archiveEntry.setLastAccessTime(entryTimestamp);

        if (archiveEntry.isDirectory()) {
            archiveEntry.setUnixMode(DIR_UNIX_MODE);
        } else {
            archiveEntry.setUnixMode(FILE_UNIX_MODE);
        }
    }

    @Override
    public void close() {
        try (archive) {
            if (manifest != null) {
                // we add the manifest directly to the final archive to make sure it is the first element added
                ZipArchiveEntry metaInfArchiveEntry = new ZipArchiveEntry("META-INF/");
                normalizeTimestampsAndPermissions(metaInfArchiveEntry);
                archive.putArchiveEntry(metaInfArchiveEntry);
                archive.closeArchiveEntry();

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                manifest.write(baos);
                byte[] manifestBytes = baos.toByteArray();

                ZipArchiveEntry manifestEntry = new ZipArchiveEntry("META-INF/MANIFEST.MF");
                normalizeTimestampsAndPermissions(manifestEntry);
                manifestEntry.setSize(manifestBytes.length);
                archive.putArchiveEntry(manifestEntry);
                archive.write(manifestBytes);
                archive.closeArchiveEntry();
            }

            directories.writeTo(archive);
            directories.close();
            scatterZipCreator.writeTo(archive);
        } catch (IOException | InterruptedException | ExecutionException e) {
            throw new IllegalStateException("Unable to create archive: " + archivePath, e);
        }
        try {
            Files.deleteIfExists(tempDirectory);
        } catch (Exception e) {
            // noop, it's not a big deal to keep this directory around
        }
    }

    private static class DoNotShutdownDelegatingExecutorService implements ExecutorService {

        private final ExecutorService delegate;

        private DoNotShutdownDelegatingExecutorService(ExecutorService delegate) {
            this.delegate = delegate;
        }

        @Override
        public void shutdown() {
            // do nothing, that's the purpose of this wrapper
        }

        @Override
        public List<Runnable> shutdownNow() {
            // do nothing, that's the purpose of this wrapper
            return List.of();
        }

        @Override
        public boolean isShutdown() {
            return delegate.isShutdown();
        }

        @Override
        public boolean isTerminated() {
            return false;
        }

        @Override
        public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
            // do nothing, that's the purpose of this wrapper
            return false;
        }

        @Override
        public <T> Future<T> submit(Callable<T> task) {
            return delegate.submit(task);
        }

        @Override
        public <T> Future<T> submit(Runnable task, T result) {
            return delegate.submit(task, result);
        }

        @Override
        public Future<?> submit(Runnable task) {
            return delegate.submit(task);
        }

        @Override
        public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
            return delegate.invokeAll(tasks);
        }

        @Override
        public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
                throws InterruptedException {
            return delegate.invokeAll(tasks, timeout, unit);
        }

        @Override
        public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
            return delegate.invokeAny(tasks);
        }

        @Override
        public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
                throws InterruptedException, ExecutionException, TimeoutException {
            return delegate.invokeAny(tasks, timeout, unit);
        }

        @Override
        public void execute(Runnable command) {
            delegate.execute(command);
        }
    }
}
