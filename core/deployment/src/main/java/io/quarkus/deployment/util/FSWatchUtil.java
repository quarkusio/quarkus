package io.quarkus.deployment.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.apache.commons.io.FilenameUtils;
import org.jboss.logging.Logger;

public class FSWatchUtil {

    private static final Logger log = Logger.getLogger(FSWatchUtil.class);

    /**
     * in a loop, checks for modifications in the files
     * 
     * @param watchers list of {@link Watcher}s
     */
    public static void observe(Collection<Watcher> watchers,
            long intervalMs) throws InterruptedException {
        Map<Path, Long> lastModified = new HashMap<>();

        long lastCheck = 0;
        // we're assuming no changes between the compilation and first execution, so don't trigger the watcher on first run
        boolean firstRun = true;
        //noinspection InfiniteLoopStatement
        while (true) {
            for (Watcher watcher : watchers) {
                try {
                    List<Path> matchingPaths = Files.walk(watcher.rootPath)
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
                    log.warn("Failed checking for code gen source modifications", e);
                }
            }

            long toSleep = intervalMs - (System.currentTimeMillis() - lastCheck);
            if (toSleep > 0) {
                //noinspection BusyWait
                Thread.sleep(toSleep);
            }
            lastCheck = System.currentTimeMillis();
            firstRun = false;
        }
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
