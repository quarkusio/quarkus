package io.quarkus.test.common;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;

import io.quarkus.runtime.logging.FileConfig;

public class PropertyTestUtil {

    public static void setLogFileProperty() {
        System.setProperty("quarkus.log.file.path", getLogFileLocation());
    }

    public static void setLogFileProperty(String logFileName) {
        System.setProperty("quarkus.log.file.path", getLogFileLocation(logFileName));
    }

    public static String getLogFileLocation() {
        return getLogFileLocation(FileConfig.DEFAULT_LOG_FILE_NAME);
    }

    private static String getLogFileLocation(String logFileName) {
        if (Files.isDirectory(Paths.get("build"))) {
            return "build" + File.separator + logFileName;
        }
        return "target" + File.separator + logFileName;
    }
}
