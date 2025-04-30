package io.quarkus.test.common;

import static io.quarkus.runtime.logging.LogRuntimeConfig.FileConfig;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

public class PropertyTestUtil {

    private static final String LOG_FILE_PATH_PROPERTY = "quarkus.log.file.path";

    public static void setLogFileProperty() {
        System.setProperty(LOG_FILE_PATH_PROPERTY, getLogFileLocation());
    }

    public static void setLogFileProperty(String logFileName) {
        System.setProperty(LOG_FILE_PATH_PROPERTY, getLogFileLocation(logFileName));
    }

    public static String getLogFileLocation() {
        String logFilePath = System.getProperty(LOG_FILE_PATH_PROPERTY);

        if (logFilePath != null) {
            return logFilePath;
        }

        return getLogFileLocation(FileConfig.DEFAULT_LOG_FILE_NAME);
    }

    public static Path getLogFilePath() {
        return Paths.get(getLogFileLocation());
    }

    private static String getLogFileLocation(String logFileName) {
        return String.join(File.separator, getLogFileLocationParts(logFileName));
    }

    private static List<String> getLogFileLocationParts(String logFileName) {
        if (Files.isDirectory(Paths.get("build"))) {
            return Arrays.asList("build", logFileName);
        }
        return Arrays.asList("target", logFileName);
    }
}
