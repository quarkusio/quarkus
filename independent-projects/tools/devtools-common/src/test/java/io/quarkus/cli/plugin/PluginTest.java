package io.quarkus.cli.plugin;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectMapper;

public class PluginTest {

    private final ObjectMapper objectMapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    @Test
    public void shouldNotHaveNullProperties() throws Exception {
        assertPluginFieldsNotNull(new Plugin("my-plugin", PluginType.executable));
        assertPluginFieldsNotNull(new Plugin("my-plugin", PluginType.executable, null, null, null, false));
        assertPluginFieldsNotNull(objectMapper.readValue("{\"name\": \"my-plugin\", \"type\": \"executable\"}", Plugin.class));
    }

    private void assertPluginFieldsNotNull(Plugin p) {
        assertNotNull(p.getDescription());
        assertNotNull(p.getLocation());
        assertNotNull(p.getCatalogLocation());
    }
}
