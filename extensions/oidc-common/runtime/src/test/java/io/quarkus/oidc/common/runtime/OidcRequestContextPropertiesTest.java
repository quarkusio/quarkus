package io.quarkus.oidc.common.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Map;

import org.junit.jupiter.api.Test;

import io.quarkus.oidc.common.OidcRequestContextProperties;

public class OidcRequestContextPropertiesTest {

    @Test
    public void testModifyPropertiesDefaultConstructor() throws Exception {
        OidcRequestContextProperties props = new OidcRequestContextProperties();
        assertNull(props.get("a"));
        props.put("a", "value");
        assertEquals("value", props.get("a"));
    }

    @Test
    public void testModifyExistinProperties() throws Exception {
        OidcRequestContextProperties props = new OidcRequestContextProperties(Map.of("a", "value"));
        assertEquals("value", props.get("a"));
        props.put("a", "avalue");
        assertEquals("avalue", props.get("a"));
        props.put("b", "bvalue");
        assertEquals("bvalue", props.get("b"));
    }
}
