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

    @Test
    public void testSearchConfigByQuery() throws Exception {
        JsonNode response = super.executeJsonRPCMethod("searchConfig",
                Map.of("query", "application.name"));
        Assertions.assertNotNull(response);
        Assertions.assertTrue(response.isArray());
        Assertions.assertTrue(response.size() > 0, "Should find at least one config matching 'application.name'");

        // Verify the result structure
        JsonNode first = response.get(0);
        Assertions.assertNotNull(first.get("name"));
        Assertions.assertTrue(first.get("name").asText().contains("application.name"));
    }

    @Test
    public void testSearchConfigWithLimit() throws Exception {
        JsonNode response = super.executeJsonRPCMethod("searchConfig",
                Map.of("query", "quarkus", "limit", 2));
        Assertions.assertNotNull(response);
        Assertions.assertTrue(response.isArray());
        Assertions.assertTrue(response.size() <= 2, "Should respect the limit parameter");
    }

    @Test
    public void testSearchConfigByExtension() throws Exception {
        JsonNode response = super.executeJsonRPCMethod("searchConfig",
                Map.of("query", "", "extension", "log"));
        Assertions.assertNotNull(response);
        Assertions.assertTrue(response.isArray());
        for (int i = 0; i < response.size(); i++) {
            String name = response.get(i).get("name").asText();
            Assertions.assertTrue(name.startsWith("quarkus.log."),
                    "Config key '" + name + "' should start with 'quarkus.log.'");
        }
    }

    @Test
    public void testGetExtensionConfig() throws Exception {
        JsonNode response = super.executeJsonRPCMethod("getExtensionConfig",
                Map.of("extension", "log"));
        Assertions.assertNotNull(response);
        Assertions.assertTrue(response.isArray());
        Assertions.assertTrue(response.size() > 0, "Should find config keys for the 'log' extension");

        for (int i = 0; i < response.size(); i++) {
            String name = response.get(i).get("name").asText();
            Assertions.assertTrue(name.startsWith("quarkus.log."),
                    "Config key '" + name + "' should start with 'quarkus.log.'");
        }
    }

    @Test
    public void testGetExtensionConfigEmpty() throws Exception {
        JsonNode response = super.executeJsonRPCMethod("getExtensionConfig",
                Map.of("extension", ""));
        Assertions.assertNotNull(response);
        Assertions.assertTrue(response.isArray());
        Assertions.assertEquals(0, response.size(), "Empty extension should return no results");
    }

    @Test
    public void testSearchConfigResultStructure() throws Exception {
        JsonNode response = super.executeJsonRPCMethod("searchConfig",
                Map.of("query", "log.level", "limit", 1));
        Assertions.assertNotNull(response);
        Assertions.assertTrue(response.isArray());
        Assertions.assertTrue(response.size() > 0);

        JsonNode entry = response.get(0);
        Assertions.assertNotNull(entry.get("name"), "Result should have 'name'");
        Assertions.assertNotNull(entry.get("type"), "Result should have 'type'");
        // currentValue should only be present when non-null
        if (entry.has("currentValue")) {
            Assertions.assertFalse(entry.get("currentValue").isNull(),
                    "currentValue should not be null when present");
        }
    }
}
