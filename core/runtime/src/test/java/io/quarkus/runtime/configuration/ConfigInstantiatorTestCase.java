package io.quarkus.runtime.configuration;

import static java.util.Map.entry;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;
import org.eclipse.microprofile.config.spi.ConfigSource;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.SmallRyeConfigBuilder;

/**
 * Tests {@link ConfigInstantiator} with a small test config.
 */
public class ConfigInstantiatorTestCase {

    private static final Map<String, String> TEST_CONFIG_MAP = Map.ofEntries(
            entry("quarkus.log.category.\"foo.bar\".level", "DEBUG"),
            entry("quarkus.log.category.baz.level", "TRACE"),

            entry("quarkus.map-of-maps.map-of-string-maps.outer1.inner1", "o1i1"),
            entry("quarkus.map-of-maps.map-of-string-maps.outer1.inner2", "o1i2"),
            entry("quarkus.map-of-maps.map-of-string-maps.\"outer2.key\".inner1", "o2i1"),
            entry("quarkus.map-of-maps.map-of-string-maps.\"outer2.key\".\"inner2.key\"", "o2i2"),

            entry("quarkus.map-of-maps.map-of-maps.outer1.inner1.value", "o1i1"),
            entry("quarkus.map-of-maps.map-of-maps.outer1.inner2.value", "o1i2"),
            entry("quarkus.map-of-maps.map-of-maps.\"outer2.key\".inner1.value", "o2i1"),
            entry("quarkus.map-of-maps.map-of-maps.\"outer2.key\".\"inner2.key\".value", "o2i2"),

            entry("quarkus.named.value", "val"));

    private static Config testConfig;
    private static Config cfgToRestore;

    @BeforeAll
    static void registerTestConfig() {
        var localTestConfig = new SmallRyeConfigBuilder()
                .addDiscoveredConverters()
                .withSources(new TestConfigSource())
                .build();

        var cfgProviderResolver = ConfigProviderResolver.instance();
        try {
            cfgProviderResolver.registerConfig(localTestConfig, Thread.currentThread().getContextClassLoader());
        } catch (IllegalStateException e) { // a config is already registered; remember for later restoration
            cfgToRestore = cfgProviderResolver.getConfig();
            cfgProviderResolver.releaseConfig(cfgToRestore);
            cfgProviderResolver.registerConfig(localTestConfig, Thread.currentThread().getContextClassLoader());
        }
        testConfig = localTestConfig;
    }

    @AfterAll
    static void releaseTestConfig() {
        var cfgProviderResolver = ConfigProviderResolver.instance();
        if (testConfig != null) {
            cfgProviderResolver.releaseConfig(testConfig);
            if (cfgToRestore != null) {
                cfgProviderResolver.registerConfig(cfgToRestore, Thread.currentThread().getContextClassLoader());
            }
        }
    }

    @Test
    public void handleMapOfMapConfig() {
        MapOfMapsConfig mapOfMapsConfig = new MapOfMapsConfig();
        ConfigInstantiator.handleObject(mapOfMapsConfig);

        assertThat(mapOfMapsConfig.mapOfStringMaps).hasSize(2);
        assertThat(mapOfMapsConfig.mapOfStringMaps.get("outer1"))
                .isEqualTo(Map.of("inner1", "o1i1", "inner2", "o1i2"));
        assertThat(mapOfMapsConfig.mapOfStringMaps.get("outer2.key"))
                .isEqualTo(Map.of("inner1", "o2i1", "inner2.key", "o2i2"));

        assertThat(mapOfMapsConfig.mapOfMaps).hasSize(2);
        assertThat(mapOfMapsConfig.mapOfMaps.get("outer1"))
                .isEqualTo(Map.of("inner1", new MapValueConfig("o1i1"), "inner2", new MapValueConfig("o1i2")));
        assertThat(mapOfMapsConfig.mapOfMaps.get("outer2.key"))
                .isEqualTo(Map.of("inner1", new MapValueConfig("o2i1"), "inner2.key", new MapValueConfig("o2i2")));
    }

    @Test
    public void handleWithNameConfig() {
        WithNameConfig config = new WithNameConfig();
        ConfigInstantiator.handleObject(config);

        assertThat(config.value).isEqualTo("val");
    }

    private static class MapOfMapsConfig {

        @ConfigItem
        public Map<String, Map<String, String>> mapOfStringMaps;

        @ConfigItem
        public Map<String, Map<String, MapValueConfig>> mapOfMaps;
    }

    @ConfigGroup
    static class MapValueConfig {

        @ConfigItem
        public String value;

        // value constructor, equals (hashCode) and toString for easy testing:

        public MapValueConfig() {
        }

        MapValueConfig(String value) {
            this.value = value;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            MapValueConfig other = (MapValueConfig) obj;
            return Objects.equals(value, other.value);
        }

        @Override
        public int hashCode() {
            return Objects.hash(value);
        }

        @Override
        public String toString() {
            return String.format("MapValueConfig[%s]", value);
        }
    }

    @ConfigRoot(name = "named")
    private static class WithNameConfig {

        @ConfigItem
        public String value;
    }

    private static class TestConfigSource implements ConfigSource {

        public Map<String, String> getProperties() {
            return TEST_CONFIG_MAP;
        }

        public Set<String> getPropertyNames() {
            return TEST_CONFIG_MAP.keySet();
        }

        public String getValue(final String propertyName) {
            return TEST_CONFIG_MAP.get(propertyName);
        }

        public String getName() {
            return "ConfigInstantiatorTestCase config source";
        }
    }
}
