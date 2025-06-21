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

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
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

    private static final String DOT = ".";

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

    public DefaultAccessLogReceiver(final Executor logWriteExecutor, final File outputDirectory, final String logBaseName) {
        this(logWriteExecutor, outputDirectory.toPath(), logBaseName, null);
    }

    public DefaultAccessLogReceiver(final Executor logWriteExecutor, final File outputDirectory, final String logBaseName,
            final String logNameSuffix) {
        this(logWriteExecutor, outputDirectory.toPath(), logBaseName, logNameSuffix, true);
    }

    public DefaultAccessLogReceiver(final Executor logWriteExecutor, final File outputDirectory, final String logBaseName,
            final String logNameSuffix, boolean rotate) {
        this(logWriteExecutor, outputDirectory.toPath(), logBaseName, logNameSuffix, rotate);
    }

    public DefaultAccessLogReceiver(final Executor logWriteExecutor, final Path outputDirectory, final String logBaseName) {
        this(logWriteExecutor, outputDirectory, logBaseName, null);
    }

    public DefaultAccessLogReceiver(final Executor logWriteExecutor, final Path outputDirectory, final String logBaseName,
            final String logNameSuffix) {
        this(logWriteExecutor, outputDirectory, logBaseName, logNameSuffix, true);
    }

    public DefaultAccessLogReceiver(final Executor logWriteExecutor, final Path outputDirectory, final String logBaseName,
            final String logNameSuffix, boolean rotate) {
        this(logWriteExecutor, outputDirectory, logBaseName, logNameSuffix, rotate, null);
    }

    private DefaultAccessLogReceiver(final Executor logWriteExecutor, final Path outputDirectory, final String logBaseName,
            final String logNameSuffix, boolean rotate, LogFileHeaderGenerator fileHeader) {
        this.logWriteExecutor = logWriteExecutor;
        this.outputDirectory = outputDirectory;
        this.logBaseName = effectiveLogBaseName(logBaseName);
        this.rotate = rotate;
        this.fileHeaderGenerator = fileHeader;
        this.logNameSuffix = effectiveLogNameSuffix(logNameSuffix);
        this.pendingMessages = new ConcurrentLinkedDeque<>();
        this.defaultLogFile = outputDirectory.resolve(this.logBaseName + this.logNameSuffix);
        calculateChangeOverPoint();
    }

    private String effectiveLogBaseName(String logBaseName) {
        if (logBaseName == null) {
            return "";
        }

        if (!logBaseName.endsWith(DOT)) {
            return logBaseName;
        }

        return logBaseName.substring(0, logBaseName.length() - 1);
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
        currentDateString = df.format(new Date());
        // if there is an existing default log file, use the date last modified instead of the current date
        if (Files.exists(defaultLogFile)) {
            try {
                currentDateString = df.format(new Date(Files.getLastModifiedTime(defaultLogFile).toMillis()));
            } catch (IOException e) {
                // ignore. use the current date if exception happens.
            }
        }
        changeOverPoint = calendar.getTimeInMillis();
    }

    @Override
    public void logMessage(final String message) {
        this.pendingMessages.add(message);
        int state = stateUpdater.get(this);
        if (state == 0) {
            if (stateUpdater.compareAndSet(this, 0, 1)) {
                logWriteExecutor.execute(this);
            }
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
        if (forceLogRotation) {
            doRotate();
        } else if (initialRun && Files.exists(defaultLogFile)) {
            //if there is an existing log file check if it should be rotated
            long lm = 0;
            try {
                lm = Files.getLastModifiedTime(defaultLogFile).toMillis();
            } catch (IOException e) {
                log.error("Error rotating access log", e);
            }
            Calendar c = Calendar.getInstance();
            c.setTimeInMillis(changeOverPoint);
            c.add(Calendar.DATE, -1);
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
            stateUpdater.set(this, 0);
            //check to see if there is still more messages
            //if so then run this again
            if (!pendingMessages.isEmpty() || forceLogRotation) {
                if (stateUpdater.compareAndSet(this, 0, 1)) {
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
                    log.error("Error writing access log", e);
                }
            }
        }
    }

    /**
     * For tests only. Blocks the current thread until all messages are written
     * Just does a busy wait.
     * <p/>
     * DO NOT USE THIS OUTSIDE OF A TEST
     */
    void awaitWrittenForTest() throws InterruptedException {
        while (!pendingMessages.isEmpty() || forceLogRotation) {
            Thread.sleep(10);
        }
        while (state != 0) {
            Thread.sleep(10);
        }
    }

    private void writeMessage(final List<String> messages) {
        if (System.currentTimeMillis() > changeOverPoint) {
            doRotate();
        }
        try {
            if (writer == null) {
                boolean created = !Files.exists(defaultLogFile);
                writer = Files.newBufferedWriter(defaultLogFile, StandardCharsets.UTF_8, StandardOpenOption.APPEND,
                        StandardOpenOption.CREATE);
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
            log.error("Error writing access log", e);
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
            if (!Files.exists(defaultLogFile)) {
                return;
            }
            Path newFile = outputDirectory.resolve(logBaseName + DOT + currentDateString + logNameSuffix);
            int count = 0;
            while (Files.exists(newFile)) {
                ++count;
                newFile = outputDirectory.resolve(logBaseName + DOT + currentDateString + "-" + count + logNameSuffix);
            }
            Files.move(defaultLogFile, newFile);
        } catch (IOException e) {
            log.error("Error rotating access log", e);
        } finally {
            calculateChangeOverPoint();
        }
    }

    /**
     * forces a log rotation. This rotation is performed in an async manner, you cannot rely on the rotation
     * being performed immediately after this method returns.
     */
    public void rotate() {
        forceLogRotation = true;
        if (stateUpdater.compareAndSet(this, 0, 1)) {
            logWriteExecutor.execute(this);
        }
    }

    @Override
    public void close() throws IOException {
        closed = true;
        if (stateUpdater.compareAndSet(this, 0, 1)) {
            logWriteExecutor.execute(this);
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Executor logWriteExecutor;
        private Path outputDirectory;
        private String logBaseName;
        private String logNameSuffix;
        private boolean rotate;
        private LogFileHeaderGenerator logFileHeaderGenerator;

        public Executor getLogWriteExecutor() {
            return logWriteExecutor;
        }

        public Builder setLogWriteExecutor(Executor logWriteExecutor) {
            this.logWriteExecutor = logWriteExecutor;
            return this;
        }

        public Path getOutputDirectory() {
            return outputDirectory;
        }

        public Builder setOutputDirectory(Path outputDirectory) {
            this.outputDirectory = outputDirectory;
            return this;
        }

        public String getLogBaseName() {
            return logBaseName;
        }

        public Builder setLogBaseName(String logBaseName) {
            this.logBaseName = logBaseName;
            return this;
        }

        public String getLogNameSuffix() {
            return logNameSuffix;
        }

        public Builder setLogNameSuffix(String logNameSuffix) {
            this.logNameSuffix = logNameSuffix;
            return this;
        }

        public boolean isRotate() {
            return rotate;
        }

        public Builder setRotate(boolean rotate) {
            this.rotate = rotate;
            return this;
        }

        public LogFileHeaderGenerator getLogFileHeaderGenerator() {
            return logFileHeaderGenerator;
        }

        public Builder setLogFileHeaderGenerator(LogFileHeaderGenerator logFileHeaderGenerator) {
            this.logFileHeaderGenerator = logFileHeaderGenerator;
            return this;
        }

        public DefaultAccessLogReceiver build() {
            return new DefaultAccessLogReceiver(logWriteExecutor, outputDirectory, logBaseName, logNameSuffix, rotate,
                    logFileHeaderGenerator);
        }
    }
}
