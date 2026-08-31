package io.quarkus.dev.testing;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

/**
 * provides a way for a test run to tell the external application about watched paths.
 *
 * This could be a test specific application.properties or import.sql for example
 */
public class TestWatchedFiles {

    private static volatile Map<String, Boolean> watchedFilePaths;
    private static volatile BiConsumer<Map<String, Boolean>, List<Entry<Predicate<String>, Boolean>>> watchedFilesListener;
    private static volatile List<Entry<Predicate<String>, Boolean>> watchedFilePredicates;

    public synchronized static void setWatchedFilePaths(Map<String, Boolean> watchedFilePaths,
            List<Entry<Predicate<String>, Boolean>> watchedFilePredicates) {
        TestWatchedFiles.watchedFilePaths = watchedFilePaths;
        TestWatchedFiles.watchedFilePredicates = watchedFilePredicates;
        if (watchedFilesListener != null) {
            watchedFilesListener.accept(watchedFilePaths, watchedFilePredicates);
        }
    }

    public synchronized static void setWatchedFilesListener(
            BiConsumer<Map<String, Boolean>, List<Entry<Predicate<String>, Boolean>>> watchedFilesListener) {
        TestWatchedFiles.watchedFilesListener = watchedFilesListener;
        if (watchedFilesListener != null && watchedFilePaths != null) {
            watchedFilesListener.accept(watchedFilePaths, watchedFilePredicates);
        }
    }
}
