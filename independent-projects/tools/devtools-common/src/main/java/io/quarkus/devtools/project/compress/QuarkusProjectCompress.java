package io.quarkus.devtools.project.compress;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import org.apache.commons.compress.archivers.zip.UnixStat;
import org.apache.commons.compress.archivers.zip.X5455_ExtendedTimestamp;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.compress.archivers.zip.ZipExtraField;
import org.apache.commons.compress.archivers.zip.ZipLong;

public final class QuarkusProjectCompress {

    private static final List<String> EXECUTABLES = List.of("gradlew", "mvnw");

    // Visible for testing
    static final int DIR_UNIX_MODE = UnixStat.DIR_FLAG | UnixStat.DEFAULT_DIR_PERM;

    private QuarkusProjectCompress() {
    }

    public static void zip(final Path quarkusProjectDirPath, final Path targetZipPath, final boolean includeProjectDirectory)
            throws IOException {
        zip(quarkusProjectDirPath, targetZipPath, includeProjectDirectory, null);
    }

    public static void zip(final Path quarkusProjectDirPath, final Path targetZipPath, final boolean includeProjectDirectory,
            final Long withSpecificFilesTime) throws IOException {
        try (final ZipArchiveOutputStream zaos = new ZipArchiveOutputStream(Files.newOutputStream(targetZipPath));
                Stream<Path> paths = Files.walk(quarkusProjectDirPath)) {
            paths
                    .filter((path) -> includeProjectDirectory || !quarkusProjectDirPath.equals(path))
                    .forEach((path) -> {
                        try {
                            String entryName = quarkusProjectDirPath.relativize(path).toString().replace('\\', '/');
                            if (includeProjectDirectory) {
                                entryName = quarkusProjectDirPath.getFileName()
                                        + (entryName.length() == 0 ? "" : "/" + entryName);
                            }
                            int unixMode;
                            if (Files.isDirectory(path)) {
                                entryName += "/";
                                unixMode = DIR_UNIX_MODE;
                            } else {
                                unixMode = getFileUnixMode(isExecutable(entryName));
                            }
                            final ZipArchiveEntry entry = new ZipArchiveEntry(path.toFile(), entryName);
                            entry.setUnixMode(unixMode);
                            if (withSpecificFilesTime != null) {
                                entry.setExtraFields(new ZipExtraField[] { getTimestamp(withSpecificFilesTime) });
                            }
                            zaos.putArchiveEntry(entry);
                            if (!Files.isDirectory(path)) {
                                Files.copy(path, zaos);
                            }
                            zaos.closeArchiveEntry();
                        } catch (final IOException e) {
                            throw new UncheckedIOException(e);
                        }
                    });
        }
    }

    // Visible for testing
    static int getFileUnixMode(boolean isExecutable) {
        return UnixStat.FILE_FLAG | (isExecutable ? 0755 : UnixStat.DEFAULT_FILE_PERM);
    }

    // Visible for testing
    static boolean isExecutable(final String entryName) {
        return EXECUTABLES.stream().anyMatch(entryName::contains);
    }

    private static X5455_ExtendedTimestamp getTimestamp(final long time) {
        final X5455_ExtendedTimestamp timestamp = new X5455_ExtendedTimestamp();
        final ZipLong zipTime = new ZipLong(time / 1000);
        timestamp.setCreateTime(zipTime);
        timestamp.setModifyTime(zipTime);
        timestamp.setAccessTime(zipTime);
        return timestamp;
    }
}
