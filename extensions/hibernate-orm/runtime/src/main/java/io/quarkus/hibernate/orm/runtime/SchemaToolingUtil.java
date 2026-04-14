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

    public static PreparedImportScripts unzipZipFilesAndReplaceZips(String commaSeparatedFileNames) {
        List<String> unzippedFilesNames = new ArrayList<>();
        List<TempDirCleanup> cleanups = new ArrayList<>();
        if (commaSeparatedFileNames != null) {
            String[] fileNames = commaSeparatedFileNames.split(",");
            for (String fileName : fileNames) {
                if (fileName.endsWith(".zip")) {
                    try {
                        Path unzipDir = Files.createTempDirectory(SQL_LOAD_SCRIPT_UNZIPPED_DIR_PREFIX);
                        Thread hook = new Thread(
                                () -> recursiveDeleteQuietly(unzipDir),
                                "shutdown-hook-delete-" + unzipDir.getFileName());
                        Runtime.getRuntime().addShutdownHook(hook);
                        cleanups.add(new TempDirCleanup(unzipDir, hook));

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
            return new PreparedImportScripts(String.join(",", unzippedFilesNames), cleanups);
        } else {
            return new PreparedImportScripts(null, Collections.emptyList());
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

    private record TempDirCleanup(Path dir, Thread shutdownHook) {
    }

    public static final class PreparedImportScripts implements AutoCloseable {

        private final String rewrittenValue;
        private final List<TempDirCleanup> cleanups;

        private PreparedImportScripts(String rewrittenValue, List<TempDirCleanup> cleanups) {
            this.rewrittenValue = rewrittenValue;
            this.cleanups = cleanups;
        }

        public String getRewrittenValue() {
            return rewrittenValue;
        }

        @Override
        public void close() {
            for (TempDirCleanup cleanup : cleanups) {
                recursiveDeleteQuietly(cleanup.dir());
                try {
                    Runtime.getRuntime().removeShutdownHook(cleanup.shutdownHook());
                } catch (IllegalStateException e) {
                    // JVM is already shutting down – the hook will execute (or already has), nothing to do.
                }
            }
        }
    }
}
