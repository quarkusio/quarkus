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
import static java.nio.file.StandardOpenOption.APPEND;
import static java.nio.file.StandardOpenOption.CREATE;
import static java.util.Calendar.DATE;
import static java.util.Calendar.HOUR_OF_DAY;
import static java.util.Calendar.MINUTE;
import static java.util.Calendar.SECOND;
import static java.util.concurrent.atomic.AtomicIntegerFieldUpdater.newUpdater;

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

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
    private static final Logger LOG = Logger.getLogger(DefaultAccessLogReceiver.class);

    private static final String DEFAULT_LOG_SUFFIX = "log";

    private final Executor logWriteExecutor;

    private final Deque<String> pendingMessages;

    //0 = not running
    //1 = queued
    //2 = running
    @SuppressWarnings("unused")
    private volatile int state = 0;

    private static final AtomicIntegerFieldUpdater<DefaultAccessLogReceiver> STATE_UPDATER = newUpdater(
            DefaultAccessLogReceiver.class, "state");

    private long changeOverPoint;
    private String currentDateString;
    private boolean forceLogRotation;

    private final Path outputDirectory;
    private final Path defaultLogFile;

    private final String logBaseName;
    private final String logNameSuffix; // always starts with a '.' character

    private Writer writer = null;

    private volatile boolean closed = false;
    private boolean initialRun = true;
    private final boolean rotate;
    private final LogFileHeaderGenerator fileHeaderGenerator;

    public DefaultAccessLogReceiver(final Executor logWriteExecutor, final File outputDirectory, final String logBaseName,
            final String logNameSuffix, boolean rotate) {
        this(logWriteExecutor, outputDirectory.toPath(), logBaseName, logNameSuffix, rotate);
    }

    public DefaultAccessLogReceiver(final Executor logWriteExecutor, final Path outputDirectory, final String logBaseName,
            final String logNameSuffix, boolean rotate) {
        this(logWriteExecutor, outputDirectory, logBaseName, logNameSuffix, rotate, null);
    }

    private DefaultAccessLogReceiver(final Executor logWriteExecutor, final Path outputDirectory, final String logBaseName,
            final String logNameSuffix, boolean rotate, LogFileHeaderGenerator fileHeader) {
        this.logWriteExecutor = logWriteExecutor;
        this.outputDirectory = outputDirectory;
        this.logBaseName = logBaseName;
        this.rotate = rotate;
        this.fileHeaderGenerator = fileHeader;
        this.logNameSuffix = effectiveLogNameSuffix(logNameSuffix);
        this.pendingMessages = new ConcurrentLinkedDeque<>();
        this.defaultLogFile = outputDirectory.resolve(logBaseName + this.logNameSuffix);
        initChangeOverPoint(Calendar.getInstance());
        initCurrentDateString();
    }

    private static String effectiveLogNameSuffix(String logNameSuffix) {
        var result = (logNameSuffix != null) ? logNameSuffix : DEFAULT_LOG_SUFFIX;
        if (result.charAt(0) != '.') {
            return '.' + result;
        }
        return result;
    }

    private void initChangeOverPoint(Calendar calendar) {
        calendar.set(SECOND, 0);
        calendar.set(MINUTE, 0);
        calendar.set(HOUR_OF_DAY, 0);
        calendar.add(DATE, 1);
        changeOverPoint = calendar.getTimeInMillis();
    }

    private void initCurrentDateString() {
        // if there is an existing default LOG file, use the date last modified instead of the current date
        if (Files.exists(defaultLogFile)) {
            currentDateString = DATE_FORMAT.format(getCurrentDate());
        }
    }

    private Date getCurrentDate() {
        try {
            return new Date(Files.getLastModifiedTime(defaultLogFile).toMillis());
        } catch (IOException ignored) {
            return new Date();
        }
    }

    @Override
    public void logMessage(final String message) {
        this.pendingMessages.add(message);
        int state = STATE_UPDATER.get(this);
        if (state == 0) {
            if (STATE_UPDATER.compareAndSet(this, 0, 1)) {
                logWriteExecutor.execute(this);
            }
        }
    }

    /**
     * processes all queued log messages
     */
    @Override
    public void run() {
        if (!STATE_UPDATER.compareAndSet(this, 1, 2)) {
            return;
        }
        if (forceLogRotation) {
            doRotate();
        } else if (initialRun && Files.exists(defaultLogFile)) {
            //if there is an existing log file check if it should be rotated
            long lm = 0;
            try {
                lm = Files.getLastModifiedTime(defaultLogFile).toMillis();
            } catch (IOException e) {
                LOG.error("Error rotating access log", e);
            }
            Calendar c = Calendar.getInstance();
            c.setTimeInMillis(changeOverPoint);
            c.add(DATE, -1);
            if (lm <= c.getTimeInMillis()) {
                doRotate();
            }
        }
        initialRun = false;
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
            if (!messages.isEmpty()) {
                writeMessage(messages);
            }
        } finally {
            STATE_UPDATER.set(this, 0);
            //check to see if there is still more messages
            //if so then run this again
            if (!pendingMessages.isEmpty() || forceLogRotation) {
                if (STATE_UPDATER.compareAndSet(this, 0, 1)) {
                    logWriteExecutor.execute(this);
                }
            } else if (closed) {
                try {
                    if (writer != null) {
                        writer.flush();
                        writer.close();
                        writer = null;
                    }
                } catch (IOException e) {
                    LOG.error("Error writing access log", e);
                }
            }
        }
    }

    private void writeMessage(final List<String> messages) {
        if (System.currentTimeMillis() > changeOverPoint) {
            doRotate();
        }
        try {
            if (writer == null) {
                writer = Files.newBufferedWriter(defaultLogFile, UTF_8, APPEND, CREATE);
                if (Files.size(defaultLogFile) == 0 && fileHeaderGenerator != null) {
                    String header = fileHeaderGenerator.generateHeader();
                    if (header != null) {
                        writer.write(header);
                        writer.write("\n");
                        writer.flush();
                    }
                }
            }
            for (String message : messages) {
                writer.write(message);
                writer.write('\n');
            }
            writer.flush();
        } catch (IOException e) {
            LOG.error("Error writing access log", e);
        }
    }

    private void doRotate() {
        forceLogRotation = false;
        if (!rotate) {
            return;
        }
        try {
            if (writer != null) {
                writer.flush();
                writer.close();
                writer = null;
            }
            if (Files.exists(defaultLogFile)) {
                Path newFile = outputDirectory.resolve(logBaseName + currentDateString + logNameSuffix);
                int count = 0;
                while (Files.exists(newFile)) {
                    ++count;
                    newFile = outputDirectory.resolve(logBaseName + currentDateString + "-" + count + logNameSuffix);
                }
                Files.move(defaultLogFile, newFile);
            }
        } catch (IOException e) {
            LOG.error("Error rotating access log", e);
        } finally {
            initChangeOverPoint(Calendar.getInstance());
        }
    }

    /**
     * forces a log rotation. This rotation is performed in an async manner, you cannot rely on the rotation
     * being performed immediately after this method returns.
     */
    public void rotate() {
        forceLogRotation = true;
        if (STATE_UPDATER.compareAndSet(this, 0, 1)) {
            logWriteExecutor.execute(this);
        }
    }

    @Override
    public void close() throws IOException {
        closed = true;
        if (STATE_UPDATER.compareAndSet(this, 0, 1)) {
            logWriteExecutor.execute(this);
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Path outputDirectory;
        private boolean rotate;

        public Path getOutputDirectory() {
            return outputDirectory;
        }

        public Builder setOutputDirectory(Path outputDirectory) {
            this.outputDirectory = outputDirectory;
            return this;
        }

        public boolean isRotate() {
            return rotate;
        }

        public Builder setRotate(boolean rotate) {
            this.rotate = rotate;
            return this;
        }

        public DefaultAccessLogReceiver build() {
            return new DefaultAccessLogReceiver(null, outputDirectory, null, null, rotate,
                    null);
        }
    }
}
