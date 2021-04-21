package io.quarkus.deployment.dev.filewatch;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;

import java.io.File;
import java.io.IOException;
import java.nio.file.ClosedWatchServiceException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
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
    private final Map<File, PathData> files = Collections.synchronizedMap(new HashMap<File, PathData>());
    private final Map<WatchKey, PathData> pathDataByKey = Collections
            .synchronizedMap(new IdentityHashMap<WatchKey, PathData>());

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
                            final List<FileChangeEvent> results = new ArrayList<FileChangeEvent>();
                            List<WatchEvent<?>> events = key.pollEvents();
                            final Set<File> addedFiles = new HashSet<File>();
                            final Set<File> deletedFiles = new HashSet<File>();
                            for (WatchEvent<?> event : events) {
                                Path eventPath = (Path) event.context();
                                File targetFile = ((Path) key.watchable()).resolve(eventPath).toFile();
                                FileChangeEvent.Type type;

                                if (event.kind() == StandardWatchEventKinds.ENTRY_CREATE) {
                                    type = FileChangeEvent.Type.ADDED;
                                    addedFiles.add(targetFile);
                                    if (targetFile.isDirectory()) {
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
                                for (FileChangeCallback callback : pathData.callbacks) {
                                    invokeCallback(callback, results);
                                }
                            }
                        }
                    } finally {
                        //if the key is no longer valid remove it from the files list
                        if (!key.reset()) {
                            files.remove(key.watchable());
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

    public synchronized void watchPath(File file, FileChangeCallback callback) {
        try {
            PathData data = files.get(file);
            if (data == null) {
                Set<File> allDirectories = doScan(file).keySet();
                Path path = Paths.get(file.toURI());
                data = new PathData(path);
                for (File dir : allDirectories) {
                    addWatchedDirectory(data, dir);
                }
                files.put(file, data);
            }
            data.callbacks.add(callback);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void addWatchedDirectory(PathData data, File dir) throws IOException {
        Path path = Paths.get(dir.toURI());
        WatchKey key = path.register(watchService, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);
        pathDataByKey.put(key, data);
        data.keys.add(key);
    }

    public synchronized void unwatchPath(File file, final FileChangeCallback callback) {
        PathData data = files.get(file);
        if (data != null) {
            data.callbacks.remove(callback);
            if (data.callbacks.isEmpty()) {
                files.remove(file);
                for (WatchKey key : data.keys) {
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

    private static Map<File, Long> doScan(File file) {
        final Map<File, Long> results = new HashMap<File, Long>();

        final Deque<File> toScan = new ArrayDeque<File>();
        toScan.add(file);
        while (!toScan.isEmpty()) {
            File next = toScan.pop();
            if (next.isDirectory()) {
                results.put(next, next.lastModified());
                File[] list = next.listFiles();
                if (list != null) {
                    for (File f : list) {
                        toScan.push(new File(f.getAbsolutePath()));
                    }
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
        final Path path;
        final List<FileChangeCallback> callbacks = new ArrayList<FileChangeCallback>();
        final List<WatchKey> keys = new ArrayList<WatchKey>();

        private PathData(Path path) {
            this.path = path;
        }
    }

}
