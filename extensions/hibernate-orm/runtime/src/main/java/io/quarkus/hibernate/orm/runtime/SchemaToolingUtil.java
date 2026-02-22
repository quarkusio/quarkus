package io.quarkus.hibernate.orm.runtime;

import java.io.IOException;
import java.net.URL;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import io.quarkus.fs.util.ZipUtils;

public class SchemaToolingUtil {
    private static final String SQL_LOAD_SCRIPT_UNZIPPED_DIR_PREFIX = "import-sql-unzip-";
    private static final List<Path> tempFilePaths = new ArrayList<>();

    public static String unzipZipFilesAndReplaceZips(String commaSeparatedFileNames) {
        List<String> unzippedFilesNames = new ArrayList<>();
        if (commaSeparatedFileNames != null) {
            String[] fileNames = commaSeparatedFileNames.split(",");
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
                                tempFilePaths.add(path);
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
            return String.join(",", unzippedFilesNames);
        } else {
            return null;
        }
    }

    /**
     * Performs the deletion of SQL import temp directories and files.
     * The process is done by getting the list of the temp files that were unzipped and deleting them.
     * Temp files' parents (temp directories) are added to a Set and further deleted.
     **/
    public static void deleteSqlImportTempDirectories() {
        Set<Path> tempDirPaths = new HashSet<>();
        try {
            for (Path tempFilePath : tempFilePaths) {
                Files.deleteIfExists(tempFilePath);
                tempDirPaths.add(tempFilePath.getParent());
            }

            for (Path tempDirPath : tempDirPaths) {
                Files.deleteIfExists(tempDirPath);
            }
        } catch (IOException e) {
            throw new IllegalStateException(String.format(Locale.ROOT, "Error deleting unzipped temp files : %s",
                    e.getMessage()), e);
        }
    }
}
