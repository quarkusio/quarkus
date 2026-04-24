package io.quarkus.hibernate.orm.runtime;

import java.io.IOException;
import java.net.URL;
import java.nio.file.DirectoryStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import org.jboss.logging.Logger;

import io.quarkus.fs.util.ZipUtils;

public class SchemaToolingUtil {

    private static final Logger log = Logger.getLogger(SchemaToolingUtil.class);
    private static final String SQL_LOAD_SCRIPT_UNZIPPED_DIR_PREFIX = "import-sql-unzip-";
    private static final String SQL_LOAD_SCRIPT_SHUTDOWN_HOOK_NAME = "shutdown-hook-delete-import-sql-temp-dirs";

    public static PreparedImportScripts unzipZipFilesAndReplaceZips(String commaSeparatedFileNames) {
        List<String> unzippedFilesNames = new ArrayList<>();
        List<Path> unzipDirs = new ArrayList<>();
        RuntimeException failure = null;
        try {
            if (commaSeparatedFileNames != null) {
                String[] fileNames = commaSeparatedFileNames.split(",");
                for (String fileName : fileNames) {
                    if (fileName.endsWith(".zip")) {
                        try {
                            Path unzipDir = Files.createTempDirectory(SQL_LOAD_SCRIPT_UNZIPPED_DIR_PREFIX);
                            unzipDirs.add(unzipDir);

                            URL resource = Thread.currentThread()
                                    .getContextClassLoader()
                                    .getResource(fileName);
                            Path zipFile = Paths.get(resource.toURI());
                            ZipUtils.unzip(zipFile, unzipDir);
                            try (DirectoryStream<Path> paths = Files.newDirectoryStream(unzipDir)) {
                                for (Path path : paths) {
                                    unzippedFilesNames.add(path.toAbsolutePath().toString());
                                }
                            }
                        } catch (Exception e) {
                            throw new IllegalStateException(String.format(Locale.ROOT, "Error unzipping import file %s: %s",
                                    fileName, e.getMessage()), e);
                        }
                    } else {
                        unzippedFilesNames.add(fileName);
                    }
                }
                return new PreparedImportScripts(String.join(",", unzippedFilesNames), unzipDirs);
            } else {
                return new PreparedImportScripts(null, Collections.emptyList());
            }
        } catch (RuntimeException e) {
            failure = e;
            throw e;
        } finally {
            if (failure != null) {
                for (Path unzipDir : unzipDirs) {
                    recursiveDeleteQuietly(unzipDir);
                }
            }
        }
    }

    static void recursiveDeleteQuietly(Path dir) {
        try {
            Files.walkFileTree(dir, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Files.deleteIfExists(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path d, IOException exc) throws IOException {
                    Files.deleteIfExists(d);
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            log.debugf(e, "Failed to delete temporary import-script directory %s", dir);
        }
    }

    public static final class PreparedImportScripts implements AutoCloseable {

        private final String rewrittenValue;
        private final List<Path> unzipDirs;
        private final Thread shutdownHook;

        private PreparedImportScripts(String rewrittenValue, List<Path> unzipDirs) {
            this.rewrittenValue = rewrittenValue;
            this.unzipDirs = List.copyOf(unzipDirs);
            if (this.unzipDirs.isEmpty()) {
                this.shutdownHook = null;
            } else {
                this.shutdownHook = new Thread(
                        () -> this.unzipDirs.forEach(SchemaToolingUtil::recursiveDeleteQuietly),
                        SQL_LOAD_SCRIPT_SHUTDOWN_HOOK_NAME);
                Runtime.getRuntime().addShutdownHook(shutdownHook);
            }
        }

        public String getRewrittenValue() {
            return rewrittenValue;
        }

        @Override
        public String toString() {
            return String.valueOf(rewrittenValue);
        }

        @Override
        public void close() {
            unzipDirs.forEach(SchemaToolingUtil::recursiveDeleteQuietly);
            if (shutdownHook == null) {
                return;
            }
            try {
                Runtime.getRuntime().removeShutdownHook(shutdownHook);
            } catch (IllegalStateException e) {
                // JVM is already shutting down – the hook will execute (or already has), nothing to do.
            }
        }
    }
}
