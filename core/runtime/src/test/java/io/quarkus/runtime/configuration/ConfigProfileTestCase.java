package io.quarkus.runtime.configuration;

import static java.util.Collections.singletonMap;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import io.smallrye.config.PropertiesConfigSource;
import io.smallrye.config.SmallRyeConfig;
import io.smallrye.config.SmallRyeConfigBuilder;

public class ConfigProfileTestCase {

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
        builder.withWrapper(DeploymentProfileConfigSource.wrapper());
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
    public void testDefaultProfile() {
        final SmallRyeConfig config = buildConfig(maps(
                singletonMap("foo.one", "v1"),
                singletonMap("foo.two", "v2"),
                singletonMap("%foo.foo.three", "f1"),
                singletonMap("%prod.foo.four", "v4")));
        assertEquals("v1", config.getValue("foo.one", String.class));
        assertEquals("v2", config.getValue("foo.two", String.class));
        assertFalse(config.getOptionalValue("foo.three", String.class).isPresent());
        assertEquals("v4", config.getValue("foo.four", String.class));
    }

    @Test
    public void testOverridenProfile() {
        System.setProperty("quarkus.profile", "foo");
        try {
            final SmallRyeConfig config = buildConfig(maps(
                    singletonMap("foo.one", "v1"),
                    singletonMap("foo.two", "v2"),
                    singletonMap("%foo.foo.three", "f1"),
                    singletonMap("%prod.foo.four", "v4")));
            assertEquals("v1", config.getValue("foo.one", String.class));
            assertEquals("v2", config.getValue("foo.two", String.class));
            assertEquals("f1", config.getValue("foo.three", String.class));
            assertFalse(config.getOptionalValue("foo.four", String.class).isPresent());
        } finally {
            System.clearProperty("quarkus.profile");
        }
    }

    @Test
    public void testBackwardCompatibleOverridenProfile() {
        System.setProperty("quarkus-profile", "foo");
        try {
            final SmallRyeConfig config = buildConfig(maps(
                    singletonMap("foo.one", "v1"),
                    singletonMap("foo.two", "v2"),
                    singletonMap("%foo.foo.three", "f1"),
                    singletonMap("%prod.foo.four", "v4")));
            assertEquals("v1", config.getValue("foo.one", String.class));
            assertEquals("v2", config.getValue("foo.two", String.class));
            assertEquals("f1", config.getValue("foo.three", String.class));
            assertFalse(config.getOptionalValue("foo.four", String.class).isPresent());
        } finally {
            System.clearProperty("quarkus-profile");
        }
    }
}
