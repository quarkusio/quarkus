package io.quarkus.deployment.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.apache.commons.io.FilenameUtils;
import org.jboss.logging.Logger;

public class FSWatchUtil {

    private static final Logger log = Logger.getLogger(FSWatchUtil.class);

    private final List<ExecutorService> executors = new ArrayList<>();

    private volatile boolean closed = false;

    /**
     * in a loop, checks for modifications in the files
     * 
     * @param watchers list of {@link Watcher}s
     */
    public void observe(Collection<Watcher> watchers,
            long intervalMs) {
        ThreadFactory tf = (Runnable r) -> new Thread(r, "FSWatchUtil");
        ExecutorService executorService = Executors.newSingleThreadExecutor(tf);
        executorService.execute(
                () -> doObserve(watchers, intervalMs));
        executors.add(executorService);
    }

    private void doObserve(Collection<Watcher> watchers, long intervalMs) {
        Map<Path, Long> lastModified = new HashMap<>();
        long lastCheck = 0;
        // we're assuming no changes between the compilation and first execution, so don't trigger the watcher on first run
        boolean firstRun = true;
        //noinspection InfiniteLoopStatement
        while (!closed) {
            for (Watcher watcher : watchers) {
                try {
                    Path rootPath = watcher.rootPath;
                    List<Path> matchingPaths = Files.walk(rootPath)
                            .filter(path -> FilenameUtils.getExtension(path.toString()).equals(watcher.fileExtension))
                            .collect(Collectors.toList());
                    List<Path> changedFiles = new ArrayList<>();
                    for (Path path : matchingPaths) {
                        long lastModifiedTime = Files.getLastModifiedTime(path).toMillis();
                        if (lastModifiedTime > lastModified.computeIfAbsent(path, whatever -> 0L)) {
                            changedFiles.add(path);
                            lastModified.put(path, lastModifiedTime);
                        }
                    }

                    if (!firstRun && !changedFiles.isEmpty()) {
                        watcher.action.accept(changedFiles);
                    }
                } catch (IOException e) {
                    log.debug("Failed checking for code gen source modifications", e);
                }
            }

            long toSleep = intervalMs - (System.currentTimeMillis() - lastCheck);
            if (toSleep > 0) {
                try {
                    //noinspection BusyWait
                    Thread.sleep(toSleep);
                } catch (InterruptedException e) {
                    log.debug("Watching for code gen interrupted");
                }
            }
            lastCheck = System.currentTimeMillis();
            firstRun = false;
        }
    }

    public void shutdown() {
        executors.forEach(ExecutorService::shutdown);
        executors.clear();
        closed = true;
    }

    public static class Watcher {
        private final Path rootPath;
        private final String fileExtension;
        private final Consumer<Collection<Path>> action;

        /**
         *
         * @param rootPath directory to check for changes
         * @param fileExtension file extensions to take into account
         * @param action action to trigger on discovered changes
         */
        public Watcher(Path rootPath, String fileExtension, Consumer<Collection<Path>> action) {
            this.rootPath = rootPath;
            this.fileExtension = fileExtension;
            this.action = action;
        }
    }
}
