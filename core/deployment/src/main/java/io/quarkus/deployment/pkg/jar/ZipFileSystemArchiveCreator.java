package io.quarkus.deployment.pkg.jar;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import org.jboss.logging.Logger;

import io.quarkus.fs.util.ZipUtils;

class ZipFileSystemArchiveCreator implements ArchiveCreator {

    private static final Logger LOG = Logger.getLogger(ZipFileSystemArchiveCreator.class);

    // we probably don't need the Windows entry but let's be safe
    private static final Set<String> MANIFESTS = Set.of("META-INF/MANIFEST.MF", "META-INF\\MANIFEST.MF");

    private final FileSystem zipFileSystem;

    private final Map<String, String> addedFiles = new HashMap<>();

    public ZipFileSystemArchiveCreator(Path archivePath, boolean compressed, Instant entryTimestamp) {
        try {
            if (compressed) {
                zipFileSystem = ZipUtils.createNewReproducibleZipFileSystem(archivePath, entryTimestamp);
            } else {
                zipFileSystem = ZipUtils.createNewReproducibleZipFileSystem(archivePath, Map.of("compressionMethod", "STORED"),
                        entryTimestamp);
            }
        } catch (IOException e) {
            throw new RuntimeException("Unable to initialize ZipFileSystem: " + archivePath, e);
        }
    }

    @Override
    public void addManifest(Manifest manifest) throws IOException {
        Path manifestInFsPath = zipFileSystem.getPath("META-INF", "MANIFEST.MF");
        handleParentDirectories(manifestInFsPath, CURRENT_APPLICATION);
        try (final OutputStream os = Files.newOutputStream(manifestInFsPath)) {
            manifest.write(os);
        }
    }

    @Override
    public void addDirectory(String directory, String source) throws IOException {
        if (addedFiles.putIfAbsent(directory, source) != null) {
            return;
        }
        final Path targetInFsPath = zipFileSystem.getPath(directory);
        try {
            handleParentDirectories(targetInFsPath, source);
            Files.createDirectory(targetInFsPath);
        } catch (FileAlreadyExistsException e) {
            if (!Files.isDirectory(targetInFsPath)) {
                throw e;
            }
        }
    }

    /**
     * First-write wins, subsequent additions of the same file will generate a warning.
     * <p>
     * Use {@link #addFileIfNotExists(Path, String, String)} if you expect a second write to be an expected behavior.
     * It has the same first-write wins behavior but won't generate a warning.
     */
    @Override
    public void addFile(Path origin, String target, String source) throws IOException {
        doAddFile(target, source,
                targetInFsPath -> Files.copy(origin, targetInFsPath, StandardCopyOption.REPLACE_EXISTING));
    }

    /**
     * First-write wins, subsequent additions of the same file will generate a warning.
     * <p>
     * Use {@link #addFileIfNotExists(byte[], String, String)} if you expect a second write to be an expected behavior.
     * It has the same first-write wins behavior but won't generate a warning.
     */
    @Override
    public void addFile(byte[] bytes, String target, String source) throws IOException {
        doAddFile(target, source, targetInFsPath -> Files.write(targetInFsPath, bytes));
    }

    /**
     * First-write wins, subsequent additions of the same file will generate a warning.
     */
    @Override
    public void addFile(List<byte[]> bytes, String target, String source) throws IOException {
        addFile(joinWithNewlines(bytes), target, source);
    }

    /**
     * First-write wins but don't generate a warning if an entry with this name already exists.
     */
    @Override
    public void addFileIfNotExists(Path origin, String target, String source) throws IOException {
        if (addedFiles.containsKey(target)) {
            return;
        }

        addFile(origin, target, source);
    }

    /**
     * First-write wins but don't generate a warning if an entry with this name already exists.
     */
    @Override
    public void addFileIfNotExists(byte[] bytes, String target, String source) throws IOException {
        if (addedFiles.containsKey(target)) {
            return;
        }

        addFile(bytes, target, source);
    }

    private void doAddFile(String target, String source, FileWriter writer) throws IOException {
        if (MANIFESTS.contains(target)) {
            return;
        }
        String existingSource = addedFiles.putIfAbsent(target, source);
        if (existingSource != null) {
            LOG.warn("Duplicate entry '" + target + "' — already added from '" + existingSource
                    + "', ignoring entry from '" + source + "'");
            return;
        }
        Path targetInFsPath = zipFileSystem.getPath(target);
        handleParentDirectories(targetInFsPath, source);
        writer.write(targetInFsPath);
    }

    @FunctionalInterface
    private interface FileWriter {
        void write(Path targetInFsPath) throws IOException;
    }

    @Override
    public boolean isMultiVersion() {
        return Files.isDirectory(zipFileSystem.getPath("META-INF", "versions"));
    }

    @Override
    public void makeMultiVersion() throws IOException {
        final Path manifestPath = zipFileSystem.getPath("META-INF", "MANIFEST.MF");
        final Manifest manifest = new Manifest();
        // read the existing one
        try (final InputStream is = Files.newInputStream(manifestPath)) {
            manifest.read(is);
        }
        manifest.getMainAttributes().put(Attributes.Name.MULTI_RELEASE, "true");
        try (final OutputStream os = Files.newOutputStream(manifestPath)) {
            manifest.write(os);
        }
    }

    private void handleParentDirectories(Path targetInFsPath, String source) throws IOException {
        if (targetInFsPath.getParent() != null) {
            Files.createDirectories(targetInFsPath.getParent());
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

    @Override
    public void close() {
        try {
            zipFileSystem.close();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
