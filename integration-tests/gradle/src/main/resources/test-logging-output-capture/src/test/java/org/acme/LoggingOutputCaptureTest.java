package org.acme;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;
import org.jboss.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Verifies that Quarkus logger output is properly captured by Gradle's test
 * output capture mechanism, so that testLogging.showStandardStreams = false
 * can suppress it.
 *
 * The test itself always passes. The Gradle wrapper test that invokes this
 * project checks that the marker strings below do NOT appear in the build
 * output when showStandardStreams = false.
 */
@QuarkusTest
public class LoggingOutputCaptureTest {

    private static final Logger LOG = Logger.getLogger(LoggingOutputCaptureTest.class);

    @Test
    void testLoggerOutput() {
        LOG.info("MARKER_INFO_LOG_OUTPUT");
        LOG.warn("MARKER_WARN_LOG_OUTPUT");

        // Also test System.out, which should be suppressed too
        System.out.println("MARKER_STDOUT_OUTPUT");

        assertEquals("hello", new ExampleService().greet());
    }
}
