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

package org.jboss.logmanager;

import java.io.PrintStream;

/**
 * Caches {@link System#out stdout} and {@link System#err stderr} early.
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@SuppressWarnings({"WeakerAccess", "unused"})
public class StandardOutputStreams {
    public static final PrintStream stdout = System.out;
    public static final PrintStream stderr = System.err;

    /**
     * Prints an error messages to {@link #stderr stderr}.
     *
     * @param msg the message to print
     */
    public static void printError(final String msg) {
        stderr.println(msg);
    }

    /**
     * Prints an error messages to {@link #stderr stderr}.
     *
     * @param format the {@link java.util.Formatter format}
     * @param args   the arguments for the format
     */
    public static void printError(final String format, final Object... args) {
        stderr.printf(format, args);
    }

    /**
     * Prints an error messages to {@link #stderr stderr}.
     *
     * @param cause the cause of the error, if not {@code null} the {@link Throwable#printStackTrace(PrintStream)}
     *              writes to {@link #stderr stderr}
     * @param msg   the message to print
     */
    public static void printError(final Throwable cause, final String msg) {
        stderr.println(msg);
        if (cause != null) {
            cause.printStackTrace(stderr);
        }
    }

    /**
     * Prints an error messages to {@link #stderr stderr}.
     *
     * @param cause  the cause of the error, if not {@code null} the {@link Throwable#printStackTrace(PrintStream)}
     *               writes to {@link #stderr stderr}
     * @param format the {@link java.util.Formatter format}
     * @param args   the arguments for the format
     */
    public static void printError(final Throwable cause, final String format, final Object... args) {
        stderr.printf(format, args);
        if (cause != null) {
            cause.printStackTrace(stderr);
        }
    }
}
