package org.shamrock.runtime.configuration;

import static java.util.Collections.singletonMap;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

import io.smallrye.config.PropertiesConfigSource;
import io.smallrye.config.SmallRyeConfig;
import io.smallrye.config.SmallRyeConfigBuilder;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;
import org.jboss.shamrock.runtime.configuration.ExpandingConfigSource;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 */
public class ConfigExpanderTestCase {

    @BeforeClass
    public static void initConfig() {
        ConfigProviderResolver.setInstance(new TestConfigProviderResolver());
    }

    private SmallRyeConfig buildConfig(Map<String, String> configMap) {
        final SmallRyeConfigBuilder builder = new SmallRyeConfigBuilder();
        builder.withWrapper(ExpandingConfigSource.WRAPPER);
        builder.withSources(new PropertiesConfigSource(configMap, "test input", 500));
        final SmallRyeConfig config = (SmallRyeConfig) builder.build();
        TestConfigProviderResolver.config = config;
        return config;
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
            singletonMap("foo.three", "+${foo.two}+")
        ));
        assertEquals("value", config.getValue("foo.one", String.class));
        assertEquals("value", config.getValue("foo.two", String.class));
        assertEquals("+value+", config.getValue("foo.three", String.class));
    }

    @Test
    public void testExpanderDefaults() {
        final SmallRyeConfig config = buildConfig(maps(
            singletonMap("foo.two", "${foo.one:value}"),
            singletonMap("foo.three", "+${foo.two}+")
        ));
        assertEquals("value", config.getValue("foo.two", String.class));
        assertEquals("+value+", config.getValue("foo.three", String.class));
    }

    @Test
    public void testExpanderMissing() {
        final SmallRyeConfig config = buildConfig(maps(
            singletonMap("foo.two", "${foo.one}empty"),
            singletonMap("foo.three", "+${foo.two}+")
        ));
        try {
            config.getValue("foo.two", String.class);
            fail("Expected exception");
        } catch (NoSuchElementException expected) {
            // OK
        }
        try {
            config.getValue("foo.three", String.class);
            fail("Expected exception");
        } catch (NoSuchElementException expected) {
            // OK
        }
    }

    @Test
    public void testExpanderOptional() {
        final SmallRyeConfig config = buildConfig(maps(
            singletonMap("foo.two", "${foo.one?}empty"),
            singletonMap("foo.three", "+${foo.two}+")
        ));
        assertEquals("empty", config.getValue("foo.two", String.class));
        assertEquals("+empty+", config.getValue("foo.three", String.class));
    }

    @Test
    public void testStackBlowOut() {
        final SmallRyeConfig config = buildConfig(maps(
            singletonMap("foo.blowout", "${foo.blowout}")
        ));
        try {
            config.getValue("foo.blowout", String.class);
            fail("Expected exception");
        } catch (IllegalStateException expected) {
            // OK
        }
    }
}
