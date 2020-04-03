package io.quarkus.kubernetes.client.runtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

import java.util.List;

import org.eclipse.microprofile.config.spi.ConfigSource;
import org.junit.jupiter.api.Test;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ConfigMapBuilder;

class ConfigMapConfigSourceUtilTest {

    ConfigMapConfigSourceUtil sut = new ConfigMapConfigSourceUtil();

    @Test
    void testEmptyData() {
        ConfigMap configMap = configMapBuilder("testEmptyData").build();

        List<ConfigSource> configSources = sut.toConfigSources(configMap.getMetadata().getName(), configMap.getData());

        assertThat(configSources).isEmpty();
    }

    @Test
    void testOnlyLiteralData() {
        ConfigMap configMap = configMapBuilder("testOnlyLiteralData")
                .addToData("some.key", "someValue").addToData("some.other", "someOtherValue").build();

        List<ConfigSource> configSources = sut.toConfigSources(configMap.getMetadata().getName(), configMap.getData());

        assertThat(configSources).hasOnlyOneElementSatisfying(c -> {
            assertThat(c.getProperties()).containsOnly(entry("some.key", "someValue"),
                    entry("some.other", "someOtherValue"));
            assertThat(c.getName()).contains("testOnlyLiteralData");
        });
    }

    @Test
    void testOnlySingleMatchingPropertiesData() {
        ConfigMap configMap = configMapBuilder("testOnlySingleMatchingPropertiesData")
                .addToData("application.properties", "key1=value1\nkey2=value2\nsome.key=someValue").build();

        List<ConfigSource> configSources = sut.toConfigSources(configMap.getMetadata().getName(), configMap.getData());

        assertThat(configSources).hasOnlyOneElementSatisfying(c -> {
            assertThat(c.getProperties()).containsOnly(entry("key1", "value1"), entry("key2", "value2"),
                    entry("some.key", "someValue"));
            assertThat(c.getName()).contains("testOnlySingleMatchingPropertiesData");
        });
    }

    @Test
    void testOnlySingleNonMatchingPropertiesData() {
        ConfigMap configMap = configMapBuilder("testOnlySingleMatchingPropertiesData")
                .addToData("app.properties", "key1=value1\nkey2=value2\nsome.key=someValue").build();

        List<ConfigSource> configSources = sut.toConfigSources(configMap.getMetadata().getName(), configMap.getData());

        assertThat(configSources).isEmpty();
    }

    @Test
    void testOnlySingleMatchingYamlData() {
        ConfigMap configMap = configMapBuilder("testOnlySingleMatchingYamlData")
                .addToData("application.yaml", "key1: value1\nkey2: value2\nsome:\n  key: someValue").build();

        List<ConfigSource> configSources = sut.toConfigSources(configMap.getMetadata().getName(), configMap.getData());

        assertThat(configSources).hasOnlyOneElementSatisfying(c -> {
            assertThat(c.getProperties()).containsOnly(entry("key1", "value1"), entry("key2", "value2"),
                    entry("some.key", "someValue"));
            assertThat(c.getName()).contains("testOnlySingleMatchingYamlData");
        });
    }

    @Test
    void testOnlySingleNonMatchingYamlData() {
        ConfigMap configMap = configMapBuilder("testOnlySingleMatchingPropertiesData")
                .addToData("app.yaml", "key1: value1\nkey2: value2\nsome:\n  key: someValue").build();

        List<ConfigSource> configSources = sut.toConfigSources(configMap.getMetadata().getName(), configMap.getData());

        assertThat(configSources).isEmpty();
    }

    @Test
    void testWithAllKindsOfData() {
        ConfigMap configMap = configMapBuilder("testWithAllKindsOfData")
                .addToData("some.key", "someValue")
                .addToData("application.properties", "key1=value1\napp.key=val")
                .addToData("app.properties", "ignored1=ignoredValue1")
                .addToData("application.yaml", "key2: value2\nsome:\n  otherKey: someOtherValue")
                .addToData("app.yaml", "ignored2: ignoredValue2")
                .addToData("application.yml", "key3: value3")
                .addToData("app.yml", "ignored3: ignoredValue3")
                .build();

        List<ConfigSource> configSources = sut.toConfigSources(configMap.getMetadata().getName(), configMap.getData());

        assertThat(configSources).hasSize(4);
        assertThat(configSources).filteredOn(c -> c.getName().toLowerCase().contains("literal"))
                .hasOnlyOneElementSatisfying(c -> {
                    assertThat(c.getProperties()).containsOnly(entry("some.key", "someValue"));
                });
        assertThat(configSources).filteredOn(c -> c.getName().toLowerCase().contains("application.properties"))
                .hasOnlyOneElementSatisfying(c -> {
                    assertThat(c.getProperties()).containsOnly(entry("key1", "value1"), entry("app.key", "val"));
                });
        assertThat(configSources).filteredOn(c -> c.getName().toLowerCase().contains("application.yaml"))
                .hasOnlyOneElementSatisfying(c -> {
                    assertThat(c.getProperties()).containsOnly(entry("key2", "value2"),
                            entry("some.otherKey", "someOtherValue"));
                });
        assertThat(configSources).filteredOn(c -> c.getName().toLowerCase().contains("application.yml"))
                .hasOnlyOneElementSatisfying(c -> {
                    assertThat(c.getProperties()).containsOnly(entry("key3", "value3"));
                });
    }

    private ConfigMapBuilder configMapBuilder(String name) {
        return new ConfigMapBuilder().withNewMetadata()
                .withName(name).endMetadata();
    }

}
