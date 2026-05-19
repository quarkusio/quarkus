package io.quarkus.devui;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Iterator;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.fasterxml.jackson.databind.JsonNode;

import io.quarkus.devui.tests.DevUIJsonRPCTest;
import io.quarkus.test.QuarkusDevModeTest;

public class LogstreamTest extends DevUIJsonRPCTest {

    @RegisterExtension
    static final QuarkusDevModeTest config = new QuarkusDevModeTest()
            .withEmptyApplication();

    public LogstreamTest() {
        super("devui-logstream");
    }

    @Test
    public void testHistory() throws Exception {
        JsonNode historyResponse = super.executeJsonRPCMethod("history");
        assertThat(historyResponse).isNotNull();
        assertThat(historyResponse.isArray()).isTrue();
        assertThat(hasStartedLine(historyResponse.elements())).isTrue();
    }

    @Test
    public void testGetLoggers() throws Exception {
        JsonNode getLoggersResponse = super.executeJsonRPCMethod("getLoggers");
        assertThat(getLoggersResponse).isNotNull();
        assertThat(getLoggersResponse.isArray()).isTrue();
        int size = getLoggersResponse.size();
        assertThat(size).isGreaterThan(0);
    }

    @Test
    public void testUpdateLoggers() throws Exception {
        // Get the level before
        JsonNode getLoggerResponse = super.executeJsonRPCMethod("getLogger",
                Map.of("loggerName", "io.quarkus.devui.runtime.DevUIWebSocket"));
        assertThat(getLoggerResponse).isNotNull();
        assertThat(getLoggerResponse.get("effectiveLevel").asText()).isEqualTo("INFO");

        // Update the level
        JsonNode updateLogLevelResponse = super.executeJsonRPCMethod("updateLogLevel",
                Map.of("loggerName", "io.quarkus.devui.runtime.DevUIWebSocket",
                        "levelValue", "DEBUG"));
        assertThat(updateLogLevelResponse).isNotNull();
        assertThat(updateLogLevelResponse.get("effectiveLevel").asText()).isEqualTo("DEBUG");

        // Restore the level
        JsonNode restoreLogLevelResponse = super.executeJsonRPCMethod("updateLogLevel",
                Map.of("loggerName", "io.quarkus.devui.runtime.DevUIWebSocket",
                        "levelValue", "INFO"));
        assertThat(restoreLogLevelResponse).isNotNull();
        assertThat(restoreLogLevelResponse.get("effectiveLevel").asText()).isEqualTo("INFO");

    }

    @Test
    public void testGetRootLogger() throws Exception {
        JsonNode getRootLoggerResponse = super.executeJsonRPCMethod("getRootLogger");
        assertThat(getRootLoggerResponse).isNotNull();
        assertThat(getRootLoggerResponse.get("name").asText()).isEqualTo("");

        // Validate effectiveLevel is a valid log level name
        String effectiveLevel = getRootLoggerResponse.get("effectiveLevel").asText();
        assertThat(effectiveLevel).as("effectiveLevel should not be null").isNotNull();
        assertThat(isValidLogLevel(effectiveLevel))
                .as("effectiveLevel should be a valid log level (TRACE, DEBUG, INFO, WARN, ERROR, FATAL, OFF), got: "
                        + effectiveLevel)
                .isTrue();

        // Validate configuredLevel is either null or a valid log level name
        JsonNode configuredLevelNode = getRootLoggerResponse.get("configuredLevel");
        if (!configuredLevelNode.isNull()) {
            String configuredLevel = configuredLevelNode.asText();
            assertThat(isValidLogLevel(configuredLevel))
                    .as("configuredLevel should be a valid log level (TRACE, DEBUG, INFO, WARN, ERROR, FATAL, OFF), got: "
                            + configuredLevel)
                    .isTrue();
        }
    }

    private boolean isValidLogLevel(String level) {
        return level != null &&
                (level.equals("TRACE") || level.equals("DEBUG") || level.equals("INFO") ||
                        level.equals("WARN") || level.equals("ERROR") || level.equals("FATAL") ||
                        level.equals("OFF"));
    }

    @Test
    public void testUpdateRootLogger() throws Exception {
        // Get the root logger level before
        JsonNode getRootLoggerResponse = super.executeJsonRPCMethod("getRootLogger");
        assertThat(getRootLoggerResponse).isNotNull();
        String originalLevel = getRootLoggerResponse.get("effectiveLevel").asText();

        try {
            // Update the root logger level to DEBUG
            JsonNode updateLogLevelResponse = super.executeJsonRPCMethod("updateLogLevel",
                    Map.of("loggerName", "",
                            "levelValue", "DEBUG"));
            assertThat(updateLogLevelResponse).isNotNull();
            assertThat(updateLogLevelResponse.get("name").asText()).isEqualTo("");
            assertThat(updateLogLevelResponse.get("effectiveLevel").asText()).isEqualTo("DEBUG");

            // Verify feedback loop prevention: Make multiple JSON-RPC calls to generate WebSocket traffic.
            // Each call triggers Netty WebSocket frame encoding/decoding. If the feedback loop filter
            // wasn't working, Netty would emit DEBUG logs for each frame, those logs would be broadcast
            // over the same WebSocket, triggering more Netty logs in an exponentially growing loop.
            // Without the filter, we'd see hundreds or thousands of Netty logs within a few iterations.
            verifyNoFeedbackLoopAtLevel("DEBUG", 5);

        } finally {
            // Restore the root logger level
            super.executeJsonRPCMethod("updateLogLevel",
                    Map.of("loggerName", "",
                            "levelValue", originalLevel));
        }
    }

    @Test
    public void testUpdateRootLoggerToTrace() throws Exception {
        JsonNode getRootLoggerResponse = super.executeJsonRPCMethod("getRootLogger");
        assertThat(getRootLoggerResponse).isNotNull();
        String originalLevel = getRootLoggerResponse.get("effectiveLevel").asText();

        try {
            // Set root logger to TRACE - the most verbose level
            JsonNode traceResponse = super.executeJsonRPCMethod("updateLogLevel",
                    Map.of("loggerName", "",
                            "levelValue", "TRACE"));
            assertThat(traceResponse).isNotNull();
            assertThat(traceResponse.get("effectiveLevel").asText()).isEqualTo("TRACE");

            JsonNode verifyResponse = super.executeJsonRPCMethod("getRootLogger");
            assertThat(verifyResponse).isNotNull();
            assertThat(verifyResponse.get("effectiveLevel").asText()).isEqualTo("TRACE");

            // Verify feedback loop prevention at TRACE level: At TRACE level, Netty emits a TRACE log
            // for every WebSocket frame it encodes/decodes. Without the filter, these logs would be
            // broadcast over the same WebSocket, triggering more Netty TRACE logs in an exponentially
            // growing feedback loop that would saturate the event loop and hang the application within
            // seconds. This is the most critical test case as TRACE is the most verbose level.
            verifyNoFeedbackLoopAtLevel("TRACE", 5);

        } finally {
            super.executeJsonRPCMethod("updateLogLevel",
                    Map.of("loggerName", "",
                            "levelValue", originalLevel));
        }
    }

    /**
     * Verify that the feedback loop filter is working at the given log level.
     *
     * <p>
     * <strong>Context:</strong> When the root logger is set to TRACE or DEBUG, Netty WebSocket frame
     * encoder/decoder emits TRACE/DEBUG logs for every WebSocket frame. Without filtering in MutinyLogHandler,
     * these logs would be broadcast over the same WebSocket, triggering more Netty logs in an exponentially
     * growing feedback loop that would hang the application within seconds.
     *
     * <p>
     * This test generates WebSocket traffic by making JSON-RPC calls (which use WebSocket transport) and verifies:
     * <ol>
     * <li>No Netty/Vertx/logstream framework logs at verbose levels appear in history (they're filtered)</li>
     * <li>History size remains bounded (not growing exponentially)</li>
     * <li>Growth per iteration is minimal (no feedback loop amplification)</li>
     * </ol>
     *
     * @param level The log level being tested (DEBUG or TRACE)
     * @param iterations Number of JSON-RPC calls to make (each generates WebSocket traffic)
     */
    private void verifyNoFeedbackLoopAtLevel(String level, int iterations) throws Exception {
        int previousSize = 0;
        int maxGrowthPerIteration = 0;

        for (int i = 0; i < iterations; i++) {
            // Each JSON-RPC call uses WebSocket transport. At TRACE/DEBUG levels, this triggers:
            // - Netty WebSocket08FrameEncoder TRACE logs for each outbound frame
            // - Netty WebSocket08FrameDecoder TRACE logs for each inbound frame
            // - Vert.x Http1xServerConnection DEBUG logs for connection events
            // Without the filter in MutinyLogHandler, these would be broadcast back over the
            // WebSocket, creating an infinite loop. This call tests that the filter works.
            JsonNode historyResponse = super.executeJsonRPCMethod("history");
            assertThat(historyResponse).isNotNull();
            assertThat(historyResponse.isArray()).isTrue();

            int currentSize = historyResponse.size();

            // Track maximum growth between iterations to detect exponential amplification
            if (i > 0) {
                int growth = currentSize - previousSize;
                maxGrowthPerIteration = Math.max(maxGrowthPerIteration, growth);
            }
            previousSize = currentSize;

            // Scan history for framework logs that should have been filtered.
            // If the filter wasn't working, we'd see Netty/Vertx TRACE/DEBUG logs here.
            int frameworkLogCount = 0;
            for (JsonNode logEntry : historyResponse) {
                String loggerName = logEntry.has("loggerName") ? logEntry.get("loggerName").asText() : "";
                String logLevel = logEntry.has("level") ? logEntry.get("level").asText() : "";

                // Identify framework loggers that emit verbose logs during WebSocket operations
                boolean isFrameworkLogger = loggerName.startsWith("io.netty.") ||
                        loggerName.startsWith("io.vertx.") ||
                        loggerName.startsWith("io.quarkus.devui.runtime.logstream.");

                // Check for verbose levels (both standard and JUL naming)
                boolean isVerboseLevel = logLevel.equals("TRACE") || logLevel.equals("DEBUG") ||
                        logLevel.equals("FINEST") || logLevel.equals("FINE");

                if (isFrameworkLogger && isVerboseLevel) {
                    frameworkLogCount++;
                }
            }

            assertThat(frameworkLogCount)
                    .as("Iteration %d: Framework (Netty/Vertx/logstream) %s logs must be filtered. "
                            + "Found %d logs that should have been filtered. "
                            + "Without the filter, WebSocket frame encoding would trigger logs, "
                            + "which would be broadcast, triggering more logs in an infinite loop.",
                            i, level, frameworkLogCount)
                    .isEqualTo(0);

            // Verify history size is reasonable - without the filter, we'd see 1000+ logs
            // within a few iterations due to exponential growth
            assertThat(currentSize)
                    .as("Iteration %d: History size must remain bounded. Size %d exceeds limit. "
                            + "Without the filter, exponential feedback would cause size to explode.",
                            i, currentSize)
                    .isLessThan(1000);
        }

        // Final check: verify that growth per iteration is minimal.
        // Without the filter, each iteration would add hundreds/thousands of logs (exponential).
        // With the filter, we should see only a few logs per iteration (application logs only).
        assertThat(maxGrowthPerIteration)
                .as("Maximum history growth per iteration was %d. This should be minimal (< 50). "
                        + "Without the filter, we'd see exponential growth (hundreds of logs per iteration) "
                        + "as each broadcast triggers more Netty logs, which trigger more broadcasts.",
                        maxGrowthPerIteration)
                .isLessThan(50);
    }

    private boolean hasStartedLine(Iterator<JsonNode> elements) {
        while (elements.hasNext()) {
            JsonNode next = elements.next();
            String line = next.get("formattedMessage").asText();

            if (line.contains("powered by Quarkus") && line.contains(" started in ")) {
                return true;
            }
        }
        return false;
    }
}
