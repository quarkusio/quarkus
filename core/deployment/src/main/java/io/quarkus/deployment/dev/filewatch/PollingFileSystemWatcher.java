/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.quarkus.deployment.dev.filewatch;

import java.io.File;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.jboss.logging.Logger;

/**
 * Polling based file system watcher service, for use on operating systems that don't have
 * a useful watch service.
 *
 * @author Stuart Douglas
 */
public class PollingFileSystemWatcher implements FileSystemWatcher, Runnable {

    private static final Logger log = Logger.getLogger(PollingFileSystemWatcher.class);

    private static final AtomicInteger threadIdCounter = new AtomicInteger(0);
    public static final String THREAD_NAME = "xnio-polling-file-watcher";

    private final Map<File, PollHolder> files = Collections.synchronizedMap(new HashMap<File, PollHolder>());

    private final Thread watchThread;
    private final long pollInterval;

    private volatile boolean stopped = false;

    public PollingFileSystemWatcher(final String name, long pollInterval, final boolean daemon) {
        watchThread = new Thread(this, THREAD_NAME + "[" + name + "]-" + threadIdCounter);
        watchThread.setDaemon(daemon);
        watchThread.start();
        this.pollInterval = pollInterval;
    }

    @Override
    public void run() {
        while (!stopped) {
            try {
                doNotify();
                Thread.sleep(pollInterval);
            } catch (InterruptedException e) {
                //ignore
            }
        }
    }

    private void doNotify() {
        for (Map.Entry<File, PollHolder> entry : files.entrySet()) {
            Map<File, Long> result = doScan(entry.getKey());
            List<FileChangeEvent> currentDiff = doDiff(result, entry.getValue().currentFileState);
            if (!currentDiff.isEmpty()) {
                entry.getValue().currentFileState = result;
                for (FileChangeCallback callback : entry.getValue().callbacks) {
                    invokeCallback(callback, currentDiff);
                }
            }
        }
    }

    private List<FileChangeEvent> doDiff(Map<File, Long> newFileState, Map<File, Long> currentFileState) {
        final List<FileChangeEvent> results = new ArrayList<FileChangeEvent>();
        final Map<File, Long> currentCopy = new HashMap<File, Long>(currentFileState);
        for (Map.Entry<File, Long> newEntry : newFileState.entrySet()) {
            Long old = currentCopy.remove(newEntry.getKey());
            if (old == null) {
                results.add(new FileChangeEvent(newEntry.getKey(), FileChangeEvent.Type.ADDED));
            } else {
                if (!old.equals(newEntry.getValue()) && !newEntry.getKey().isDirectory()) {
                    //we don't add modified events for directories
                    //as we will be generating modified events for the files in the dir anyway
                    results.add(new FileChangeEvent(newEntry.getKey(), FileChangeEvent.Type.MODIFIED));
                }
            }
        }
        for (Map.Entry<File, Long> old : currentCopy.entrySet()) {
            results.add(new FileChangeEvent(old.getKey(), FileChangeEvent.Type.REMOVED));
        }
        return results;
    }

    @Override
    public synchronized void watchPath(File file, FileChangeCallback callback) {
        PollHolder holder = files.get(file);
        if (holder == null) {
            files.put(file, holder = new PollHolder(doScan(file)));
        }
        holder.callbacks.add(callback);
    }

    @Override
    public synchronized void unwatchPath(File file, final FileChangeCallback callback) {
        PollHolder holder = files.get(file);
        if (holder != null) {
            holder.callbacks.remove(callback);
            if (holder.callbacks.isEmpty()) {
                files.remove(file);
            }
        }
        files.remove(file);
    }

    @Override
    public void close() throws IOException {
        this.stopped = true;
        watchThread.interrupt();
    }

    private class PollHolder {
        Map<File, Long> currentFileState;
        final List<FileChangeCallback> callbacks = new ArrayList<FileChangeCallback>();

        private PollHolder(Map<File, Long> currentFileState) {
            this.currentFileState = currentFileState;
        }
    }

    static Map<File, Long> doScan(File file) {
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
            } else {
                results.put(next, next.lastModified());
            }
        }
        return results;
    }

    static void invokeCallback(FileChangeCallback callback, List<FileChangeEvent> results) {
        try {
            callback.handleChanges(results);
        } catch (Exception e) {
            log.error("Failed to invoke class change callback", e);
        }
    }

}
