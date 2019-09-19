package io.quarkus.deployment.devmode;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Tests {@link ReplacementDebugPage}
 */
public class ReplacementDebugPageTest {

    /**
     * Test that the stacktrace generated for display in HTML, by {@link ReplacementDebugPage},
     * shows the root cause (if any) first
     *
     * @throws Exception
     */
    @Test
    public void testRootCauseStackTraceGeneration() throws Exception {
        // try with one which doesn't have a root cause
        final String html1 = ReplacementDebugPage.generateHtml(new Exception("testStackTraceGeneration - simple"));
        assertNotNull(html1, "HTML wasn't generated");
        assertFalse(html1.contains("root cause shown first"),
                "Single level exception wasn't expected to have a nested root cause");

        // now try with one which has nested root cause
        final Throwable rootNPE = new NullPointerException("intentional NPE");
        Throwable multiNested = rootNPE;
        for (int i = 0; i < 10; i++) {
            multiNested = wrap(multiNested, "Level " + i);
        }
        final String html2 = ReplacementDebugPage.generateHtml(multiNested);
        assertNotNull(html2, "HTML wasn't generated");
        assertTrue(html2.contains("root cause shown first"), "Root cause wasn't displayed");
        assertTrue(html2.contains("complete stacktrace follows"), "Complete stacktrace wasn't displayed");
    }

    private static Throwable wrap(final Throwable t, final String message) {
        return new RuntimeException(message, t);
    }
}
