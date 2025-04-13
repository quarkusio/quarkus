package io.quarkus.hibernate.orm.runtime;

import java.net.URL;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;

import org.jboss.logging.Logger;

import io.quarkus.fs.util.ZipUtils;

public class SchemaToolingUtil {
    private static final String COMMA = ",";
    private static final String ZIP_FILE_EXTENSION = ".zip";
    private static final String SQL_LOAD_SCRIPT_UNZIPPED_DIR_PREFIX = "import-sql-unzip-";

    public static String unzipZipFilesAndReplaceZips(String commaSeparatedFileNames) {
        List<String> unzippedFilesNames = new LinkedList<>();
        if (commaSeparatedFileNames != null) {
            String[] fileNames = commaSeparatedFileNames.split(COMMA);
            for (String fileName : fileNames) {
                if (fileName.endsWith(ZIP_FILE_EXTENSION)) {
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
                    } catch (Exception e) {
                        throw new IllegalStateException(String.format("Error unzipping import file %s: %s",
                          fileName, e.getMessage()), e);                    }
                } else {
                    unzippedFilesNames.add(fileName);
                }
            }
            return String.join(COMMA, unzippedFilesNames);
        } else {
            return null;
        }
    }
}
