/*
 * JBoss, Home of Professional Open Source.
 *
 * Copyright 2014 Red Hat, Inc., and individual contributors
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
import org.jboss.logmanager.ExtHandler;
import org.jboss.logmanager.ExtLogRecord;

import java.util.logging.ErrorManager;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.LogRecord;

/**
 * A queue handler which retains the last few messages logged.  The handler can be used as-is to remember recent
 * messages, or one or more handlers may be nested, which allows this handler to "replay" messages to the child
 * handler(s) upon request.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public class QueueHandler extends ExtHandler {
    private final Deque<ExtLogRecord> buffer = new ArrayDeque<ExtLogRecord>();
    private int limit = 10;

    /**
     * Construct a new instance with a default queue length.
     */
    public QueueHandler() {
    }

    /**
     * Construct a new instance.
     *
     * @param limit the queue length to use
     */
    public QueueHandler(final int limit) {
        if (limit < 1) {
            throw badQueueLength();
        }
        this.limit = limit;
    }

    public void publish(final ExtLogRecord record) {
        if (isEnabled() && record != null) {
            doPublish(record);
        }
    }

    public void publish(final LogRecord record) {
        if (isEnabled() && record != null) {
            doPublish(ExtLogRecord.wrap(record));
        }
    }

    protected void doPublish(final ExtLogRecord record) {
        synchronized (buffer) {
            if (isLoggable(record)) {
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
                if (buffer.size() == limit) { buffer.removeFirst(); }
                buffer.addLast(record);
            }
            for (Handler handler : getHandlers()) {
                handler.publish(record);
            }
        }
    }

    /**
     * Get the queue length limit.  This is the number of messages that will be saved before old messages roll off
     * of the queue.
     *
     * @return the queue length limit
     */
    public int getLimit() {
        synchronized (buffer) {
            return limit;
        }
    }

    /**
     * Set the queue length limit.  This is the number of messages that will be saved before old messages roll off
     * of the queue.
     *
     * @param limit the queue length limit
     */
    public void setLimit(final int limit) {
        if (limit < 1) {
            throw badQueueLength();
        }
        synchronized (buffer) {
            this.limit = limit;
        }
    }

    /**
     * Get a copy of the queue as it is at an exact moment in time.
     *
     * @return the copy of the queue
     */
    public ExtLogRecord[] getQueue() {
        synchronized (buffer) {
            return buffer.toArray(new ExtLogRecord[buffer.size()]);
        }
    }

    /**
     * Get a copy of the queue, rendering each record as a string.
     *
     * @return the copy of the queue rendered as strings
     */
    public String[] getQueueAsStrings() {
        final ExtLogRecord[] queue = getQueue();
        final int length = queue.length;
        final String[] strings = new String[length];
        final Formatter formatter = getFormatter();
        for (int i = 0, j = 0; j < length; j++) {
            final String formatted;
            try {
                formatted = formatter.format(queue[j]);
                if (formatted.length() > 0) {
                    strings[i++] = getFormatter().format(queue[j]);
                }
            } catch (Exception ex) {
                reportError("Formatting error", ex, ErrorManager.FORMAT_FAILURE);
            }
        }
        return strings;
    }

    /**
     * Replay the stored queue to the nested handlers.
     */
    public void replay() {
        final Handler[] handlers = getHandlers();
        if (handlers.length > 0) for (ExtLogRecord record : getQueue()) {
            for (Handler handler : handlers) {
                handler.publish(record);
            }
        }
    }

    private static IllegalArgumentException badQueueLength() {
        return new IllegalArgumentException("Queue length must be at least 1");
    }
}
