package io.quarkus.runtime.configuration;

import static java.util.Collections.singletonMap;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import io.smallrye.config.ExpressionConfigSourceInterceptor;
import io.smallrye.config.PropertiesConfigSource;
import io.smallrye.config.SmallRyeConfig;
import io.smallrye.config.SmallRyeConfigBuilder;

/**
 */
public class ConfigExpanderTestCase {

    static ClassLoader classLoader;
    static ConfigProviderResolver cpr;
    Config config;

    @BeforeAll
    public static void initConfig() {
        classLoader = Thread.currentThread().getContextClassLoader();
        cpr = ConfigProviderResolver.instance();
    }

    @AfterEach
    public void doAfter() {
        try {
            cpr.releaseConfig(cpr.getConfig());
        } catch (IllegalStateException ignored) {
            // just means no config was installed, which is fine
        }
    }

    private SmallRyeConfig buildConfig(Map<String, String> configMap) {
        final SmallRyeConfigBuilder builder = new SmallRyeConfigBuilder();
        builder.withInterceptors(new ExpressionConfigSourceInterceptor());
        builder.withSources(new PropertiesConfigSource(configMap, "test input", 500));
        final SmallRyeConfig config = (SmallRyeConfig) builder.build();
        cpr.registerConfig(config, classLoader);
        this.config = config;
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
        final SmallRyeConfig config = buildConfig(maps(
                singletonMap("foo.two", "${foo.one}empty"),
                singletonMap("foo.three", "+${foo.two}+")));
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
