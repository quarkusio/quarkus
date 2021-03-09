package io.quarkus.test.common;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import io.quarkus.runtime.logging.FileConfig;

public class PropertyTestUtil {

    private static final String LOG_FILE_PATH_PROPERTY = "quarkus.log.file.path";

    public static void setLogFileProperty() {
        System.setProperty(LOG_FILE_PATH_PROPERTY, getLogFileLocation());
    }

    public static void setLogFileProperty(String logFileName) {
        System.setProperty(LOG_FILE_PATH_PROPERTY, getLogFileLocation(logFileName));
    }

    public static String getLogFileLocation() {
        return getLogFileLocation(getLogFinalName());
    }

    private static String getLogFinalName() {
        return System.getProperty(LOG_FILE_PATH_PROPERTY, FileConfig.DEFAULT_LOG_FILE_NAME);
    }

    public static Path getLogFilePath() {
        List<String> logFileLocationParts = getLogFileLocationParts(getLogFinalName());
        if (logFileLocationParts.isEmpty()) {
            throw new IllegalStateException("Unable to determine log file path");
        } else if (logFileLocationParts.size() == 1) {
            return Paths.get(logFileLocationParts.get(0));
        } else {
            return Paths.get(logFileLocationParts.get(0),
                    logFileLocationParts.subList(1, logFileLocationParts.size()).toArray(new String[0]));
        }
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
