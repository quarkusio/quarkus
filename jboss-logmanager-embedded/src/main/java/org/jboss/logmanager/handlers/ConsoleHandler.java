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

import java.io.Console;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Writer;

import java.util.logging.ErrorManager;
import java.util.logging.Formatter;

import org.jboss.logmanager.ExtLogRecord;
import org.jboss.logmanager.Level;
import org.jboss.logmanager.formatters.Formatters;

/**
 * A console handler which writes to {@code System.out} by default.
 */
public class ConsoleHandler extends OutputStreamHandler {
    private static final OutputStream out = System.out;
    private static final OutputStream err = System.err;

    /**
     * The target stream type.
     */
    public enum Target {

        /**
         * The target for {@link System#out}.
         */
        SYSTEM_OUT,
        /**
         * The target for {@link System#err}.
         */
        SYSTEM_ERR,
        /**
         * The target for {@link System#console()}.
         */
        CONSOLE,
    }

    private static final PrintWriter console;

    private final ErrorManager localErrorManager = new ErrorManager() {
        public void error(final String msg, final Exception ex, final int code) {
            final ExtLogRecord record = new ExtLogRecord(Level.ERROR, "Failed to publish log record (%s[%d]): %s", ExtLogRecord.FormatStyle.PRINTF, getClass().getName());
            final String codeStr;
            switch (code) {
                case ErrorManager.GENERIC_FAILURE: codeStr = "GENERIC_FAILURE"; break;
                case ErrorManager.WRITE_FAILURE:   codeStr = "WRITE_FAILURE";   break;
                case ErrorManager.FLUSH_FAILURE:   codeStr = "FLUSH_FAILURE";   break;
                case ErrorManager.CLOSE_FAILURE:   codeStr = "CLOSE_FAILURE";   break;
                case ErrorManager.OPEN_FAILURE:    codeStr = "OPEN_FAILURE";    break;
                case ErrorManager.FORMAT_FAILURE:  codeStr = "FORMAT_FAILURE";  break;
                default: codeStr = "Unknown Code"; break;
            }
            record.setParameters(new Object[] {
                codeStr,
                Integer.toString(code),
                msg,
            });
            record.setThrown(ex);
            publish(record);
        }
    };

    static {
        final Console con = System.console();
        console = con == null ? null : con.writer();
    }

    /**
     * Construct a new instance.
     */
    public ConsoleHandler() {
        this(Formatters.nullFormatter());
    }

    /**
     * Construct a new instance.
     *
     * @param formatter the formatter to use
     */
    public ConsoleHandler(final Formatter formatter) {
        this(console == null ? Target.SYSTEM_OUT : Target.CONSOLE, formatter);
    }

    /**
     * Construct a new instance.
     *
     * @param target the target to write to, or {@code null} to start with an uninitialized target
     */
    public ConsoleHandler(final Target target) {
        this(target, Formatters.nullFormatter());
    }

    /**
     * Construct a new instance.
     *
     * @param target the target to write to, or {@code null} to start with an uninitialized target
     * @param formatter the formatter to use
     */
    public ConsoleHandler(final Target target, final Formatter formatter) {
        super(formatter);
        switch (target) {
            case SYSTEM_OUT: setOutputStream(wrap(out)); break;
            case SYSTEM_ERR: setOutputStream(wrap(err)); break;
            case CONSOLE: setWriter(wrap(console)); break;
            default: throw new IllegalArgumentException();
        }
    }

    /**
     * Set the target for this console handler.
     *
     * @param target the target to write to, or {@code null} to clear the target
     */
    public void setTarget(Target target) {
        final Target t = (target == null ? console == null ? Target.SYSTEM_OUT : Target.CONSOLE : target);
        switch (t) {
            case SYSTEM_OUT: setOutputStream(wrap(out)); break;
            case SYSTEM_ERR: setOutputStream(wrap(err)); break;
            case CONSOLE: setWriter(wrap(console)); break;
            default: throw new IllegalArgumentException();
        }
    }

    public void setErrorManager(final ErrorManager em) {
        if (em == localErrorManager) {
            // ignore to avoid loops
            super.setErrorManager(new ErrorManager());
            return;
        }
        super.setErrorManager(em);
    }

    /**
     * Get the local error manager.  This is an error manager that will publish errors to this console handler.
     * The console handler itself should not use this error manager.
     *
     * @return the local error manager
     */
    public ErrorManager getLocalErrorManager() {
        return localErrorManager;
    }

    private static OutputStream wrap(final OutputStream outputStream) {
        return outputStream == null ?
                null :
                outputStream instanceof UncloseableOutputStream ?
                        outputStream :
                        new UncloseableOutputStream(outputStream);
    }

    private static Writer wrap(final Writer writer) {
        return writer == null ?
                null :
                writer instanceof UncloseableWriter ?
                        writer :
                        new UncloseableWriter(writer);
    }

    /** {@inheritDoc} */
    public void setOutputStream(final OutputStream outputStream) {
        super.setOutputStream(wrap(outputStream));
    }
}
