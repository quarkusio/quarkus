package io.quarkus.hibernate.orm.runtime;

import java.net.URL;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import io.quarkus.fs.util.ZipUtils;

public class SchemaToolingUtil {
    private static final String SQL_LOAD_SCRIPT_UNZIPPED_DIR_PREFIX = "import-sql-unzip-";

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
}
