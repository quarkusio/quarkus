package io.quarkus.runtime.configuration;

import static java.util.Map.entry;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;
import org.eclipse.microprofile.config.spi.ConfigSource;
import org.jboss.logmanager.Level;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.quarkus.runtime.logging.LogConfig;
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

            entry("quarkus.named.value", "val"),

            entry("quarkus.named2.value", "val1"),
            entry("quarkus.named2.group.value", "val2"),

            entry("quarkus.named3.value", "val3"),
            entry("quarkus.named3.group.value", "val4"),
            entry("quarkus.named3.map-of-groups.foo.value", "val5"),
            entry("quarkus.named3.map-of-groups.bar.value", "val6"),

            entry("quarkus.named4", "val7"));

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
    public void handleLogConfig() {
        LogConfig logConfig = new LogConfig();
        ConfigInstantiator.handleObject(logConfig);

        assertThat(logConfig.level).isEqualTo(Level.INFO);
        assertThat(logConfig.categories).hasSize(2);
        // note: category assertions are a bit awkward because most fields and classes are just package visible
        // (level.level selects the actual level member of InheritableLevel.ActualLevel)
        assertThat(logConfig.categories.get("foo.bar"))
                .hasFieldOrPropertyWithValue("level.level", Level.DEBUG);
        assertThat(logConfig.categories.get("baz"))
                .hasFieldOrPropertyWithValue("level.level", Level.TRACE);
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

    @Test
    public void handleWithoutConfigSuffix() {
        RootWithoutConfigSuffix config = new RootWithoutConfigSuffix();
        ConfigInstantiator.handleObject(config);
        assertThat(config.value).isEqualTo("val1");
        assertThat(config.group.value).isEqualTo("val2");
    }

    // Not adding @ConfigItem to properties feels wrong, but it's supported,
    // so ConfigInstantiator should support it too.
    @Test
    public void handleWithoutConfigItem() {
        RootWithoutConfigItem config = new RootWithoutConfigItem();
        ConfigInstantiator.handleObject(config);
        assertThat(config.value).isEqualTo("val3");
        assertThat(config.group.value).isEqualTo("val4");
        assertThat(config.mapOfGroups).containsKeys("foo", "bar");
        assertThat(config.mapOfGroups.get("foo").value).isEqualTo("val5");
        assertThat(config.mapOfGroups.get("bar").value).isEqualTo("val6");
    }

    // Not adding @ConfigItem to properties feels wrong, but it's supported,
    // so ConfigInstantiator should support it too.
    @Test
    public void handleElementNameParent() {
        RootWithElementNameParent config = new RootWithElementNameParent();
        ConfigInstantiator.handleObject(config);
        assertThat(config.value).isEqualTo("val7");
    }

    @Test
    public void createEmptyObjectWithConfigIgnored() {
        LogConfig logConfig = ConfigInstantiator.createEmptyObject(LogConfig.class);

        // On contrary to the test "handleLogConfig" above,
        // here we expect configuration to be ignored:
        // we only want the defaults.
        // This is arguably more useful for config groups than for config roots.
        assertThat(logConfig.level).isEqualTo(Level.INFO);
        assertThat(logConfig.categories).isEmpty();
    }

    @Test
    public void createEmptyObjectWithDefaults() {
        WithDefaultsConfig config = ConfigInstantiator.createEmptyObject(WithDefaultsConfig.class);

        assertThat(config.noDefaultStringValue).isNull();
        assertThat(config.withDefaultStringValue).isEqualTo("someDefault");
        assertThat(config.noDefaultConvertedValue).isNull();
        assertThat(config.withDefaultConvertedValue).isEqualTo(42);
        assertThat(config.optionalStringValue).isNotNull().isEmpty();
        assertThat(config.optionalConvertedValue).isNotNull().isEmpty();
        assertThat(config.listOfStringsValue).isNull();
        assertThat(config.listOfConvertedValue).isNull();
        assertThat(config.mapOfStringsValue).isNotNull().isEmpty();
        assertThat(config.mapOfGroupsValue).isNotNull().isEmpty();
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

    @ConfigRoot(name = "named2")
    private static class RootWithoutConfigSuffix {

        @ConfigItem
        public String value;

        @ConfigItem
        public GroupWithoutConfigSuffix group;
    }

    @ConfigGroup
    private static class GroupWithoutConfigSuffix {

        public GroupWithoutConfigSuffix() {
        }

        @ConfigItem
        public String value;
    }

    @ConfigRoot(name = "named3")
    private static class RootWithoutConfigItem {
        @ConfigItem
        public String value;

        public GroupInRootWithoutConfigItem group;

        public Map<String, GroupInRootWithoutConfigItem> mapOfGroups;
    }

    @ConfigGroup
    private static class GroupInRootWithoutConfigItem {
        public GroupInRootWithoutConfigItem() {
        }

        @ConfigItem
        public String value;
    }

    @ConfigRoot(name = "named4")
    private static class RootWithElementNameParent {
        @ConfigItem(name = ConfigItem.PARENT)
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

    @ConfigGroup
    public static class WithDefaultsConfig {

        @ConfigItem
        public String noDefaultStringValue;

        @ConfigItem(defaultValue = "someDefault")
        public String withDefaultStringValue;

        @ConfigItem
        public Integer noDefaultConvertedValue;

        @ConfigItem(defaultValue = "42")
        public Integer withDefaultConvertedValue;

        @ConfigItem
        public Optional<String> optionalStringValue;

        @ConfigItem
        public Optional<Integer> optionalConvertedValue;

        @ConfigItem
        public List<String> listOfStringsValue;

        @ConfigItem
        public List<Integer> listOfConvertedValue;

        @ConfigItem
        public Map<String, String> mapOfStringsValue;

        @ConfigItem
        public Map<String, MapValueConfig> mapOfGroupsValue;
    }
}
