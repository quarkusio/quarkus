package io.quarkus.runtime.configuration;

import static java.util.Collections.singletonMap;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

import org.junit.jupiter.api.Test;

import io.smallrye.config.PropertiesConfigSource;
import io.smallrye.config.SmallRyeConfig;
import io.smallrye.config.SmallRyeConfigBuilder;

public class ConfigExpanderTestCase {

    private SmallRyeConfig buildConfig(Map<String, String> configMap) {
        return new SmallRyeConfigBuilder()
                .addDefaultInterceptors()
                .withSources(new PropertiesConfigSource(configMap, "test input", 500))
                .build();
    }

    private Map<String, String> maps(Map... maps) {
        Map<String, String> out = new HashMap<>();
        for (Map map : maps) {
            out.putAll(map);
        }
        return out;
    }

    @Test
    public void testBasicExpander() {
        final SmallRyeConfig config = buildConfig(maps(
                singletonMap("foo.one", "value"),
                singletonMap("foo.two", "${foo.one}"),
                singletonMap("foo.three", "+${foo.two}+")));
        assertEquals("value", config.getValue("foo.one", String.class));
        assertEquals("value", config.getValue("foo.two", String.class));
        assertEquals("+value+", config.getValue("foo.three", String.class));
    }

    @Test
    public void testExpanderDefaults() {
        final SmallRyeConfig config = buildConfig(maps(
                singletonMap("foo.two", "${foo.one:value}"),
                singletonMap("foo.three", "+${foo.two}+")));
        assertEquals("value", config.getValue("foo.two", String.class));
        assertEquals("+value+", config.getValue("foo.three", String.class));
    }

    @Test
    public void testExpanderMissing() {
        final SmallRyeConfig config = buildConfig(
                maps(singletonMap("foo.two", "${foo.one}empty"),
                        singletonMap("foo.three", "+${foo.two}+")));

        assertThrows(NoSuchElementException.class, () -> config.getValue("foo.two", String.class));

        assertThrows(NoSuchElementException.class, () -> config.getValue("foo.three", String.class));
    }

    @Test
    public void testExpanderOptional() {
        final SmallRyeConfig config = buildConfig(maps(
                singletonMap("foo.two", "${foo.one:}empty"),
                singletonMap("foo.three", "+${foo.two}+")));
        assertEquals("empty", config.getValue("foo.two", String.class));
        assertEquals("+empty+", config.getValue("foo.three", String.class));
    }

    @Test
    public void testStackBlowOut() {
        final SmallRyeConfig config = buildConfig(maps(
                singletonMap("foo.blowout", "${foo.blowout}")));
        try {
            config.getValue("foo.blowout", String.class);
            fail("Expected exception");
        } catch (IllegalArgumentException expected) {
            // OK
        }
    }
}
