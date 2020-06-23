package io.quarkus.test.common;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;

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
        return getLogFileLocation(System.getProperty(LOG_FILE_PATH_PROPERTY, FileConfig.DEFAULT_LOG_FILE_NAME));
    }

    private static String getLogFileLocation(String logFileName) {
        if (Files.isDirectory(Paths.get("build"))) {
            return "build" + File.separator + logFileName;
        }
        return "target" + File.separator + logFileName;
    }
}
