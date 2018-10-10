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

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import org.jboss.logmanager.ExtLogRecord;
import org.jboss.logmanager.ExtHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.Executors;

import java.util.logging.Handler;
import java.util.logging.ErrorManager;

/**
 * An asynchronous log handler which is used to write to a handler or group of handlers which are "slow" or introduce
 * some degree of latency.
 */
public class AsyncHandler extends ExtHandler {

    private final BlockingQueue<ExtLogRecord> recordQueue;
    private final int queueLength;
    private final Thread thread;
    private volatile OverflowAction overflowAction = OverflowAction.BLOCK;

    @SuppressWarnings("unused")
    private volatile int state;

    private static final AtomicIntegerFieldUpdater<AsyncHandler> stateUpdater = AtomicIntegerFieldUpdater.newUpdater(AsyncHandler.class, "state");

    private static final int DEFAULT_QUEUE_LENGTH = 512;

    /**
     * Construct a new instance.
     *
     * @param queueLength the queue length
     * @param threadFactory the thread factory to use to construct the handler thread
     */
    public AsyncHandler(final int queueLength, final ThreadFactory threadFactory) {
        recordQueue = new ArrayBlockingQueue<ExtLogRecord>(queueLength);
        thread = threadFactory.newThread(new AsyncTask());
        if (thread == null) {
            throw new IllegalArgumentException("Thread factory did not create a thread");
        }
        thread.setDaemon(true);
        this.queueLength = queueLength;
    }

    /**
     * Construct a new instance.
     *
     * @param threadFactory the thread factory to use to construct the handler thread
     */
    public AsyncHandler(final ThreadFactory threadFactory) {
        this(DEFAULT_QUEUE_LENGTH, threadFactory);
    }

    /**
     * Construct a new instance.
     *
     * @param queueLength the queue length
     */
    public AsyncHandler(final int queueLength) {
        this(queueLength, Executors.defaultThreadFactory());
    }

    /**
     * Construct a new instance.
     */
    public AsyncHandler() {
        this(DEFAULT_QUEUE_LENGTH);
    }

    /**
     * The full size of the queue.
     *
     * @return the full size of the queue.
     */
    public int getQueueLength() {
        return queueLength;
    }

    /**
     * Get the overflow action.
     *
     * @return the overflow action
     */
    public OverflowAction getOverflowAction() {
        return overflowAction;
    }

    /**
     * Set the overflow action.
     *
     * @param overflowAction the overflow action
     */
    public void setOverflowAction(final OverflowAction overflowAction) {
        if (overflowAction == null) {
            throw new NullPointerException("overflowAction is null");
        }
        this.overflowAction = overflowAction;
    }

    /** {@inheritDoc} */
    protected void doPublish(final ExtLogRecord record) {
        switch (state) {
            case 0: {
                if (stateUpdater.compareAndSet(this, 0, 1)) {
                    thread.start();
                }
            }
            case 1: {
                break;
            }
            default: {
                return;
            }
        }
        final BlockingQueue<ExtLogRecord> recordQueue = this.recordQueue;
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
        if (overflowAction == OverflowAction.DISCARD) {
            recordQueue.offer(record);
        } else {
            try {
                recordQueue.put(record);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    /** {@inheritDoc} */
    public void close() throws SecurityException {
        if (stateUpdater.getAndSet(this, 2) != 2) {
            thread.interrupt();
            super.close();
        }
    }

    private final class AsyncTask implements Runnable {
        public void run() {
            final BlockingQueue<ExtLogRecord> recordQueue = AsyncHandler.this.recordQueue;
            final Handler[] handlers = AsyncHandler.this.handlers;

            boolean intr = false;
            try {
                for (;;) {
                    ExtLogRecord rec = null;
                    try {
                        if (state == 2) {
                            rec = recordQueue.poll();
                            if (rec == null) {
                                return;
                            }
                        } else {
                            // auto-flush will flush on an empty queue
                            if (isAutoFlush()) {
                                rec = recordQueue.poll();
                                if (rec == null) {
                                    // flush all handlers
                                    flush();
                                    rec = recordQueue.take();
                                }
                            } else {
                                rec = recordQueue.take();
                            }
                        }
                    } catch (InterruptedException e) {
                        intr = true;
                        continue;
                    }
                    for (Handler handler : handlers) try {
                        if (handler != null) {
                            handler.publish(rec);
                        }
                    } catch (Exception e) {
                        final ErrorManager errorManager = getErrorManager();
                        if (errorManager != null) {
                            try {
                                errorManager.error("Publication error", e, ErrorManager.WRITE_FAILURE);
                            } catch (Throwable t) {
                            }
                        }
                    } catch (Throwable t) {
                        // ignore :-/
                    }
                }
            } finally {
                if (intr) {
                    Thread.currentThread().interrupt();
                }
                clearHandlers();
            }
        }
    }

    public enum OverflowAction {
        BLOCK,
        DISCARD,
    }
}
