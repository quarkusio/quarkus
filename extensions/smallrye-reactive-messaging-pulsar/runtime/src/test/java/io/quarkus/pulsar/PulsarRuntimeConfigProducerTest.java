package io.quarkus.pulsar;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;

import io.smallrye.config.SmallRyeConfig;
import io.smallrye.config.SmallRyeConfigBuilder;
import io.smallrye.config.common.MapBackedConfigSource;

class PulsarRuntimeConfigProducerTest {

    @Test
    void testMapConfig() {
        SmallRyeConfig config = new SmallRyeConfigBuilder().withSources(
                new MapBackedConfigSource("test", Map.of("pulsar.client.serviceUrl", "pulsar://pulsar:6650")) {
                }).build();
        Map<String, Object> configMap = PulsarRuntimeConfigProducer.getMapFromConfig(config, "pulsar.client");
        assertThat(configMap).containsKeys("serviceUrl");
    }

    @Test
    void testMapConfigFromEnvVars() {
        SmallRyeConfig config = new SmallRyeConfigBuilder().withSources(
                new MapBackedConfigSource("test", Map.of("PULSAR_CLIENT_SERVICE_URL", "pulsar://pulsar:6650")) {
                }).build();
        Map<String, Object> configMap = PulsarRuntimeConfigProducer.getMapFromConfig(config, "pulsar.client");
        assertThat(configMap).containsKeys("serviceUrl");
    }
}
