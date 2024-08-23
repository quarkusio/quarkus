package io.quarkus.test.devui;

import java.util.Iterator;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.fasterxml.jackson.databind.JsonNode;

import io.quarkus.devui.tests.DevUIJsonRPCTest;
import io.quarkus.test.QuarkusDevModeTest;

public class DevUIRestJsonRPCTest extends DevUIJsonRPCTest {

    @RegisterExtension
    static final QuarkusDevModeTest config = new QuarkusDevModeTest()
            .withApplicationRoot((jar) -> jar.addClass(MyResource.class));

    public DevUIRestJsonRPCTest() {
        super("io.quarkus.quarkus-rest");
    }

    @Test
    public void testEndpoints() throws Exception {

        JsonNode endpointScores = super.executeJsonRPCMethod("getEndpointScores");
        Assertions.assertNotNull(endpointScores);
        int score = endpointScores.get("score").asInt();
        Assertions.assertEquals(66, score);

        JsonNode endpoints = endpointScores.get("endpoints");
        Assertions.assertNotNull(endpoints);
        Assertions.assertTrue(endpoints.isArray());

        Iterator<JsonNode> en = endpoints.elements();
        boolean exists = false;
        while (en.hasNext()) {
            JsonNode endpoint = en.next();
            String className = endpoint.get("className").asText();
            String httpMethod = endpoint.get("httpMethod").asText();
            String fullPath = endpoint.get("fullPath").asText();
            if (className.equals("io.quarkus.test.devui.MyResource")
                    && httpMethod.equals("GET")
                    && fullPath.equals("/me/message")) {
                exists = true;
                break;
            }
        }

        Assertions.assertTrue(exists);
    }

}
