package io.quarkus.dev.testing;

import java.util.Map;

/**
 * provides a way for a test run to tell the external application about watched paths.
 *
 * This could be a test specific application.properties or import.sql for example
 */
public class TestWatchedFiles {

    private static volatile Map<String, Boolean> watchedFilePaths;

    public static Map<String, Boolean> retrieveWatchedFilePaths() {
        Map<String, Boolean> watchedFilePaths = TestWatchedFiles.watchedFilePaths;
        TestWatchedFiles.watchedFilePaths = null;
        return watchedFilePaths;
    }

    public static void setWatchedFilePaths(Map<String, Boolean> watchedFilePaths) {
        TestWatchedFiles.watchedFilePaths = watchedFilePaths;
    }
}
