package io.quarkus.gradle.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.gradle.api.Action;
import org.gradle.api.GradleException;
import org.gradle.api.file.CopySpec;
import org.gradle.api.file.DeleteSpec;
import org.gradle.api.file.FileCopyDetails;
import org.gradle.api.file.FileSystemOperations;
import org.gradle.api.services.BuildService;
import org.gradle.api.services.BuildServiceParameters;
import org.gradle.api.tasks.WorkResult;

/**
 * Gradle build service providing file system operations with timestamp preservation.
 */
public abstract class CustomFileSystemOperations
        implements BuildService<BuildServiceParameters.None> {

    @Inject
    protected abstract FileSystemOperations getFs();

    /**
     * Configuration for copy and sync operations.
     */
    public static class CopySyncConfiguration {

        List<Path> sources = new ArrayList<>();
        Path destination;
        List<String> includes = new ArrayList<>();
        List<String> excludes = new ArrayList<>();

        public CopySyncConfiguration into(Path destination) {
            this.destination = destination;
            return this;
        }

        public CopySyncConfiguration into(File destination) {
            this.destination = destination.toPath();
            return this;
        }

        public CopySyncConfiguration from(Path... sources) {
            this.sources.addAll(List.of(sources));
            return this;
        }

        public CopySyncConfiguration include(String... includes) {
            this.includes.addAll(List.of(includes));
            return this;
        }

        public CopySyncConfiguration exclude(String... excludes) {
            this.excludes.addAll(List.of(excludes));
            return this;
        }
    }

    public interface CopySyncConfigurationCustomizer {
        void customize(CopySyncConfiguration spec);
    }

    /**
     * File copy action that preserves timestamps and handles non-writeable destination files.
     */
    private static final class TimestampPreservingCopyAction implements Action<FileCopyDetails> {
        private final Path srcDir;
        private final Path destDir;
        private final List<CopiedFileMetadata> processedFiles;

        public TimestampPreservingCopyAction(Path srcDir, Path destDir, List<CopiedFileMetadata> processedFiles) {
            this.srcDir = srcDir;
            this.destDir = destDir;
            this.processedFiles = processedFiles;
        }

        @Override
        public void execute(FileCopyDetails details) {
            // Capture source file timestamp for later restoration.
            // Also delete any pre-existing non-writeable destination file to prevent copy failures.
            // This occurs with read-only files like 'app-cds.jsa' which should remain read-only.

            FileTime sourceMtime;
            Path sourceFile = srcDir.resolve(details.getSourcePath());
            try {
                sourceMtime = Files.getLastModifiedTime(sourceFile);
            } catch (IOException e) {
                throw new GradleException("Failed to get source file mtime for " + details.getSourcePath(), e);
            }
            Path destFile = destDir.resolve(details.getPath());
            if (Files.exists(destFile) && !Files.isWritable(destFile)) {
                deleteFileIfExists(destFile);
            }
            processedFiles.add(new CopiedFileMetadata(sourceFile, destFile, sourceMtime));
        }
    }

    private record CopiedFileMetadata(Path source, Path destination, FileTime sourceMtime) {
    }

    /**
     * Copies files while preserving their original timestamps.
     *
     * @param customizer configuration customizer
     * @return the result of the copy operation
     */
    public WorkResult copyPreservingTimestamps(CopySyncConfigurationCustomizer customizer) {
        CopySyncConfiguration config = new CopySyncConfiguration();
        customizer.customize(config);
        List<CopiedFileMetadata> copiedFilesMetadata = new ArrayList<>();
        var result = getFs().copy(copy -> {
            configureCopyOrSyncOperation(copy, config, copiedFilesMetadata);
        });

        restoreTimestamps(copiedFilesMetadata);

        return result;
    }

    /**
     * Synchronizes files while preserving their original timestamps.
     *
     * @param customizer configuration customizer
     * @return the result of the sync operation
     */
    public WorkResult syncPreservingTimestamps(CopySyncConfigurationCustomizer customizer) {
        CopySyncConfiguration config = new CopySyncConfiguration();
        customizer.customize(config);
        List<CopiedFileMetadata> copiedFilesMetadata = new ArrayList<>();
        var result = getFs().sync(sync -> {
            configureCopyOrSyncOperation(sync, config, copiedFilesMetadata);
        });

        restoreTimestamps(copiedFilesMetadata);

        return result;
    }

    /**
     * Deletes files and directories.
     *
     * @param action the delete action configuration
     * @return the result of the delete operation
     */
    public WorkResult delete(Action<? super DeleteSpec> action) {
        return getFs().delete(action);
    }

    /**
     * Deletes a file if it exists.
     *
     * @param file the file to delete
     */
    public static void deleteFileIfExists(Path file) {
        try {
            Files.deleteIfExists(file);
        } catch (IOException e) {
            throw new GradleException("Failed to delete file: " + file, e);
        }
    }

    private static void configureCopyOrSyncOperation(CopySpec copy, CopySyncConfiguration config,
            List<CopiedFileMetadata> copiedFilesMetadata) {
        // iterate over sources to be able to pass each source path to the TimestampPreservingCopyAction
        for (Path src : config.sources) {
            copy.from(src, spec -> spec.eachFile(
                    new TimestampPreservingCopyAction(
                            src,
                            config.destination,
                            copiedFilesMetadata)));
        }

        copy.into(config.destination);

        if (!config.includes.isEmpty()) {
            copy.include(config.includes);
        }

        if (!config.excludes.isEmpty()) {
            copy.exclude(config.excludes);
        }
    }

    private static void restoreTimestamps(List<CopiedFileMetadata> copiedFilesMetadata) {
        for (CopiedFileMetadata m : copiedFilesMetadata) {
            try {
                Files.setLastModifiedTime(m.destination(), m.sourceMtime());
            } catch (IOException e) {
                throw new GradleException("Failed to restore the timestamp of the file: " + m.destination(), e);
            }
        }
    }

}
