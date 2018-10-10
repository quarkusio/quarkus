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

package org.jboss.logmanager;

import java.io.UnsupportedEncodingException;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import java.util.logging.ErrorManager;
import java.util.logging.Filter;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import org.jboss.logmanager.errormanager.OnlyOnceErrorManager;
import org.jboss.logmanager.handlers.FlushableCloseable;

/**
 * An extended logger handler.  Use this class as a base class for log handlers which require {@code ExtLogRecord}
 * instances.
 */
public abstract class ExtHandler extends Handler implements FlushableCloseable {

    private volatile boolean autoFlush = true;
    private volatile boolean enabled = true;
    private volatile boolean closeChildren;
    private static final ErrorManager DEFAULT_ERROR_MANAGER = new OnlyOnceErrorManager();

    /**
     * The sub-handlers for this handler.  May only be updated using the {@link #handlersUpdater} atomic updater.  The array
     * instance should not be modified (treat as immutable).
     */
    @SuppressWarnings("unused")
    protected volatile Handler[] handlers;

    /**
     * The atomic updater for the {@link #handlers} field.
     */
    protected static final AtomicArray<ExtHandler, Handler> handlersUpdater = AtomicArray.create(AtomicReferenceFieldUpdater.newUpdater(ExtHandler.class, Handler[].class, "handlers"), Handler.class);

    /**
     * Construct a new instance.
     */
    protected ExtHandler() {
        handlersUpdater.clear(this);
        closeChildren = true;
        super.setErrorManager(DEFAULT_ERROR_MANAGER);
    }

    /** {@inheritDoc} */
    public void publish(final LogRecord record) {
        if (enabled && record != null && isLoggable(record)) {
            doPublish(ExtLogRecord.wrap(record));
        }
    }

    /**
     * Publish an {@code ExtLogRecord}.
     * <p/>
     * The logging request was made initially to a Logger object, which initialized the LogRecord and forwarded it here.
     * <p/>
     * The {@code ExtHandler} is responsible for formatting the message, when and if necessary. The formatting should
     * include localization.
     *
     * @param record the log record to publish
     */
    public void publish(final ExtLogRecord record) {
        if (enabled && record != null && isLoggable(record)) {
            doPublish(record);
        }
    }

    /**
     * Do the actual work of publication; the record will have been filtered already.  The default implementation
     * does nothing except to flush if the {@code autoFlush} property is set to {@code true}; if this behavior is to be
     * preserved in a subclass then this method should be called after the record is physically written.
     *
     * @param record the log record to publish
     */
    protected void doPublish(final ExtLogRecord record) {
        if (autoFlush) flush();
    }

    /**
     * Add a sub-handler to this handler.  Some handler types do not utilize sub-handlers.
     *
     * @param handler the handler to add
     */
    public void addHandler(Handler handler) {
        if (handler == null) {
            throw new NullPointerException("handler is null");
        }
        handlersUpdater.add(this, handler);
    }

    /**
     * Remove a sub-handler from this handler.  Some handler types do not utilize sub-handlers.
     *
     * @param handler the handler to remove
     */
    public void removeHandler(Handler handler) throws SecurityException {
        if (handler == null) {
            return;
        }
        handlersUpdater.remove(this, handler, true);
    }

    /**
     * Get a copy of the sub-handlers array.  Since the returned value is a copy, it may be freely modified.
     *
     * @return a copy of the sub-handlers array
     */
    public Handler[] getHandlers() {
        final Handler[] handlers = this.handlers;
        return handlers.length > 0 ? handlers.clone() : handlers;
    }

    /**
     * A convenience method to atomically get and clear all sub-handlers.
     *
     * @return the old sub-handler array
     */
    public Handler[] clearHandlers() throws SecurityException {
        final Handler[] handlers = this.handlers;
        handlersUpdater.clear(this);
        return handlers.length > 0 ? handlers.clone() : handlers;
    }

    /**
     * A convenience method to atomically get and replace the sub-handler array.
     *
     * @param newHandlers the new sub-handlers
     * @return the old sub-handler array
     */
    public Handler[] setHandlers(final Handler[] newHandlers) throws SecurityException {
        if (newHandlers == null) {
            throw new IllegalArgumentException("newHandlers is null");
        }
        if (newHandlers.length == 0) {
            return clearHandlers();
        } else {
            final Handler[] handlers = handlersUpdater.getAndSet(this, newHandlers);
            return handlers.length > 0 ? handlers.clone() : handlers;
        }
    }

    /**
     * Determine if this handler will auto-flush.
     *
     * @return {@code true} if auto-flush is enabled
     */
    public boolean isAutoFlush() {
        return autoFlush;
    }

    /**
     * Change the autoflush setting for this handler.
     *
     * @param autoFlush {@code true} to automatically flush after each write; false otherwise
     */
    public void setAutoFlush(final boolean autoFlush) throws SecurityException {
        this.autoFlush = autoFlush;
        if (autoFlush) {
            flush();
        }
    }

    /**
     * Enables or disables the handler based on the value passed in.
     *
     * @param enabled {@code true} to enable the handler or {@code false} to disable the handler.
     */
    public final void setEnabled(final boolean enabled) throws SecurityException {
        this.enabled = enabled;
    }

    /**
     * Determine if the handler is enabled.
     *
     * @return {@code true} if the handler is enabled, otherwise {@code false}.
     */
    public final boolean isEnabled() {
        return enabled;
    }

    /**
     * Indicates whether or not children handlers should be closed when this handler is {@linkplain #close() closed}.
     *
     * @return {@code true} if the children handlers should be closed when this handler is closed, {@code false} if
     * children handlers should not be closed when this handler is closed
     */
    public boolean isCloseChildren() {
        return closeChildren;
    }

    /**
     * Sets whether or not children handlers should be closed when this handler is {@linkplain #close() closed}.
     *
     * @param closeChildren {@code true} if all children handlers should be closed when this handler is closed,
     *                      {@code false} if children handlers will <em>not</em> be closed when this handler
     *                      is closed
     */
    public void setCloseChildren(final boolean closeChildren) {
        this.closeChildren = closeChildren;
    }

    /**
     * Flush all child handlers.
     */
    @Override
    public void flush() {
        for (Handler handler : handlers) try {
            handler.flush();
        } catch (Exception ex) {
            reportError("Failed to flush child handler", ex, ErrorManager.FLUSH_FAILURE);
        } catch (Throwable ignored) {}
    }

    /**
     * Close all child handlers.
     */
    @Override
    public void close() throws SecurityException {
        if (closeChildren) {
            for (Handler handler : handlers)
                try {
                    handler.close();
                } catch (Exception ex) {
                    reportError("Failed to close child handler", ex, ErrorManager.CLOSE_FAILURE);
                } catch (Throwable ignored) {
                }
        }
    }

    @Override
    public void setFormatter(final Formatter newFormatter) throws SecurityException {
        super.setFormatter(newFormatter);
    }

    @Override
    public void setFilter(final Filter newFilter) throws SecurityException {
        super.setFilter(newFilter);
    }

    @Override
    public void setEncoding(final String encoding) throws SecurityException, UnsupportedEncodingException {
        super.setEncoding(encoding);
    }

    @Override
    public void setErrorManager(final ErrorManager em) {
        super.setErrorManager(em);
    }

    @Override
    public void setLevel(final Level newLevel) throws SecurityException {
        super.setLevel(newLevel);
    }

    /**
     * Indicates whether or not the {@linkplain #getFormatter() formatter} associated with this handler or a formatter
     * from a {@linkplain #getHandlers() child handler} requires the caller to be calculated.
     * <p>
     * Calculating the caller on a {@linkplain ExtLogRecord log record} can be an expensive operation. Some handlers
     * may be required to copy some data from the log record, but may not need the caller information. If the
     * {@linkplain #getFormatter() formatter} is a {@link ExtFormatter} the
     * {@link ExtFormatter#isCallerCalculationRequired()} is used to determine if calculation of the caller is
     * required.
     * </p>
     *
     * @return {@code true} if the caller should be calculated, otherwise {@code false} if it can be skipped
     *
     * @see LogRecord#getSourceClassName()
     * @see ExtLogRecord#getSourceFileName()
     * @see ExtLogRecord#getSourceLineNumber()
     * @see LogRecord#getSourceMethodName()
     */
    @SuppressWarnings("WeakerAccess")
    public boolean isCallerCalculationRequired() {
        Formatter formatter = getFormatter();
        if (formatterRequiresCallerCalculation(formatter)) {
            return true;
        } else for (Handler handler : getHandlers()) {
            if (handler instanceof ExtHandler) {
                if (((ExtHandler) handler).isCallerCalculationRequired()) {
                    return true;
                }
            } else {
                formatter = handler.getFormatter();
                if (formatterRequiresCallerCalculation(formatter)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean formatterRequiresCallerCalculation(final Formatter formatter) {
        return formatter != null && (!(formatter instanceof ExtFormatter) || ((ExtFormatter) formatter).isCallerCalculationRequired());
    }
}
