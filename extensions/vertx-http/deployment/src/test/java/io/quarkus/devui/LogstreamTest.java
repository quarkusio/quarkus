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
    static final QuarkusDevModeTest config = new QuarkusDevModeTest()
            .withEmptyApplication();

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
    }

    @Test
    public void testUpdateLoggers() throws Exception {
        // Get the level before
        JsonNode getLoggerResponse = super.executeJsonRPCMethod("getLogger", Map.of("loggerName", "io.quarkus"));
        Assertions.assertNotNull(getLoggerResponse);
        Assertions.assertEquals("INFO", getLoggerResponse.get("effectiveLevel").asText());

        // Update the level
        JsonNode updateLogLevelResponse = super.executeJsonRPCMethod("updateLogLevel",
                Map.of("loggerName", "io.quarkus",
                        "levelValue", "DEBUG"));
        Assertions.assertNotNull(updateLogLevelResponse);
        Assertions.assertEquals("DEBUG", updateLogLevelResponse.get("effectiveLevel").asText());
    }

    @Override
    protected String getNamespace() {
        return "devui-logstream";
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
