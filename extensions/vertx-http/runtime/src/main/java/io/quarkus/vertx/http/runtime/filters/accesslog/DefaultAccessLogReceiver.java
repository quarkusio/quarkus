/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package io.quarkus.vertx.http.runtime.filters.accesslog;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.Files.newBufferedWriter;
import static java.nio.file.StandardOpenOption.APPEND;
import static java.nio.file.StandardOpenOption.CREATE;
import static java.time.Instant.now;

import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;

import org.jboss.logging.Logger;

/**
 * Log Receiver that stores logs in a directory under the specified file name, and rotates them after
 * midnight.
 * <p/>
 * Web threads do not touch the log file, but simply queue messages to be written later by a worker thread.
 * A lightweight CAS based locking mechanism is used to ensure than only 1 thread is active writing messages at
 * any given time
 *
 * @author Stuart Douglas
 */
public class DefaultAccessLogReceiver implements AccessLogReceiver, Runnable, Closeable {

    private static final Logger log = Logger.getLogger(DefaultAccessLogReceiver.class);

    private static final String DEFAULT_LOG_SUFFIX = "log";

    private final Executor logWriteExecutor;

    private final Deque<String> pendingMessages;

    //0 = not running
    //1 = queued
    //2 = running
    @SuppressWarnings("unused")
    private volatile int state = 0;

    private static final AtomicIntegerFieldUpdater<DefaultAccessLogReceiver> stateUpdater = AtomicIntegerFieldUpdater
            .newUpdater(DefaultAccessLogReceiver.class, "state");

    private long changeOverPoint;
    private String currentDateString;

    private final Path outputDirectory;
    private final Path defaultLogFile;

    private final String logBaseName;
    /**
     * Always starts with a '.' character.
     *
     * @apiNote
     */
    private final String logNameSuffix;

    private final BufferedWriter writer;

    private volatile boolean closed = false;
    private final boolean rotate;

    public DefaultAccessLogReceiver(final Executor logWriteExecutor, final File outputDirectory, final String logBaseName,
            final String logNameSuffix, boolean rotate) {
        this.logWriteExecutor = logWriteExecutor;
        this.outputDirectory = outputDirectory.toPath();
        this.logBaseName = logBaseName;
        this.rotate = rotate;
        this.logNameSuffix = effectiveLogNameSuffix(logNameSuffix);
        this.pendingMessages = new ConcurrentLinkedDeque<>();
        this.defaultLogFile = outputDirectory.toPath().resolve(logBaseName + this.logNameSuffix);
        calculateChangeOverPoint();
        try {
            writer = newBufferedWriter(defaultLogFile, UTF_8, APPEND, CREATE);
        } catch (IOException e) {
            log.error(e);
            throw new RuntimeException(e);
        }
    }

    private static String effectiveLogNameSuffix(String logNameSuffix) {
        var result = (logNameSuffix != null) ? logNameSuffix : DEFAULT_LOG_SUFFIX;
        if (result.charAt(0) != '.') {
            return '.' + result;
        }
        return result;
    }

    private void calculateChangeOverPoint() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.add(Calendar.DATE, 1);
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        // if there is an existing default log file, use the date last modified instead of the current date
        if (Files.exists(defaultLogFile)) {
            try {
                currentDateString = df.format(new Date(Files.getLastModifiedTime(defaultLogFile).toMillis()));
            } catch (IOException ignored) {
                currentDateString = df.format(new Date());
            }
        }
        changeOverPoint = calendar.getTimeInMillis();
    }

    @Override
    public void logMessage(final String message) {
        pendingMessages.add(message);
        if (stateUpdater.get(this) == 0 && stateUpdater.compareAndSet(this, 0, 1)) {
            logWriteExecutor.execute(this);
        }
    }

    /**
     * processes all queued log messages
     */
    @Override
    public void run() {
        if (!stateUpdater.compareAndSet(this, 1, 2)) {
            return;
        }
        List<String> messages = new ArrayList<>();
        String msg;
        //only grab at most 1000 messages at a time
        for (int i = 0; i < 1000; ++i) {
            msg = pendingMessages.poll();
            if (msg == null) {
                break;
            }
            messages.add(msg);
        }
        try {
            writeMessage(messages);
        } finally {
            stateUpdater.set(this, 0);
            //check to see if there is still more messages
            //if so then run this again
            if (!pendingMessages.isEmpty() && stateUpdater.compareAndSet(this, 0, 1)) {
                logWriteExecutor.execute(this);
            } else if (closed) {
                try (Writer ignored = writer) {
                } catch (IOException e) {
                    log.error(e);
                }
            }
        }
    }

    private void writeMessage(final List<String> messages) {
        if (now().toEpochMilli() > changeOverPoint) {
            rotateToNextLogFile();
        }
        try {
            messages.forEach(this::writeMessage);
            writer.flush();
        } catch (IOException e) {
            log.error(e);
        }
    }

    private void writeMessage(String message) {
        try {
            writer.write(message);
            writer.newLine();
        } catch (IOException e) {
            log.error(e);
        }
    }

    private void rotateToNextLogFile() {
        if (!rotate) {
            return;
        }
        try {
            writer.flush();
            if (!Files.exists(defaultLogFile)) {
                return;
            }
            Files.move(defaultLogFile,
                    rotateLogFile(outputDirectory.resolve(logBaseName + currentDateString + logNameSuffix), 0));
        } catch (IOException e) {
            log.error(e);
        } finally {
            calculateChangeOverPoint();
        }
    }

    private Path rotateLogFile(Path newFile, int count) {
        if (Files.exists(newFile)) {
            return rotateLogFile(outputDirectory.resolve(logBaseName + currentDateString + "-" + ++count + logNameSuffix),
                    count);
        }
        return newFile;
    }

    @Override
    public void close() throws IOException {
        closed = true;
        if (stateUpdater.compareAndSet(this, 0, 1)) {
            logWriteExecutor.execute(this);
        }
    }
}
