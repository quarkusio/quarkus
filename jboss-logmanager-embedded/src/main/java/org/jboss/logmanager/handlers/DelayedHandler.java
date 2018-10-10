/*
 * JBoss, Home of Professional Open Source.
 *
 * Copyright 2017 Red Hat, Inc., and individual contributors
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

package org.jboss.logmanager.handlers;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.logging.Formatter;
import java.util.logging.Handler;

import org.jboss.logmanager.ExtHandler;
import org.jboss.logmanager.ExtLogRecord;
import org.jboss.logmanager.StandardOutputStreams;
import org.jboss.logmanager.formatters.PatternFormatter;

/**
 * A handler that queues messages until it's at least one child handler is {@linkplain #addHandler(Handler) added} or
 * {@linkplain #setHandlers(Handler[]) set}. If the children handlers are {@linkplain #clearHandlers() cleared} then
 * the handler is no longer considered activated and messages will once again be queued.
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@SuppressWarnings({"unused", "WeakerAccess"})
public class DelayedHandler extends ExtHandler {

    private final Deque<ExtLogRecord> logRecords = new ArrayDeque<>();

    private volatile boolean activated = false;
    private volatile boolean callerCalculationRequired = false;

    @Override
    protected void doPublish(final ExtLogRecord record) {
        // If activated just delegate
        if (activated) {
            publishToChildren(record);
            super.doPublish(record);
        } else {
            synchronized (this) {
                // Check one more time to see if we've been activated before queuing the messages
                if (activated) {
                    publishToChildren(record);
                    super.doPublish(record);
                } else {
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
                    logRecords.addLast(record);
                }
            }
        }
    }

    @Override
    public final void close() throws SecurityException {
        synchronized (this) {
            if (!logRecords.isEmpty()) {
                Formatter formatter = getFormatter();
                if (formatter == null) {
                    formatter = new PatternFormatter("%d{yyyy-MM-dd HH:mm:ss,SSS} %-5p [%c] (%t) %s%e%n");
                }
                StandardOutputStreams.printError("The DelayedHandler was closed before any children handlers were " +
                        "configured. Messages will be written to stderr.");
                // Always attempt to drain the queue
                ExtLogRecord record;
                while ((record = logRecords.pollFirst()) != null) {
                    StandardOutputStreams.printError(formatter.format(record));
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
     *                                  should always be calculated before the record is being placed in the queue
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
            if (isEnabled() && isLoggable(record)) {
                publishToChildren(record);
            }
        }
        activated = true;
    }

    private void publishToChildren(final ExtLogRecord record) {
        for (Handler handler : handlers) {
            handler.publish(record);
        }
    }
}