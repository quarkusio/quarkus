package io.quarkus.cli.plugin;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;

public class PluginTest {

    private final ObjectMapper objectMapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .registerModule(new Jdk8Module());

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
