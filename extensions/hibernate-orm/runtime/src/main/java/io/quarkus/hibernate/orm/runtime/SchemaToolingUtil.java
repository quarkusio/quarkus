package io.quarkus.hibernate.orm.runtime;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import io.quarkus.fs.util.ZipUtils;
import io.smallrye.common.os.OS;

public class SchemaToolingUtil {
    private static final String SQL_LOAD_SCRIPT_UNZIPPED_DIR_PREFIX = "import-sql-unzip-";
    private static final String APP_DATA_DIRECTORY_NAME = "quarkus";
    private static final String TEMP_DIRECTORIES_DELETION_FILE_NAME = "delete.bin";

    /**
     * Clears the temporary directories (and the files contained on them) that where used to unzip schema ZIP files.
     *
     * @return the asynchronous task
     */
    public static CompletableFuture<Void> clearTempUnzippedSchemaDirectories() {
        return CompletableFuture.runAsync(() -> {
            if (Files.exists(Path.of(String.valueOf(getOSAppDataPath()), TEMP_DIRECTORIES_DELETION_FILE_NAME))) {
                try (InputStream inputStream = Files
                        .newInputStream(Path.of(String.valueOf(getOSAppDataPath()), TEMP_DIRECTORIES_DELETION_FILE_NAME))) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
                    String line;

                    while ((line = reader.readLine()) != null && !line.isBlank()) {
                        Path tempDirPath = Path.of(line);
                        // First delete the files inside the temporary directory
                        try (Stream<Path> pathStream = Files.walk(tempDirPath)) {
                            pathStream.map(Path::toFile).forEach(File::delete);
                        }
                        // Finally, delete the directory (now empty) itself
                        Files.deleteIfExists(tempDirPath);
                    }
                    Files.delete(Path.of(String.valueOf(getOSAppDataPath()), TEMP_DIRECTORIES_DELETION_FILE_NAME));
                } catch (IOException e) {
                    throw new IllegalStateException(
                            String.format(Locale.ROOT, "Error reading temporary unzip directories deletion file: %s",
                                    e.getMessage()),
                            e);
                }
            }
        });
    }

    public static String unzipZipFilesAndReplaceZips(String commaSeparatedFileNames) {
        List<String> unzippedFilesNames = new ArrayList<>();
        if (commaSeparatedFileNames != null) {
            String[] fileNames = commaSeparatedFileNames.split(",");
            StringBuilder zipFilePaths = new StringBuilder();

            for (String fileName : fileNames) {
                if (fileName.endsWith(".zip")) {
                    try {
                        Path unzipDir = Files.createTempDirectory(SQL_LOAD_SCRIPT_UNZIPPED_DIR_PREFIX);
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
                        zipFilePaths.append(unzipDir).append(System.lineSeparator());
                    } catch (Exception e) {
                        throw new IllegalStateException(String.format(Locale.ROOT, "Error unzipping import file %s: %s",
                                fileName, e.getMessage()), e);
                    }
                } else {
                    unzippedFilesNames.add(fileName);
                }
            }

            // if string builder is not empty (some temp directory were created)
            // write directories into the deletion file
            if (!zipFilePaths.isEmpty()) {
                CompletableFuture.runAsync(() -> {
                    try {
                        if (!Files.exists(Path.of(String.valueOf(getOSAppDataPath()), TEMP_DIRECTORIES_DELETION_FILE_NAME))) {
                            Files.createDirectories(getOSAppDataPath());
                            Files.createFile(Path.of(
                                    String.valueOf(getOSAppDataPath()), TEMP_DIRECTORIES_DELETION_FILE_NAME));
                        }
                        Files.writeString(Path.of(
                                String.valueOf(getOSAppDataPath()), TEMP_DIRECTORIES_DELETION_FILE_NAME),
                                zipFilePaths.toString());
                    } catch (IOException e) {
                        throw new IllegalStateException(
                                String.format(Locale.ROOT, "Failed to write to temporary unzip directories deletion file: %s",
                                        e.getMessage()),
                                e);
                    }
                });
            }
            return String.join(",", unzippedFilesNames);
        } else {
            return null;
        }
    }

    /**
     * Gets the application data directory depending on user's OS.
     *
     * @return the application data directory path
     */
    private static Path getOSAppDataPath() {
        OS currentOs = OS.current();
        String userHome = System.getProperty("user.home");
        Path osSpecificPath = Path.of("");

        switch (currentOs) {
            case WINDOWS -> osSpecificPath = Path.of("Appdata", "Local");
            case MAC -> osSpecificPath = Path.of("Library", "Application Support");
            case LINUX -> osSpecificPath = Path.of(".local", "share");
        }

        return Path.of(userHome, osSpecificPath.toString(), APP_DATA_DIRECTORY_NAME);
    }
}
