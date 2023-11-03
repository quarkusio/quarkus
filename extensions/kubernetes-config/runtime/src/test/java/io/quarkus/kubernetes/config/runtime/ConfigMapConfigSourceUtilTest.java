package io.quarkus.kubernetes.config.runtime;

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

        List<ConfigSource> configSources = sut.toConfigSources(configMap.getMetadata(), configMap.getData(), 0);

        assertThat(configSources).isEmpty();
    }

    @Test
    void testOnlyLiteralData() {
        ConfigMap configMap = configMapBuilder("testOnlyLiteralData")
                .addToData("some.key", "someValue").addToData("some.other", "someOtherValue").build();

        List<ConfigSource> configSources = sut.toConfigSources(configMap.getMetadata(), configMap.getData(), 0);

        assertThat(configSources).singleElement().satisfies(c -> {
            assertThat(c.getProperties()).containsOnly(entry("some.key", "someValue"),
                    entry("some.other", "someOtherValue"));
            assertThat(c.getName()).contains("testOnlyLiteralData");
            assertThat(c.getName()).isEqualTo(
                    "ConfigMapLiteralDataPropertiesConfigSource[configMap=namespace/testOnlyLiteralData/uid/version]");
        });
    }

    @Test
    void testOnlySingleMatchingPropertiesData() {
        ConfigMap configMap = configMapBuilder("testOnlySingleMatchingPropertiesData")
                .addToData("application.properties", "key1=value1\nkey2=value2\nsome.key=someValue").build();

        List<ConfigSource> configSources = sut.toConfigSources(configMap.getMetadata(), configMap.getData(), 0);

        assertThat(configSources).singleElement().satisfies(c -> {
            assertThat(c.getProperties()).containsOnly(entry("key1", "value1"), entry("key2", "value2"),
                    entry("some.key", "someValue"));
            assertThat(c.getName()).contains("testOnlySingleMatchingPropertiesData");
            assertThat(c.getOrdinal()).isEqualTo(270);
        });
    }

    @Test
    void testOnlySingleNonMatchingPropertiesData() {
        ConfigMap configMap = configMapBuilder("testOnlySingleMatchingPropertiesData")
                .addToData("app.properties", "key1=value1\nkey2=value2\nsome.key=someValue").build();

        List<ConfigSource> configSources = sut.toConfigSources(configMap.getMetadata(), configMap.getData(), 0);

        assertThat(configSources).isNotEmpty();
    }

    @Test
    void testOnlySingleMatchingYamlData() {
        ConfigMap configMap = configMapBuilder("testOnlySingleMatchingYamlData")
                .addToData("application.yaml", "key1: value1\nkey2: value2\nsome:\n  key: someValue").build();

        List<ConfigSource> configSources = sut.toConfigSources(configMap.getMetadata(), configMap.getData(), 0);

        assertThat(configSources).singleElement().satisfies(c -> {
            assertThat(c.getProperties()).containsOnly(entry("key1", "value1"), entry("key2", "value2"),
                    entry("some.key", "someValue"));
            assertThat(c.getName()).contains("testOnlySingleMatchingYamlData");
        });
    }

    @Test
    void testOnlySingleNonMatchingYamlData() {
        ConfigMap configMap = configMapBuilder("testOnlySingleMatchingPropertiesData")
                .addToData("app.yaml", "key1: value1\nkey2: value2\nsome:\n  key: someValue").build();

        List<ConfigSource> configSources = sut.toConfigSources(configMap.getMetadata(), configMap.getData(), 0);

        assertThat(configSources).isNotEmpty();
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

        List<ConfigSource> configSources = sut.toConfigSources(configMap.getMetadata(), configMap.getData(), 0);

        assertThat(configSources).hasSize(4);

        assertThat(configSources.get(0).getClass().getName().contains("ConfigMapLiteralDataPropertiesConfigSource")).isTrue();

        assertThat(configSources).filteredOn(c -> !c.getName().toLowerCase().contains("application"))
                .hasOnlyOneElementSatisfying(c -> {
                    assertThat(c.getProperties()).containsOnly(
                            entry("some.key", "someValue"),
                            entry("app.properties", "ignored1=ignoredValue1"),
                            entry("app.yaml", "ignored2: ignoredValue2"),
                            entry("app.yml", "ignored3: ignoredValue3"));
                });

        assertThat(configSources).filteredOn(c -> c.getName().toLowerCase().contains("application.properties"))
                .singleElement().satisfies(c -> {
                    assertThat(c.getProperties()).containsOnly(entry("key1", "value1"), entry("app.key", "val"));
                });
        assertThat(configSources).filteredOn(c -> c.getName().toLowerCase().contains("application.yaml"))
                .singleElement().satisfies(c -> {
                    assertThat(c.getProperties()).containsOnly(entry("key2", "value2"),
                            entry("some.otherKey", "someOtherValue"));
                });
        assertThat(configSources).filteredOn(c -> c.getName().toLowerCase().contains("application.yml"))
                .singleElement().satisfies(c -> {
                    assertThat(c.getProperties()).containsOnly(entry("key3", "value3"));
                });
    }

    private ConfigMapBuilder configMapBuilder(String name) {
        return new ConfigMapBuilder().withNewMetadata()
                .withName(name).withNamespace("namespace").withUid("uid")
                .withResourceVersion("version").endMetadata();
    }

}
