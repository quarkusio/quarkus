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

package org.jboss.logmanager.formatters;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public class StackTraceFormatterTests {

    @Test
    public void compareSimpleStackTrace() {
        final RuntimeException e = new RuntimeException();
        final StringWriter writer = new StringWriter();
        e.printStackTrace(new PrintWriter(writer));

        final StringBuilder sb = new StringBuilder();
        StackTraceFormatter.renderStackTrace(sb, e, -1);

        Assert.assertEquals(writer.toString(), sanitize(sb.toString()));
    }

    @Test
    public void compareCauseStackTrace() {
        final RuntimeException e = new RuntimeException("Test Exception", new IllegalStateException("Cause"));

        final StringWriter writer = new StringWriter();
        e.printStackTrace(new PrintWriter(writer));

        final StringBuilder sb = new StringBuilder();
        StackTraceFormatter.renderStackTrace(sb, e, -1);

        Assert.assertEquals(writer.toString(), sanitize(sb.toString()));
    }

    @Test
    public void compareSuppressedAndCauseStackTrace() {
        final RuntimeException r1 = new RuntimeException("Exception 1");
        final RuntimeException r2 = new RuntimeException("Exception 2", r1);
        final RuntimeException r3 = new RuntimeException("Exception 3", r2);

        final RuntimeException cause = new RuntimeException("This is the cause", r1);
        cause.addSuppressed(r2);
        cause.addSuppressed(r3);

        final StringWriter writer = new StringWriter();
        cause.printStackTrace(new PrintWriter(writer));

        final StringBuilder sb = new StringBuilder();
        StackTraceFormatter.renderStackTrace(sb, cause, -1);

        Assert.assertEquals(writer.toString(), sanitize(sb.toString()));
    }

    @Test
    public void compareNestedSuppressedStackTrace() {
        final RuntimeException r1 = new RuntimeException("Exception 1");
        final RuntimeException r2 = new RuntimeException("Exception 2", r1);
        final RuntimeException r3 = new RuntimeException("Exception 3", r2);
        final IllegalStateException nested = new IllegalStateException("Nested 1");
        nested.addSuppressed(new RuntimeException("Nested 1a"));
        r3.addSuppressed(nested);
        r3.addSuppressed(new IllegalStateException("Nested 2"));

        final RuntimeException cause = new RuntimeException("This is the cause", r1);
        cause.addSuppressed(r2);
        cause.addSuppressed(r3);

        final StringWriter writer = new StringWriter();
        cause.printStackTrace(new PrintWriter(writer));

        final StringBuilder sb = new StringBuilder();
        StackTraceFormatter.renderStackTrace(sb, cause, -1);

        Assert.assertEquals(writer.toString(), sanitize(sb.toString()));
    }

    @Test
    public void compareMultiNestedSuppressedStackTrace() {
        final Throwable cause = createMultiNestedCause();

        final StringWriter writer = new StringWriter();
        cause.printStackTrace(new PrintWriter(writer));

        final StringBuilder sb = new StringBuilder();
        StackTraceFormatter.renderStackTrace(sb, cause, -1);

        Assert.assertEquals(writer.toString(), sanitize(sb.toString()));
    }

    @Test
    public void compareMultiNestedSuppressedAndNestedCauseStackTrace() {
        final Throwable rootCause = createMultiNestedCause();
        final RuntimeException cause = new RuntimeException("This is the parent", rootCause);

        final StringWriter writer = new StringWriter();
        cause.printStackTrace(new PrintWriter(writer));

        final StringBuilder sb = new StringBuilder();
        StackTraceFormatter.renderStackTrace(sb, cause, -1);

        Assert.assertEquals(writer.toString(), sanitize(sb.toString()));
    }

    @Test
    public void testNestedSuppressStackTraceDepth() {
        // Test that all messages exist
        testDepth(-1);
        // Now test up to 11 messages
        for (int i = 0; i < 12; i++) {
            testDepth(i);
        }
    }

    private void testDepth(final int depth) {
        final Throwable cause = createMultiNestedCause();

        final StringBuilder sb = new StringBuilder();
        StackTraceFormatter.renderStackTrace(sb, cause, depth);

        String msg = sb.toString();

        // Check the buffer for suppressed messages, should only have Suppressed 1
        checkMessage(msg, "Suppressed 1", depth > 0, depth);
        checkMessage(msg, "Nested 1", depth > 1, depth);
        checkMessage(msg, "Nested 1a", depth > 2, depth);
        checkMessage(msg, "Nested 1-2", depth > 3, depth);
        checkMessage(msg, "Suppressed 2", depth > 4, depth);
        checkMessage(msg, "Nested 2", depth > 5, depth);
        checkMessage(msg, "Nested 2a", depth > 6, depth);
        checkMessage(msg, "Nested 2-2", depth > 7, depth);
        checkMessage(msg, "Suppressed 3", depth > 8, depth);
        checkMessage(msg, "Nested 3", depth > 9, depth);
        checkMessage(msg, "Nested 3a", depth > 10, depth);
        checkMessage(msg, "Nested 3-2", depth > 11, depth);
    }

    private void checkMessage(final String msg, final String text, final boolean shouldExist, final int depth) {
        final boolean test = (shouldExist || depth < 0);
        Assert.assertEquals(String.format("Depth %d should %s contained \"%s\": %s", depth, (test ? "have" : "not have"), text, msg), msg.contains(text), test);
    }

    private Throwable createMultiNestedCause() {
        final RuntimeException suppressed1 = new RuntimeException("Suppressed 1");
        final IllegalStateException nested1 = new IllegalStateException("Nested 1");
        nested1.addSuppressed(new RuntimeException("Nested 1a"));
        suppressed1.addSuppressed(nested1);
        suppressed1.addSuppressed(new IllegalStateException("Nested 1-2"));


        final RuntimeException suppressed2 = new RuntimeException("Suppressed 2");
        final IllegalStateException nested2 = new IllegalStateException("Nested 2");
        nested2.addSuppressed(new RuntimeException("Nested 2a"));
        suppressed2.addSuppressed(nested2);
        suppressed2.addSuppressed(new IllegalStateException("Nested 2-2"));


        final RuntimeException suppressed3 = new RuntimeException("Suppressed 3");
        final IllegalStateException nested3 = new IllegalStateException("Nested 3");
        nested3.addSuppressed(new RuntimeException("Nested 3a"));
        suppressed3.addSuppressed(nested3);
        suppressed3.addSuppressed(new IllegalStateException("Nested 3-2"));

        final RuntimeException cause = new RuntimeException("This is the cause");
        cause.addSuppressed(suppressed1);
        cause.addSuppressed(suppressed2);
        cause.addSuppressed(suppressed3);
        return cause;
    }

    private static String sanitize(final String s) {
        if (s.startsWith(": ")) {
            return s.substring(2);
        }
        return s;
    }
}
