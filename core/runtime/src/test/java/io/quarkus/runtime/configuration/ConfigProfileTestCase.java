package io.quarkus.runtime.configuration;

import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.StreamSupport;

import org.eclipse.microprofile.config.spi.ConfigProviderResolver;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import io.smallrye.config.ExpressionConfigSourceInterceptor;
import io.smallrye.config.ProfileConfigSourceInterceptor;
import io.smallrye.config.PropertiesConfigSource;
import io.smallrye.config.SmallRyeConfig;
import io.smallrye.config.SmallRyeConfigBuilder;

public class ConfigProfileTestCase {

    static ClassLoader classLoader;
    static ConfigProviderResolver cpr;

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

    private SmallRyeConfigBuilder configBuilder(String... keyValues) {
        return new SmallRyeConfigBuilder()
                .addDefaultInterceptors()
                .withSources(new PropertiesConfigSource(maps(keyValues), "test input", 500))
                .withProfile(ProfileManager.getActiveProfile());
    }

    private SmallRyeConfig buildConfig(String... keyValues) {
        final SmallRyeConfig config = configBuilder(keyValues).build();
        cpr.registerConfig(config, classLoader);
        return config;
    }

    private Map<String, String> maps(String... keyValues) {
        if (keyValues.length % 2 != 0) {
            throw new IllegalArgumentException("keyValues array must be a multiple of 2");
        }

        Map<String, String> props = new HashMap<>();
        for (int i = 0; i < keyValues.length; i += 2) {
            props.put(keyValues[i], keyValues[i + 1]);
        }

        return props;
    }

    @Test
    void defaultProfile() {
        final SmallRyeConfig config = buildConfig(
                "foo.one", "v1",
                "foo.two", "v2",
                "%foo.foo.three", "f1",
                "%prod.foo.four", "v4");
        assertEquals("v1", config.getValue("foo.one", String.class));
        assertEquals("v2", config.getValue("foo.two", String.class));
        assertFalse(config.getOptionalValue("foo.three", String.class).isPresent());
        assertEquals("v4", config.getValue("foo.four", String.class));
    }

    @Test
    void overriddenProfile() {
        System.setProperty("quarkus.profile", "foo");
        try {
            final SmallRyeConfig config = buildConfig(
                    "foo.one", "v1",
                    "foo.two", "v2",
                    "%foo.foo.three", "f1",
                    "%prod.foo.four", "v4");
            assertEquals("v1", config.getValue("foo.one", String.class));
            assertEquals("v2", config.getValue("foo.two", String.class));
            assertEquals("f1", config.getValue("foo.three", String.class));
            assertFalse(config.getOptionalValue("foo.four", String.class).isPresent());
        } finally {
            System.clearProperty("quarkus.profile");
        }
    }

    @Test
    void overriddenProfileHigherOrdinal() {
        System.setProperty("quarkus-profile", "foo");
        try {
            final SmallRyeConfig config = configBuilder()
                    .withSources(new PropertiesConfigSource(maps("foo", "default"), "source", 1000))
                    .withSources(new PropertiesConfigSource(maps("%foo.foo", "profile"), "source", 100))
                    .build();

            assertEquals("default", config.getRawValue("foo"));
        } finally {
            System.clearProperty("quarkus-profile");
        }
    }

    @Test
    void profileNoErrorOnExpansion() {
        System.setProperty("quarkus-profile", "foo");
        try {
            final SmallRyeConfig config = configBuilder("foo", "${noExpansionAvailable}", "%foo.foo", "profile").build();

            assertEquals("profile", config.getRawValue("foo"));
        } finally {
            System.clearProperty("quarkus-profile");
        }
    }

    @Test
    public void profile() {
        System.setProperty("quarkus-profile", "prof");
        try {
            final SmallRyeConfig config = buildConfig("my.prop", "1", "%prof.my.prop", "2");

            assertEquals("2", config.getValue("my.prop", String.class));

            assertEquals("my.prop", config.getConfigValue("my.prop").getName());
            assertEquals("my.prop", config.getConfigValue("%prof.my.prop").getName());
        } finally {
            System.clearProperty("quarkus-profile");
        }
    }

    @Test
    public void profileOnly() {
        System.setProperty("quarkus-profile", "prof");
        try {
            final SmallRyeConfig config = buildConfig("my.prop", "1", "%prof.my.prop", "2");

            assertEquals("2", config.getRawValue("my.prop"));
        } finally {
            System.clearProperty("quarkus-profile");
        }
    }

    @Test
    public void fallback() {
        System.setProperty("quarkus-profile", "prof");
        try {
            final SmallRyeConfig config = buildConfig("my.prop", "1");

            assertEquals("1", config.getRawValue("my.prop"));
        } finally {
            System.clearProperty("quarkus-profile");
        }
    }

    @Test
    public void expressions() {
        System.setProperty("quarkus-profile", "prof");
        System.setProperty("my.prop", "1");
        try {
            final SmallRyeConfig config = buildConfig("my.prop", "1", "%prof.my.prop", "${my.prop}");

            assertThrows(IllegalArgumentException.class, () -> config.getRawValue("my.prop"));
        } finally {
            System.clearProperty("quarkus-profile");
            System.clearProperty("my.prop");
        }
    }

    @Test
    public void profileExpressions() {
        System.setProperty("quarkus-profile", "prof");
        System.setProperty("%prof.my.prop.profile", "2");
        try {
            final SmallRyeConfig config = buildConfig("my.prop", "1",
                    "%prof.my.prop", "${%prof.my.prop.profile}",
                    "%prof.my.prop.profile", "2");

            assertEquals("2", config.getRawValue("my.prop"));
        } finally {
            System.clearProperty("quarkus-profile");
            System.clearProperty("%prof.my.prop.profile");
        }
    }

    @Test
    public void customConfigProfile() {
        System.setProperty("quarkus-profile", "prof");
        try {
            final SmallRyeConfig config = configBuilder()
                    .addDefaultSources()
                    .withSources(new PropertiesConfigSource(maps("my.prop", "1", "%prof.my.prop", "2"), "test", 100))
                    .build();

            assertEquals("2", config.getValue("my.prop", String.class));
        } finally {
            System.clearProperty("quarkus-profile");
        }
    }

    @Test
    public void noConfigProfile() {
        final SmallRyeConfig config = configBuilder()
                .addDefaultSources()
                .withSources(new PropertiesConfigSource(maps("my.prop", "1", "%prof.my.prop", "2"), "test", 100))
                .withInterceptors(
                        new ProfileConfigSourceInterceptor("prof"),
                        new ExpressionConfigSourceInterceptor())
                .build();

        assertEquals("2", config.getRawValue("my.prop"));
    }

    @Test
    public void priorityProfile() {
        System.setProperty("quarkus-profile", "prof");
        try {
            final SmallRyeConfig config = configBuilder()
                    .addDefaultSources()
                    .withSources(new PropertiesConfigSource(maps("%prof.my.prop", "higher-profile"), "higher", 200))
                    .withSources(new PropertiesConfigSource(maps("my.prop", "lower", "%prof.my.prop", "lower-profile"), "lower",
                            100))
                    .build();

            assertEquals("higher-profile", config.getRawValue("my.prop"));
        } finally {
            System.clearProperty("quarkus-profile");
        }
    }

    @Test
    public void priorityOverrideProfile() {
        System.setProperty("quarkus-profile", "prof");
        try {
            final SmallRyeConfig config = new SmallRyeConfigBuilder()
                    .addDefaultSources()
                    .withSources(new PropertiesConfigSource(maps("my.prop", "higher"), "higher", 200))
                    .withSources(new PropertiesConfigSource(maps("my.prop", "lower", "%prof.my.prop", "lower-profile"), "lower",
                            100))
                    .build();

            assertEquals("higher", config.getRawValue("my.prop"));
        } finally {
            System.clearProperty("quarkus-profile");
        }
    }

    @Test
    public void priorityProfileOverOriginal() {
        System.setProperty("quarkus-profile", "prof");
        try {
            final SmallRyeConfig config = configBuilder()
                    .addDefaultSources()
                    .withSources(new PropertiesConfigSource(maps("my.prop", "higher", "%prof.my.prop", "higher-profile"),
                            "higher", 200))
                    .withSources(new PropertiesConfigSource(maps("my.prop", "lower", "%prof.my.prop", "lower-profile"), "lower",
                            100))
                    .build();

            assertEquals("higher-profile", config.getRawValue("my.prop"));
        } finally {
            System.clearProperty("quarkus-profile");
        }
    }

    @Test
    public void propertyNames() {
        System.setProperty("quarkus-profile", "prof");
        try {
            final SmallRyeConfig config = buildConfig("my.prop", "1", "%prof.my.prop", "2", "%prof.prof.only", "1");

            assertEquals("2", config.getRawValue("my.prop"));
            assertEquals("1", config.getRawValue("prof.only"));

            final List<String> properties = StreamSupport.stream(config.getPropertyNames().spliterator(), false)
                    .collect(toList());
            assertFalse(properties.contains("%prof.my.prop")); // We are removing profile properties in SmallRyeConfig and keep only the main name.
            assertTrue(properties.contains("my.prop"));
            assertTrue(properties.contains("prof.only"));
        } finally {
            System.clearProperty("quarkus-profile");
        }
    }
}
