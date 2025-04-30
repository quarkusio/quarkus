package io.quarkus.dev.testing;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
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

    /**
     *
     * @param watchedFilePaths
     * @deprecated Use {@link #setWatchedFilePaths(Map, List)} instead.
     */
    @Deprecated(forRemoval = true)
    public synchronized static void setWatchedFilePaths(Map<String, Boolean> watchedFilePaths) {
        TestWatchedFiles.watchedFilePaths = watchedFilePaths;
        if (watchedFilesListener != null) {
            watchedFilesListener.accept(watchedFilePaths, List.of());
        }
    }

    /**
     *
     * @param watchedFilesListener
     * @deprecated Use {@link #setWatchedFilesListener(BiConsumer)} instead.
     */
    @Deprecated(forRemoval = true)
    public synchronized static void setWatchedFilesListener(Consumer<Map<String, Boolean>> watchedFilesListener) {
        TestWatchedFiles.watchedFilesListener = new BiConsumer<Map<String, Boolean>, List<Entry<Predicate<String>, Boolean>>>() {

            @Override
            public void accept(Map<String, Boolean> files, List<Entry<Predicate<String>, Boolean>> predicates) {
                watchedFilesListener.accept(files);
            }
        };
        if (watchedFilePaths != null) {
            watchedFilesListener.accept(watchedFilePaths);
        }
    }

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
