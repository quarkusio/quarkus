package io.quarkus.vertx.http.runtime.devmode;

import java.util.List;
import java.util.Map;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.smallrye.config.PropertiesConfigSource;

public class ConfigDescriptionsManagerUnitTest {

    public static final String DATASOURCE_JDBC_URL = "quarkus.datasource.jdbc.url";
    public static final String MAP = "Map Config Source";
    public static final String UNRELATED = "unrelated";
    public static final String UNRELATED_VALUE = "unrelated-value";
    public static final String WILDCARD_QUARKUS_DATASOURCE_JDBC_URL = "quarkus.datasource.{*}.jdbc.url";
    public static final String WILDCARD_QUARKUS_DATASOURCE_JDBC_MIN_SIZE = "quarkus.datasource.{*}.jdbc.min-size";

    @Test
    public void testBasicFunction() {
        try (var handle = setupConfig(Map.of())) {
            ConfigDescriptionsManager manager = new ConfigDescriptionsManager(List.of(desc(DATASOURCE_JDBC_URL, "The URL")));
            ConfigDescription configDescription = find(manager, DATASOURCE_JDBC_URL);
            Assertions.assertEquals(DATASOURCE_JDBC_URL, configDescription.getName());
            Assertions.assertEquals("The URL", configDescription.getDescription());
            Assertions.assertNull(configDescription.getConfigValue().getValue());
            Assertions.assertNull(configDescription.getConfigValue().getConfigSourceName());
        }

        try (var handle = setupConfig(Map.of(DATASOURCE_JDBC_URL, "jdbc:test", UNRELATED, UNRELATED_VALUE))) {
            ConfigDescriptionsManager manager = new ConfigDescriptionsManager(List.of(desc(DATASOURCE_JDBC_URL, "The URL")));
            ConfigDescription configDescription = find(manager, DATASOURCE_JDBC_URL);
            Assertions.assertEquals(DATASOURCE_JDBC_URL, configDescription.getName());
            Assertions.assertEquals("The URL", configDescription.getDescription());
            Assertions.assertEquals("jdbc:test", configDescription.getConfigValue().getValue());
            Assertions.assertTrue(configDescription.getConfigValue().getSourceName().contains("Map"));

            configDescription = find(manager, UNRELATED);
            Assertions.assertEquals(UNRELATED, configDescription.getName());
            Assertions.assertNull(configDescription.getDescription());
            Assertions.assertEquals(UNRELATED_VALUE, configDescription.getConfigValue().getValue());
            Assertions.assertTrue(configDescription.getConfigValue().getSourceName().contains("Map"));
        }
    }

    @Test
    public void testWildcardWithNoProperties() {
        try (var handle = setupConfig(Map.of())) {
            ConfigDescriptionsManager manager = new ConfigDescriptionsManager(
                    List.of(desc(DATASOURCE_JDBC_URL, "The URL"), desc(WILDCARD_QUARKUS_DATASOURCE_JDBC_URL, "The named URL")));
            ConfigDescription configDescription = find(manager, DATASOURCE_JDBC_URL);
            Assertions.assertEquals(DATASOURCE_JDBC_URL, configDescription.getName());
            Assertions.assertEquals("The URL", configDescription.getDescription());
            Assertions.assertNull(configDescription.getConfigValue().getValue());
            Assertions.assertNull(configDescription.getConfigValue().getConfigSourceName());

            //wildcards should not show up in the result directly
            Assertions.assertNull(find(manager, WILDCARD_QUARKUS_DATASOURCE_JDBC_URL));

            configDescription = find(manager, "quarkus.datasource.");
            Assertions.assertTrue(configDescription.isWildcardEntry());
            Assertions.assertNull(configDescription.getDescription());
            Assertions.assertNull(configDescription.getConfigValue().getValue());
            Assertions.assertNull(configDescription.getConfigValue().getConfigSourceName());
        }

    }

    @Test
    public void testQuotedWildcardExpansion() {
        //name is quoted in the config
        try (var handle = setupConfig(Map.of("quarkus.datasource.\"test\".jdbc.url", "jdbc:named-test"))) {
            ConfigDescriptionsManager manager = new ConfigDescriptionsManager(
                    List.of(desc(DATASOURCE_JDBC_URL, "The URL"), desc(WILDCARD_QUARKUS_DATASOURCE_JDBC_URL, "The named URL"),
                            desc(WILDCARD_QUARKUS_DATASOURCE_JDBC_MIN_SIZE, "The named min size")));
            ConfigDescription configDescription = find(manager, "quarkus.datasource.\"test\".jdbc.url");
            Assertions.assertEquals("The named URL", configDescription.getDescription());
            Assertions.assertEquals("jdbc:named-test", configDescription.getConfigValue().getValue());
            Assertions.assertTrue(configDescription.getConfigValue().getSourceName().contains("Map"));

            Assertions.assertNull(find(manager, "quarkus.datasource.test.jdbc.url"));

            configDescription = find(manager, "quarkus.datasource.\"test\".jdbc.min-size");
            Assertions.assertFalse(configDescription.isWildcardEntry());
            Assertions.assertEquals("The named min size", configDescription.getDescription());
            Assertions.assertNull(configDescription.getConfigValue().getValue());
            Assertions.assertNull(configDescription.getConfigValue().getConfigSourceName());
        }
    }

    @Test
    public void testWildcardExpansionNoQuotes() {
        //name is quoted in the config
        try (var handle = setupConfig(Map.of("quarkus.datasource.test.jdbc.url", "jdbc:named-test"))) {
            ConfigDescriptionsManager manager = new ConfigDescriptionsManager(
                    List.of(desc(DATASOURCE_JDBC_URL, "The URL"), desc(WILDCARD_QUARKUS_DATASOURCE_JDBC_URL, "The named URL"),
                            desc(WILDCARD_QUARKUS_DATASOURCE_JDBC_MIN_SIZE, "The named min size")));
            //for config defined values we always want to match the one defined in the config
            ConfigDescription configDescription = find(manager, "quarkus.datasource.test.jdbc.url");
            Assertions.assertEquals("The named URL", configDescription.getDescription());
            Assertions.assertEquals("jdbc:named-test", configDescription.getConfigValue().getValue());
            Assertions.assertTrue(configDescription.getConfigValue().getSourceName().contains("Map"));

            Assertions.assertNull(find(manager, "quarkus.datasource.\"test\".jdbc.url"));

            //no quotes, as the defining property does not have quotes
            configDescription = find(manager, "quarkus.datasource.test.jdbc.min-size");
            Assertions.assertFalse(configDescription.isWildcardEntry());
            Assertions.assertEquals("The named min size", configDescription.getDescription());
            Assertions.assertNull(configDescription.getConfigValue().getValue());
            Assertions.assertNull(configDescription.getConfigValue().getConfigSourceName());
        }
    }

    @Test
    public void testWildcardExpansionMatchingProperty() {
        //name is quoted in the config
        try (var handle = setupConfig(Map.of("quarkus.datasource.jdbc.url", "jdbc:test"))) {
            ConfigDescriptionsManager manager = new ConfigDescriptionsManager(
                    List.of(desc(DATASOURCE_JDBC_URL, "The URL"), desc(WILDCARD_QUARKUS_DATASOURCE_JDBC_URL, "The named URL"),
                            desc(WILDCARD_QUARKUS_DATASOURCE_JDBC_MIN_SIZE, "The named min size")));
            // 'jdbc' is part of an existing config key, not a named datasource
            Assertions.assertNull(find(manager, "quarkus.datasource.jdbc.jdbc.url"));
            Assertions.assertNull(find(manager, "quarkus.datasource.jdbc.jdbc.min-size"));
        }
        try (var handle = setupConfig(Map.of("quarkus.datasource.jdbc.url", "jdbc:test"))) {
            ConfigDescriptionsManager manager = new ConfigDescriptionsManager(
                    List.of(desc(WILDCARD_QUARKUS_DATASOURCE_JDBC_URL, "The named URL"),
                            desc(WILDCARD_QUARKUS_DATASOURCE_JDBC_MIN_SIZE, "The named min size")));
            // just to be paranoid we run it again but without defining jdbc as a config key, and make sure the test is valid
            Assertions.assertNotNull(find(manager, "quarkus.datasource.jdbc.jdbc.url"));
            Assertions.assertNotNull(find(manager, "quarkus.datasource.jdbc.jdbc.min-size"));
        }
    }

    @Test
    public void testQuotedWildcardExpansionMatchingProperties() {
        //name is quoted in the config
        try (var handle = setupConfig(Map.of("quarkus.datasource.jdbc.url", "jdbc:test", "quarkus.datasource.\"jdbc\".jdbc.url",
                "jdbc:named-test"))) {
            ConfigDescriptionsManager manager = new ConfigDescriptionsManager(
                    List.of(desc(DATASOURCE_JDBC_URL, "The URL"), desc(WILDCARD_QUARKUS_DATASOURCE_JDBC_URL, "The named URL"),
                            desc(WILDCARD_QUARKUS_DATASOURCE_JDBC_MIN_SIZE, "The named min size")));
            ConfigDescription configDescription = find(manager, "quarkus.datasource.\"jdbc\".jdbc.url");
            Assertions.assertEquals("The named URL", configDescription.getDescription());
            Assertions.assertEquals("jdbc:named-test", configDescription.getConfigValue().getValue());
            Assertions.assertTrue(configDescription.getConfigValue().getSourceName().contains("Map"));

            Assertions.assertNotNull(find(manager, "quarkus.datasource.jdbc.url"));

            configDescription = find(manager, "quarkus.datasource.\"jdbc\".jdbc.min-size");
            Assertions.assertFalse(configDescription.isWildcardEntry());
            Assertions.assertEquals("The named min size", configDescription.getDescription());
            Assertions.assertNull(configDescription.getConfigValue().getValue());
            Assertions.assertNull(configDescription.getConfigValue().getConfigSourceName());
        }
    }

    /**
     * Make sure a config key with a quoted dot works
     */
    @Test
    public void testQuotedDot() {
        //dot is quoted in the config
        char[] quotedDot = { '\"', '.', '\"' };

        String configWithQuotedDotIn = "bla.bla." + String.valueOf(quotedDot);
        try (var handle = setupConfig(Map.of(configWithQuotedDotIn, "foo"))) {
            ConfigDescriptionsManager manager = new ConfigDescriptionsManager();
            ConfigDescription configDescription = find(manager, configWithQuotedDotIn);
            Assertions.assertNotNull(configDescription);
            Assertions.assertEquals(configWithQuotedDotIn, configDescription.getName());
            Assertions.assertEquals("foo", configDescription.getConfigValue().getRawValue());
        }
    }

    //this happens with dev services
    //make sure that we use the quoted one only
    @Test
    public void testTwoUrlsDefined() {
        //name is quoted in the config
        try (var handle = setupConfig(
                Map.of("quarkus.datasource.test.jdbc.url", "jdbc:named-test", "quarkus.datasource.\"test\".jdbc.url",
                        "jdbc:named-test"))) {
            ConfigDescriptionsManager manager = new ConfigDescriptionsManager(
                    List.of(desc(DATASOURCE_JDBC_URL, "The URL"), desc(WILDCARD_QUARKUS_DATASOURCE_JDBC_URL, "The named URL"),
                            desc(WILDCARD_QUARKUS_DATASOURCE_JDBC_MIN_SIZE, "The named min size")));
            ConfigDescription configDescription = find(manager, "quarkus.datasource.\"test\".jdbc.url");
            Assertions.assertEquals("The named URL", configDescription.getDescription());
            Assertions.assertEquals("jdbc:named-test", configDescription.getConfigValue().getValue());
            Assertions.assertTrue(configDescription.getConfigValue().getSourceName().contains("Map"));

            //make sure we only expand the quoted version
            Assertions.assertNull(find(manager, "quarkus.datasource.test.jdbc.min-size"));

            configDescription = find(manager, "quarkus.datasource.\"test\".jdbc.min-size");
            Assertions.assertFalse(configDescription.isWildcardEntry());
            Assertions.assertEquals("The named min size", configDescription.getDescription());
            Assertions.assertNull(configDescription.getConfigValue().getValue());
            Assertions.assertNull(configDescription.getConfigValue().getConfigSourceName());
        }
    }

    private ConfigHandle setupConfig(Map<String, String> config) {
        Config cfg = ConfigProviderResolver.instance().getBuilder()
                .withSources(new PropertiesConfigSource(config, MAP, 1))
                .build();
        ConfigProviderResolver.instance().registerConfig(cfg, Thread.currentThread().getContextClassLoader());
        return new ConfigHandle(cfg);
    }

    public ConfigDescription find(ConfigDescriptionsManager manager, String key) {
        Map<ConfigSourceName, List<ConfigDescription>> values = manager.values();
        for (List<ConfigDescription> i : values.values()) {
            for (ConfigDescription j : i) {
                if (j.getName().equals(key)) {
                    return j;
                }
            }
        }
        return null;
    }

    private ConfigDescription desc(String key, String desc) {
        return new ConfigDescription(key, desc, null, false, String.class.getName(), null, null);
    }

    private static class ConfigHandle implements AutoCloseable {
        private final Config cfg;

        public ConfigHandle(Config cfg) {
            this.cfg = cfg;
        }

        @Override
        public void close() {
            ConfigProviderResolver.instance().releaseConfig(cfg);
        }
    }
}
