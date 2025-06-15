package io.quarkus.devui;

import java.util.Iterator;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.fasterxml.jackson.databind.JsonNode;

import io.quarkus.devui.tests.DevUIJsonRPCTest;
import io.quarkus.test.QuarkusDevModeTest;

public class LogstreamTest extends DevUIJsonRPCTest {

    @RegisterExtension
    static final QuarkusDevModeTest config = new QuarkusDevModeTest().withEmptyApplication();

    public LogstreamTest() {
        super("devui-logstream");
    }

    @Test
    public void testHistory() throws Exception {
        JsonNode historyResponse = super.executeJsonRPCMethod("history");
        Assertions.assertNotNull(historyResponse);
        Assertions.assertTrue(historyResponse.isArray());
        Assertions.assertTrue(hasStartedLine(historyResponse.elements()));
    }

    @Test
    public void testGetLoggers() throws Exception {
        JsonNode getLoggersResponse = super.executeJsonRPCMethod("getLoggers");
        Assertions.assertNotNull(getLoggersResponse);
        Assertions.assertTrue(getLoggersResponse.isArray());
        int size = getLoggersResponse.size();
        Assertions.assertTrue(size > 0);
    }

    @Test
    public void testUpdateLoggers() throws Exception {
        // Get the level before
        JsonNode getLoggerResponse = super.executeJsonRPCMethod("getLogger",
                Map.of("loggerName", "io.quarkus.devui.runtime.DevUIWebSocket"));
        Assertions.assertNotNull(getLoggerResponse);
        Assertions.assertEquals("INFO", getLoggerResponse.get("effectiveLevel").asText());

        // Update the level
        JsonNode updateLogLevelResponse = super.executeJsonRPCMethod("updateLogLevel",
                Map.of("loggerName", "io.quarkus.devui.runtime.DevUIWebSocket", "levelValue", "DEBUG"));
        Assertions.assertNotNull(updateLogLevelResponse);
        Assertions.assertEquals("DEBUG", updateLogLevelResponse.get("effectiveLevel").asText());

        // Restore the level
        JsonNode restoreLogLevelResponse = super.executeJsonRPCMethod("updateLogLevel",
                Map.of("loggerName", "io.quarkus.devui.runtime.DevUIWebSocket", "levelValue", "INFO"));
        Assertions.assertNotNull(restoreLogLevelResponse);
        Assertions.assertEquals("INFO", restoreLogLevelResponse.get("effectiveLevel").asText());

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
