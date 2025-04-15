/*
 * Copyright 2018 Red Hat, Inc.
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

package io.quarkus.bootstrap.logging;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.logging.ErrorManager;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Logger;

import org.jboss.logmanager.ExtHandler;
import org.jboss.logmanager.ExtLogRecord;
import org.jboss.logmanager.Level;
import org.jboss.logmanager.StandardOutputStreams;
import org.jboss.logmanager.formatters.PatternFormatter;

/**
 * A handler that queues messages until it's at least one child handler is {@linkplain #addHandler(Handler) added} or
 * {@linkplain #setHandlers(Handler[]) set}. If the children handlers are {@linkplain #clearHandlers() cleared} then
 * the handler is no longer considered activated and messages will once again be queued.
 */
@SuppressWarnings({ "unused", "WeakerAccess" })
public class QuarkusDelayedHandler extends ExtHandler {

    /**
     * This is a system property that can be used to help debug startup issues if TRACE and DEBUG logs are being
     * dropped due to the queue limit being exceeded.
     *
     * This is not a normal config property, and is unlikely to be needed under normal usage.
     */
    public static final String QUARKUS_LOG_MAX_STARTUP_RECORDS = "quarkus-log-max-startup-records";

    private final Deque<ExtLogRecord> logRecords = new ArrayDeque<>();
    private final List<Runnable> logCloseTasks = new ArrayList<>();
    private final Set<CategoryAndLevel> droppedRecords = Collections.synchronizedSet(new HashSet<>());

    private final int queueLimit;
    private volatile boolean buildTimeLoggingActivated = false;
    private volatile boolean activated = false;
    private volatile boolean callerCalculationRequired = false;
    //accessed under lock
    private int discardLevel = Integer.MIN_VALUE;
    private int lowestInQueue = Integer.MAX_VALUE;

    public QuarkusDelayedHandler() {
        this(Integer.getInteger(QUARKUS_LOG_MAX_STARTUP_RECORDS, 4000));
    }

    public QuarkusDelayedHandler(final int queueLimit) {
        this.queueLimit = queueLimit;
    }

    @Override
    protected void doPublish(final ExtLogRecord record) {
        // If activated just delegate
        if (activated) {
            publishToNestedHandlers(record);
            super.doPublish(record);
        } else {
            synchronized (this) {
                // Check one more time to see if we've been activated before queuing the messages
                if (activated || buildTimeLoggingActivated) {
                    publishToNestedHandlers(record);
                    super.doPublish(record);
                } else {
                    // drop everything below the discard level
                    // this is not ideal, but we can run out of memory otherwise
                    // this only happens if we end up with more than 4k log messages before activation
                    if (record.getLevel().intValue() <= discardLevel) {
                        droppedRecords.add(new CategoryAndLevel(record.getLoggerName(), record.getLevel()));
                        return;
                    }
                    // Determine whether the queue was overrun
                    if (logRecords.size() >= queueLimit) {
                        compactQueue();
                        if (logRecords.size() >= queueLimit) {
                            droppedRecords.add(new CategoryAndLevel(record.getLoggerName(), record.getLevel()));
                            //still too full, nothing we can do
                            return;
                        }
                    }
                    // Determine if we need to calculate the caller information before we queue the record
                    if (isCallerCalculationRequired()) {
                        // prepare record to move to another thread
                        record.copyAll();
                    } else {
                        // Disable the caller calculation since it's been determined we won't be using it
                        record.disableCallerCalculation();
                        // Copy the MDC over
                        record.copyMdc();
                    }
                    lowestInQueue = Integer.min(record.getLevel().intValue(), lowestInQueue);
                    logRecords.addLast(record);
                }
            }
        }
    }

    private void compactQueue() {
        if (lowestInQueue == Level.INFO.intValue()) {
            //we don't discard info and above
            return;
        }
        int newLowest = Integer.MAX_VALUE;
        Iterator<ExtLogRecord> it = logRecords.iterator();
        while (it.hasNext()) {
            ExtLogRecord rec = it.next();
            if (rec.getLevel().intValue() == lowestInQueue) {
                droppedRecords.add(new CategoryAndLevel(rec.getLoggerName(), rec.getLevel()));
                it.remove();
            } else {
                newLowest = Integer.min(rec.getLevel().intValue(), newLowest);
            }
        }
        discardLevel = lowestInQueue;
        lowestInQueue = newLowest;

    }

    @Override
    public final void close() throws SecurityException {
        synchronized (this) {
            if (!logRecords.isEmpty()) {
                Formatter formatter = getFormatter();
                Formatter defaultFormatter = new PatternFormatter("%d{yyyy-MM-dd HH:mm:ss,SSS} %-5p [%c] (%t) %s%e%n");
                boolean usingDefaultFormatter = false;
                if (formatter == null) {
                    usingDefaultFormatter = true;
                    formatter = defaultFormatter;
                }
                StandardOutputStreams.printError("The DelayedHandler was closed before any children handlers were " +
                        "configured. Messages will be written to stderr.");
                // Always attempt to drain the queue
                ExtLogRecord record;
                Exception formattingException = null;
                while ((record = logRecords.pollFirst()) != null) {
                    String value = null;
                    try {
                        value = formatter.format(record);
                    } catch (Exception e) {
                        if (!usingDefaultFormatter) {
                            // try once more using the default formatter
                            value = defaultFormatter.format(record);
                        } else {
                            formattingException = e;
                        }
                    }
                    if (value != null) {
                        StandardOutputStreams.printError(value);
                    }
                }
                if (formattingException != null) {
                    // let's just log the last formatting exception as there is not much more we can do
                    formattingException.printStackTrace();
                }
            }
        }
        activated = false;
        super.close();
    }

    /**
     * {@inheritDoc}
     * <p>
     * Note that once this is invoked the handler will be activated and the messages will no longer be queued. If more
     * than one child handler is required the {@link #setHandlers(Handler[])} should be used.
     * </p>
     *
     * @see #setHandlers(Handler[])
     */
    @Override
    public void addHandler(final Handler handler) throws SecurityException {
        super.addHandler(handler);
        activate();
    }

    /**
     * {@inheritDoc}
     * <p>
     * Note that once this is invoked the handler will be activated and the messages will no longer be queued.
     * </p>
     */
    @Override
    public Handler[] setHandlers(final Handler[] newHandlers) throws SecurityException {
        final Handler[] result = super.setHandlers(newHandlers);
        activate();
        return result;
    }

    public void addLoggingCloseTask(Runnable runnable) {
        logCloseTasks.add(runnable);
    }

    public synchronized Handler[] setBuildTimeHandlers(final Handler[] newHandlers) throws SecurityException {
        final Handler[] result = super.setHandlers(newHandlers);
        buildTimeLoggingActivated = true;
        ExtLogRecord record;
        while ((record = logRecords.pollFirst()) != null) {
            if (isEnabled() && isLoggable(record) && Logger.getLogger(record.getLoggerName()).isLoggable(record.getLevel())) {
                publishToNestedHandlers(record);
            }
        }
        return result;
    }

    public synchronized void buildTimeComplete() {
        buildTimeLoggingActivated = false;
        runCloseTasks();
    }

    /**
     * {@inheritDoc}
     * <p>
     * Note that if the last child handler is removed the handler will no longer be activated and the messages will
     * again be queued.
     * </p>
     *
     * @see #clearHandlers()
     */
    @Override
    public void removeHandler(final Handler handler) throws SecurityException {
        super.removeHandler(handler);
        activated = handlers.length != 0;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Note that once this is invoked the handler will no longer be activated and messages will again be queued.
     * </p>
     *
     * @see #removeHandler(Handler)
     */
    @Override
    public Handler[] clearHandlers() throws SecurityException {
        activated = false;
        runCloseTasks();
        return super.clearHandlers();
    }

    /**
     * {@inheritDoc}
     * <p>
     * This can be overridden to always require the caller calculation by setting the
     * {@link #setCallerCalculationRequired(boolean)} value to {@code true}.
     * </p>
     *
     * @see #setCallerCalculationRequired(boolean)
     */
    @Override
    public boolean isCallerCalculationRequired() {
        return callerCalculationRequired || super.isCallerCalculationRequired();
    }

    /**
     * Sets whether or not {@linkplain ExtLogRecord#copyAll() caller information} will be required when formatting
     * records.
     * <p>
     * If set to {@code true} the {@linkplain ExtLogRecord#copyAll() caller information} will be calculated for each
     * record that is placed in the queue. A value of {@code false} means the
     * {@link super#isCallerCalculationRequired()} will be used.
     * </p>
     * <p>
     * Note that the caller information is only attempted to be calculated when the handler has not been activated. Once
     * activated it's up to the {@linkplain #getHandlers() children handlers} to determine how the record is processed.
     * </p>
     *
     * @param callerCalculationRequired {@code true} if the {@linkplain ExtLogRecord#copyAll() caller information}
     *        should always be calculated before the record is being placed in the queue
     */
    public void setCallerCalculationRequired(final boolean callerCalculationRequired) {
        this.callerCalculationRequired = callerCalculationRequired;
    }

    /**
     * Indicates whether or not this handler has been activated.
     *
     * @return {@code true} if the handler has been activated, otherwise {@code false}
     */
    public final boolean isActivated() {
        return activated;
    }

    private synchronized void activate() {
        // Always attempt to drain the queue
        ExtLogRecord record;
        while ((record = logRecords.pollFirst()) != null) {
            if (isEnabled() && isLoggable(record) && Logger.getLogger(record.getLoggerName()).isLoggable(record.getLevel())) {
                publishToNestedHandlers(record);
            }
        }
        Map<String, List<java.util.logging.Level>> lostCategories = new TreeMap<>();
        for (CategoryAndLevel entry : droppedRecords) {
            if (Logger.getLogger(entry.category).isLoggable(entry.level)) {
                lostCategories.computeIfAbsent(entry.category, (key) -> new ArrayList<>())
                        .add(entry.level);
            }
        }
        if (lostCategories.size() > 0) {
            StringBuilder msg = new StringBuilder(
                    "The delayed handler's queue was overrun and log record(s) were lost (Did you forget to configure logging?): \n");
            for (Map.Entry<String, List<java.util.logging.Level>> entry : lostCategories.entrySet()) {
                msg.append(String.format("\t - %s: %s\n", entry.getKey(), entry.getValue()));
            }
            reportError(msg.toString(), null, ErrorManager.WRITE_FAILURE);
            lostCategories.clear();
        }
        droppedRecords.clear();
        activated = true;
    }

    private void runCloseTasks() {
        if (!logCloseTasks.isEmpty()) {
            for (Runnable i : logCloseTasks) {
                i.run();
            }
            logCloseTasks.clear();
        }
    }

    static final class CategoryAndLevel {
        final String category;
        final java.util.logging.Level level;

        CategoryAndLevel(String category, java.util.logging.Level level) {
            this.category = category;
            this.level = level;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            CategoryAndLevel that = (CategoryAndLevel) o;
            return Objects.equals(category, that.category) && Objects.equals(level, that.level);
        }

        @Override
        public int hashCode() {
            return Objects.hash(category, level);
        }
    }
}
