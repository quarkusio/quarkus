package io.quarkus.kubernetes.config.runtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

import java.util.Base64;
import java.util.List;

import org.eclipse.microprofile.config.spi.ConfigSource;
import org.junit.jupiter.api.Test;

import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;

class SecretConfigSourceUtilTest {

    SecretConfigSourceUtil sut = new SecretConfigSourceUtil();

    @Test
    void testEmptyData() {
        Secret secret = secretMapBuilder("testEmptyData").build();

        List<ConfigSource> configSources = sut.toConfigSources(secret.getMetadata(), secret.getData(), 0);

        assertThat(configSources).isEmpty();
    }

    @Test
    void testOnlyLiteralData() {
        Secret configMap = secretMapBuilder("testOnlyLiteralData")
                .addToData("some.key", encodeValue("someValue")).addToData("some.other", encodeValue("someOtherValue")).build();

        List<ConfigSource> configSources = sut.toConfigSources(configMap.getMetadata(), configMap.getData(), 0);

        assertThat(configSources).singleElement().satisfies(c -> {
            assertThat(c.getProperties()).containsOnly(entry("some.key", "someValue"),
                    entry("some.other", "someOtherValue"));
            assertThat(c.getName()).contains("testOnlyLiteralData");
            assertThat(c.getOrdinal()).isEqualTo(285);
            assertThat(c.getName())
                    .isEqualTo("SecretLiteralDataPropertiesConfigSource[secret=namespace/testOnlyLiteralData/uid/version]");
        });
    }

    @Test
    void testOnlySingleMatchingPropertiesData() {
        Secret secret = secretMapBuilder("testOnlySingleMatchingPropertiesData")
                .addToData("application.properties", encodeValue("key1=value1\nkey2=value2\nsome.key=someValue")).build();

        List<ConfigSource> configSources = sut.toConfigSources(secret.getMetadata(), secret.getData(), 0);

        assertThat(configSources).singleElement().satisfies(c -> {
            assertThat(c.getProperties()).containsOnly(entry("key1", "value1"), entry("key2", "value2"),
                    entry("some.key", "someValue"));
            assertThat(c.getName()).contains("testOnlySingleMatchingPropertiesData");
        });
    }

    @Test
    void testOnlySingleNonMatchingPropertiesData() {
        Secret secret = secretMapBuilder("testOnlySingleMatchingPropertiesData")
                .addToData("app.properties", encodeValue("key1=value1\nkey2=value2\nsome.key=someValue")).build();

        List<ConfigSource> configSources = sut.toConfigSources(secret.getMetadata(), secret.getData(), 0);

        assertThat(configSources).isNotEmpty();
    }

    @Test
    void testOnlySingleMatchingYamlData() {
        Secret configMap = secretMapBuilder("testOnlySingleMatchingYamlData")
                .addToData("application.yaml", encodeValue("key1: value1\nkey2: value2\nsome:\n  key: someValue")).build();

        List<ConfigSource> configSources = sut.toConfigSources(configMap.getMetadata(), configMap.getData(), 0);

        assertThat(configSources).singleElement().satisfies(c -> {
            assertThat(c.getProperties()).containsOnly(entry("key1", "value1"), entry("key2", "value2"),
                    entry("some.key", "someValue"));
            assertThat(c.getName()).contains("testOnlySingleMatchingYamlData");
        });
    }

    @Test
    void testOnlySingleNonMatchingYamlData() {
        Secret secret = secretMapBuilder("testOnlySingleMatchingPropertiesData")
                .addToData("app.yaml", encodeValue("key1: value1\nkey2: value2\nsome:\n  key: someValue")).build();

        List<ConfigSource> configSources = sut.toConfigSources(secret.getMetadata(), secret.getData(), 0);

        assertThat(configSources).isNotEmpty();
    }

    @Test
    void testWithAllKindsOfData() {
        Secret secret = secretMapBuilder("testWithAllKindsOfData")
                .addToData("some.key", encodeValue("someValue"))
                .addToData("application.properties", encodeValue("key1=value1\napp.key=val"))
                .addToData("app.properties", encodeValue("ignored1=ignoredValue1"))
                .addToData("application.yaml", encodeValue("key2: value2\nsome:\n  otherKey: someOtherValue"))
                .addToData("app.yaml", encodeValue("ignored2: ignoredValue2"))
                .addToData("application.yml", encodeValue("key3: value3"))
                .addToData("app.yml", encodeValue("ignored3: ignoredValue3"))
                .build();

        List<ConfigSource> configSources = sut.toConfigSources(secret.getMetadata(), secret.getData(), 0);

        assertThat(configSources).hasSize(4);
        assertThat(configSources.get(0).getClass().getName().contains("SecretLiteralDataPropertiesConfigSource")).isTrue();

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

    private SecretBuilder secretMapBuilder(String name) {
        return new SecretBuilder().withNewMetadata()
                .withName(name).withNamespace("namespace").withUid("uid")
                .withResourceVersion("version").endMetadata();
    }

    private String encodeValue(String value) {
        return Base64.getEncoder().encodeToString(value.getBytes());
    }

}
