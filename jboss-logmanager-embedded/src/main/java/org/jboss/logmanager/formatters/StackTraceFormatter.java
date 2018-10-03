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

package org.jboss.logmanager.formatters;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

/**
 * Formatter used to format the stack trace of an exception.
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
class StackTraceFormatter {
    private static final String CAUSED_BY_CAPTION = "Caused by: ";
    private static final String SUPPRESSED_CAPTION = "Suppressed: ";

    private final Set<Throwable> seen = Collections.newSetFromMap(new IdentityHashMap<Throwable, Boolean>());
    private final StringBuilder builder;
    private final int suppressedDepth;
    private int suppressedCount;

    private StackTraceFormatter(final StringBuilder builder, final int suppressedDepth) {
        this.builder = builder;
        this.suppressedDepth = suppressedDepth;
    }

    /**
     * Writes the stack trace into the builder.
     *
     * @param builder         the string builder ot append the stack trace to
     * @param t               the throwable to render
     * @param suppressedDepth the number of suppressed messages to include
     */
    static void renderStackTrace(final StringBuilder builder, final Throwable t, final int suppressedDepth) {
        new StackTraceFormatter(builder, suppressedDepth).renderStackTrace(t);
    }

    private void renderStackTrace(final Throwable t) {
        // Reset the suppression count
        suppressedCount = 0;
        // Write the exception message
        builder.append(": ").append(t);
        newLine();

        // Write the stack trace for this message
        final StackTraceElement[] stackTrace = t.getStackTrace();
        for (StackTraceElement element : stackTrace) {
            renderTrivial("", element);
        }

        // Write any suppressed messages, if required
        if (suppressedDepth != 0) {
            for (Throwable se : t.getSuppressed()) {
                if (suppressedDepth < 0 || suppressedDepth > suppressedCount++) {
                    renderStackTrace(stackTrace, se, SUPPRESSED_CAPTION, "\t");
                }
            }
        }

        // Print cause if there is one
        final Throwable ourCause = t.getCause();
        if (ourCause != null) {
            renderStackTrace(stackTrace, ourCause, CAUSED_BY_CAPTION, "");
        }
    }

    private void renderStackTrace(final StackTraceElement[] parentStack, final Throwable child, final String caption, final String prefix) {
        if (seen.contains(child)) {
            builder.append("\t[CIRCULAR REFERENCE:")
                    .append(child)
                    .append(']');
            newLine();
        } else {
            seen.add(child);
            // Find the unique frames suppressing duplicates
            final StackTraceElement[] causeStack = child.getStackTrace();
            int m = causeStack.length - 1;
            int n = parentStack.length - 1;
            while (m >= 0 && n >= 0 && causeStack[m].equals(parentStack[n])) {
                m--;
                n--;
            }
            final int framesInCommon = causeStack.length - 1 - m;

            // Print our stack trace
            builder.append(prefix)
                    .append(caption)
                    .append(child);
            newLine();
            for (int i = 0; i <= m; i++) {
                renderTrivial(prefix, causeStack[i]);
            }
            if (framesInCommon != 0) {
                builder.append(prefix)
                        .append("\t... ")
                        .append(framesInCommon)
                        .append(" more");
                newLine();
            }

            // Print suppressed exceptions, if any
            if (suppressedDepth != 0) {
                for (Throwable se : child.getSuppressed()) {
                    if (suppressedDepth < 0 || suppressedDepth > suppressedCount++) {
                        renderStackTrace(causeStack, se, SUPPRESSED_CAPTION, prefix + "\t");
                    }
                }
            }

            // Print cause, if any
            Throwable ourCause = child.getCause();
            if (ourCause != null) {
                renderStackTrace(causeStack, ourCause, CAUSED_BY_CAPTION, prefix);
            }
        }
    }

    private void renderTrivial(final String prefix, final StackTraceElement element) {
        builder.append(prefix)
                .append("\tat ")
                .append(element);
        newLine();
    }

    private void newLine() {
        builder.append(System.lineSeparator());
    }
}
