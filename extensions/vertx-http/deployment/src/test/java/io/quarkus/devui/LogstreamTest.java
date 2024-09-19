package io.quarkus.devui;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.fasterxml.jackson.databind.JsonNode;

import io.quarkus.devui.tests.DevUITest;
import io.quarkus.devui.tests.JsonRPCServiceClient;
import io.quarkus.devui.tests.Namespace;
import io.quarkus.test.QuarkusDevModeTest;

@DevUITest(@Namespace("devui-logstream"))
class LogstreamTest {

    @RegisterExtension
    static final QuarkusDevModeTest config = new QuarkusDevModeTest()
            .withEmptyApplication()
            .shared();

    @Test
    public void testHistory(JsonRPCServiceClient client) throws Exception {
        JsonNode historyResponse = client
                .request("history")
                .send()
                .get(5, TimeUnit.SECONDS);
        Assertions.assertNotNull(historyResponse);
        Assertions.assertTrue(historyResponse.isArray());
        Assertions.assertTrue(hasStartedLine(historyResponse.elements()));
    }

    @Test
    public void testGetLoggers(JsonRPCServiceClient client) throws Exception {
        JsonNode getLoggersResponse = client
                .request("getLoggers")
                .send()
                .get(5, TimeUnit.SECONDS);
        Assertions.assertNotNull(getLoggersResponse);
        Assertions.assertTrue(getLoggersResponse.isArray());
        int size = getLoggersResponse.size();
        Assertions.assertTrue(size > 0);
    }

    @Test
    public void testUpdateLoggers(JsonRPCServiceClient client) throws Exception {
        // Get the level before
        JsonNode getLoggerResponse = client
                .request(
                        "getLogger",
                        Map.of("loggerName", "io.quarkus.devui.runtime.DevUIWebSocket"))
                .send()
                .get(5, TimeUnit.SECONDS);
        Assertions.assertNotNull(getLoggerResponse);
        Assertions.assertEquals("INFO", getLoggerResponse.get("effectiveLevel").asText());

        // Update the level
        JsonNode updateLogLevelResponse = client
                .request(
                        "updateLogLevel",
                        Map.of(
                                "loggerName", "io.quarkus.devui.runtime.DevUIWebSocket",
                                "levelValue", "DEBUG"))
                .send()
                .get(5, TimeUnit.SECONDS);
        Assertions.assertNotNull(updateLogLevelResponse);
        Assertions.assertEquals("DEBUG", updateLogLevelResponse.get("effectiveLevel").asText());

        // Restore the level
        JsonNode restoreLogLevelResponse = client
                .request(
                        "updateLogLevel",
                        Map.of(
                                "loggerName", "io.quarkus.devui.runtime.DevUIWebSocket",
                                "levelValue", "INFO"))
                .send()
                .get(5, TimeUnit.SECONDS);
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
