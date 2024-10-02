package io.quarkus.deployment.dev.filesystem.watch;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.ClosedWatchServiceException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jboss.logging.Logger;

/**
 * File system watcher service based on JDK7 {@link WatchService}. Instantiating this class will create a new thread,
 * that will run until {@link #close()} is called.
 *
 * NOTE: this was copied from Xnio, it provides more functionality than we currently need.
 *
 */
public class WatchServiceFileSystemWatcher implements Runnable {

    private static final Logger log = Logger.getLogger(WatchServiceFileSystemWatcher.class);

    private static final AtomicInteger threadIdCounter = new AtomicInteger(0);

    private WatchService watchService;
    private final Map<Path, PathData> monitoredDirectories = Collections.synchronizedMap(new HashMap<>());
    private final Map<WatchKey, PathData> pathDataByKey = Collections
            .synchronizedMap(new IdentityHashMap<>());

    private volatile boolean stopped = false;
    private final Thread watchThread;

    public WatchServiceFileSystemWatcher(final String name, final boolean daemon) {
        try {
            watchService = FileSystems.getDefault().newWatchService();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        watchThread = new Thread(this, name + " - " + threadIdCounter);
        watchThread.setDaemon(daemon);
        watchThread.start();
    }

    @Override
    public void run() {
        while (!stopped) {
            try {
                final WatchKey key = watchService.take();
                if (key != null) {
                    try {
                        PathData pathData = pathDataByKey.get(key);
                        if (pathData != null) {
                            final List<FileChangeEvent> results = new ArrayList<>();
                            List<WatchEvent<?>> events = key.pollEvents();
                            final Set<Path> addedFiles = new HashSet<>();
                            final Set<Path> deletedFiles = new HashSet<>();
                            for (WatchEvent<?> event : events) {
                                Path eventPath = (Path) event.context();
                                Path targetFile = ((Path) key.watchable()).resolve(eventPath).toAbsolutePath();
                                FileChangeEvent.Type type;

                                if (event.kind() == StandardWatchEventKinds.ENTRY_CREATE) {
                                    type = FileChangeEvent.Type.ADDED;
                                    addedFiles.add(targetFile);
                                    if (Files.isDirectory(targetFile)) {
                                        try {
                                            addWatchedDirectory(pathData, targetFile);
                                        } catch (IOException e) {
                                            log.debugf(e, "Could not add watched directory %s", targetFile);
                                        }
                                    }
                                } else if (event.kind() == StandardWatchEventKinds.ENTRY_MODIFY) {
                                    type = FileChangeEvent.Type.MODIFIED;
                                } else if (event.kind() == StandardWatchEventKinds.ENTRY_DELETE) {
                                    type = FileChangeEvent.Type.REMOVED;
                                    deletedFiles.add(targetFile);
                                } else {
                                    continue;
                                }
                                results.add(new FileChangeEvent(targetFile, type));
                            }
                            key.pollEvents().clear();

                            //now we need to prune the results, to remove duplicates
                            //e.g. if the file is modified after creation we only want to
                            //show the create event
                            Iterator<FileChangeEvent> it = results.iterator();
                            while (it.hasNext()) {
                                FileChangeEvent event = it.next();

                                if (!pathData.isMonitored(event.getFile())) {
                                    it.remove();
                                    continue;
                                }

                                if (event.getType() == FileChangeEvent.Type.MODIFIED) {
                                    if (addedFiles.contains(event.getFile()) &&
                                            deletedFiles.contains(event.getFile())) {
                                        // XNIO-344
                                        // All file change events (ADDED, REMOVED and MODIFIED) occurred here.
                                        // This happens when an updated file is moved from the different
                                        // filesystems or the directory having different project quota on Linux.
                                        // ADDED and REMOVED events will be removed in the latter conditional branching.
                                        // So, this MODIFIED event needs to be kept for the file change notification.
                                        continue;
                                    }
                                    if (addedFiles.contains(event.getFile()) ||
                                            deletedFiles.contains(event.getFile())) {
                                        it.remove();
                                    }
                                } else if (event.getType() == FileChangeEvent.Type.ADDED) {
                                    if (deletedFiles.contains(event.getFile())) {
                                        it.remove();
                                    }
                                } else if (event.getType() == FileChangeEvent.Type.REMOVED) {
                                    if (addedFiles.contains(event.getFile())) {
                                        it.remove();
                                    }
                                }
                            }

                            if (!results.isEmpty()) {
                                for (FileChangeCallback callback : pathData.getCallbacks()) {
                                    invokeCallback(callback, results);
                                }
                            }
                        }
                    } finally {
                        //if the key is no longer valid remove it from the files list
                        if (!key.reset()) {
                            monitoredDirectories.remove(key.watchable());
                        }
                    }
                }
            } catch (InterruptedException e) {
                //ignore
            } catch (ClosedWatchServiceException cwse) {
                // the watcher service is closed, so no more waiting on events
                // @see https://developer.jboss.org/message/911519
                break;
            }
        }
    }

    public synchronized void watchDirectoryRecursively(Path directory, FileChangeCallback callback) {
        try {
            Path absoluteDirectory = directory.toAbsolutePath();
            PathData data = monitoredDirectories.get(absoluteDirectory);
            if (data == null) {
                Set<Path> allDirectories = doScan(absoluteDirectory).keySet();
                data = new PathData(absoluteDirectory, List.of());
                for (Path dir : allDirectories) {
                    addWatchedDirectory(data, dir);
                }
                monitoredDirectories.put(absoluteDirectory, data);
            }
            data.addCallback(callback);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @param directory a directory that will be watched
     * @param monitoredFiles list of monitored files relative to directory. An empty list will monitor all files.
     * @param callback callback called when a file is changed
     */
    public synchronized void watchFiles(Path directory, List<Path> monitoredFiles, FileChangeCallback callback) {
        try {
            Path absoluteDirectory = directory.toAbsolutePath();
            PathData data = monitoredDirectories.get(absoluteDirectory);
            if (data == null) {
                data = new PathData(absoluteDirectory, monitoredFiles);
                addWatchedDirectory(data, absoluteDirectory);
                monitoredDirectories.put(absoluteDirectory, data);
            }
            data.addCallback(callback);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void addWatchedDirectory(PathData data, Path dir) throws IOException {
        WatchKey key = dir.register(watchService, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);
        pathDataByKey.put(key, data);
        data.addWatchKey(key);
    }

    public synchronized void unwatchPath(Path directory, final FileChangeCallback callback) {
        PathData data = monitoredDirectories.get(directory);
        if (data != null) {
            data.removeCallback(callback);
            if (data.getCallbacks().isEmpty()) {
                monitoredDirectories.remove(directory);
                for (WatchKey key : data.getWatchKeys()) {
                    key.cancel();
                    pathDataByKey.remove(key);
                }

            }
        }
    }

    public void close() throws IOException {
        this.stopped = true;
        watchThread.interrupt();
        if (watchService != null) {
            watchService.close();
        }
    }

    private static Map<Path, Long> doScan(Path directory) {
        final Map<Path, Long> results = new HashMap<>();

        final Deque<Path> toScan = new ArrayDeque<>();
        toScan.add(directory);
        while (!toScan.isEmpty()) {
            Path next = toScan.pop();
            if (Files.isDirectory(next)) {
                try {
                    results.put(next, Files.getLastModifiedTime(directory).toMillis());
                    try (Stream<Path> list = Files.list(next)) {
                        list.forEach(p -> toScan.push(p.toAbsolutePath()));
                    }
                } catch (IOException e) {
                    throw new UncheckedIOException("Unable to scan: " + next, e);
                }
            }
        }
        return results;
    }

    private static void invokeCallback(FileChangeCallback callback, List<FileChangeEvent> results) {
        try {
            callback.handleChanges(results);
        } catch (Exception e) {
            log.error("Failed to invoke watch callback", e);
        }
    }

    private class PathData {

        private final Path path;
        private final List<FileChangeCallback> callbacks = new ArrayList<>();
        private final List<WatchKey> watchKeys = new ArrayList<>();
        private final List<Path> monitoredFiles;

        private PathData(Path path, List<Path> monitoredFiles) {
            this.path = path;
            this.monitoredFiles = monitoredFiles.stream().map(p -> path.resolve(p).toAbsolutePath())
                    .collect(Collectors.toList());
        }

        private void addWatchKey(WatchKey key) {
            this.watchKeys.add(key);
        }

        private void addCallback(FileChangeCallback callback) {
            this.callbacks.add(callback);
        }

        private void removeCallback(FileChangeCallback callback) {
            this.callbacks.remove(callback);
        }

        private List<FileChangeCallback> getCallbacks() {
            return callbacks;
        }

        private List<WatchKey> getWatchKeys() {
            return watchKeys;
        }

        private boolean isMonitored(Path file) {
            if (monitoredFiles.isEmpty()) {
                return true;
            }

            Path absolutePath = file.isAbsolute() ? file : file.toAbsolutePath();

            for (Path monitoredFile : monitoredFiles) {
                if (monitoredFile.equals(absolutePath)) {
                    return true;
                }
            }

            return false;
        }
    }

}
