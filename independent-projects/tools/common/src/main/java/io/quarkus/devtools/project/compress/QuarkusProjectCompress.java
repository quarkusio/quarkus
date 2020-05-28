package io.quarkus.devtools.project.compress;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import io.quarkus.devtools.project.QuarkusProject;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.apache.commons.compress.archivers.zip.UnixStat;
import org.apache.commons.compress.archivers.zip.X5455_ExtendedTimestamp;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.compress.archivers.zip.ZipExtraField;
import org.apache.commons.compress.archivers.zip.ZipLong;

public final class QuarkusProjectCompress {

    private static final List<String> EXECUTABLES = ImmutableList.of(
            "gradlew",
            "gradlew.bat",
            "mvnw",
            "mvnw.bat");

    @VisibleForTesting
    static final int DIR_UNIX_MODE = UnixStat.DIR_FLAG | UnixStat.DEFAULT_DIR_PERM;

    private QuarkusProjectCompress() {
    }

    public static void zip(final QuarkusProject quarkusProject, final Path targetZipPath, final boolean includeProjectFolder)
            throws IOException {
        zip(quarkusProject, targetZipPath, includeProjectFolder, null);
    }

    public static void zip(final QuarkusProject quarkusProject, final Path targetZipPath, final boolean includeProjectFolder,
            final Long withSpecificFilesTime) throws IOException {
        try (final ZipArchiveOutputStream zaos = new ZipArchiveOutputStream(Files.newOutputStream(targetZipPath))) {
            final Path projectFolderPath = quarkusProject.getProjectFolderPath();
            Files.walk(projectFolderPath)
                    .filter((path) -> includeProjectFolder || !projectFolderPath.equals(path))
                    .forEach((path) -> {
                        try {
                            String entryName = projectFolderPath.relativize(path).toString().replace('\\', '/');
                            if (includeProjectFolder) {
                                entryName = projectFolderPath.getFileName() + "/" + entryName;
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

    @VisibleForTesting
    static int getFileUnixMode(boolean isExecutable) {
        return UnixStat.FILE_FLAG | (isExecutable ? 0755 : UnixStat.DEFAULT_FILE_PERM);
    }

    @VisibleForTesting
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
