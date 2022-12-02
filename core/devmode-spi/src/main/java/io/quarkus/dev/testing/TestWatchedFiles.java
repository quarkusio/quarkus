package io.quarkus.dev.testing;

import java.util.Map;
import java.util.function.Consumer;

/**
 * provides a way for a test run to tell the external application about watched paths.
 *
 * This could be a test specific application.properties or import.sql for example
 */
public class TestWatchedFiles {

    private static volatile Map<String, Boolean> watchedFilePaths;
    private static volatile Consumer<Map<String, Boolean>> watchedFilesListener;

    public synchronized static void setWatchedFilePaths(Map<String, Boolean> watchedFilePaths) {
        TestWatchedFiles.watchedFilePaths = watchedFilePaths;
        if (watchedFilesListener != null) {
            watchedFilesListener.accept(watchedFilePaths);
        }
    }

    public synchronized static void setWatchedFilesListener(Consumer<Map<String, Boolean>> watchedFilesListener) {
        TestWatchedFiles.watchedFilesListener = watchedFilesListener;
        if (watchedFilePaths != null) {
            watchedFilesListener.accept(watchedFilePaths);
        }
    }
}
