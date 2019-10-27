package io.quarkus.runtime.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOError;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 *
 */
public class ExceptionUtilTest {

    /**
     * Tests the {@link ExceptionUtil#rootCauseFirstStackTrace(Throwable)} method
     *
     * @throws Exception
     */
    @Test
    public void testReversed() throws Exception {
        final Throwable ex = generateException();
        final String rootCauseFirst = ExceptionUtil.rootCauseFirstStackTrace(ex);
        assertNotNull(rootCauseFirst, "Stacktrace was null");
        assertTrue(rootCauseFirst.contains("Resulted in:"),
                "Stacktrace doesn't contain the \"Resulted in:\" string");
        assertFalse(rootCauseFirst.contains("Caused by:"), "Stacktrace contains the \"Caused by:\" string");
        final String[] lines = rootCauseFirst.split("\n");
        final String firstLine = lines[0];
        assertTrue(firstLine.startsWith(NumberFormatException.class.getName() + ": For input string: \"23.23232\""),
                "Unexpected root cause");
        final List<String> expectedResultedIns = new ArrayList<>();
        expectedResultedIns.add(IllegalArgumentException.class.getName() + ": Incorrect param");
        expectedResultedIns.add(IOException.class.getName() + ": Request processing failed");
        expectedResultedIns.add(IOError.class.getName());
        expectedResultedIns.add(RuntimeException.class.getName() + ": Unexpected exception");
        for (final String line : lines) {
            if (!line.startsWith("Resulted in:")) {
                continue;
            }
            final String expected = expectedResultedIns.remove(0);
            assertTrue(line.startsWith("Resulted in: " + expected), "Unexpected stacktrace element '" + line + "'");
        }
        assertTrue(expectedResultedIns.isEmpty(), "Reversed stacktrace is missing certain elements");
    }

    private Throwable generateException() {
        try {
            try {
                Integer.parseInt("23.23232");
            } catch (NumberFormatException nfe) {
                throw new IllegalArgumentException("Incorrect param", nfe);
            }
        } catch (IllegalArgumentException iae) {
            try {
                throw new IOException("Request processing failed", iae);
            } catch (IOException e) {
                try {
                    throw new IOError(e);
                } catch (IOError ie) {
                    return new RuntimeException("Unexpected exception", ie);
                }
            }
        }
        throw new RuntimeException("Should not reach here");
    }
}
