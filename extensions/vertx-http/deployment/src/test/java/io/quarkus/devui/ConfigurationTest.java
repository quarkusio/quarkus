package io.quarkus.devui;

import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.fasterxml.jackson.databind.JsonNode;

import io.quarkus.devui.tests.DevUIJsonRPCTest;
import io.quarkus.test.QuarkusDevModeTest;

public class ConfigurationTest extends DevUIJsonRPCTest {

    @RegisterExtension
    static final QuarkusDevModeTest config = new QuarkusDevModeTest()
            .withEmptyApplication();

    public ConfigurationTest() {
        super("devui-configuration");
    }

    @Test
    public void testConfigurationUpdate() throws Exception {

        JsonNode updatePropertyResponse = super.executeJsonRPCMethod("updateProperty",
                Map.of(
                        "name", "quarkus.application.name",
                        "value", "changedByTest"));
        Assertions.assertNotNull(updatePropertyResponse);
        Assertions.assertTrue(updatePropertyResponse.asBoolean());

        // Get the properties to make sure it is changed
        JsonNode allPropertiesResponse = super.executeJsonRPCMethod("getAllValues");
        Assertions.assertNotNull(allPropertiesResponse);
        String applicationName = allPropertiesResponse.get("quarkus.application.name").asText();
        Assertions.assertEquals("changedByTest", applicationName);
    }
}
