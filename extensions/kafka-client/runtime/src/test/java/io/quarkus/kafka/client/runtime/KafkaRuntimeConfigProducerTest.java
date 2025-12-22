package io.quarkus.kafka.client.runtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Optional;

import org.eclipse.microprofile.config.Config;
import org.junit.jupiter.api.Test;

import io.quarkus.kafka.client.tls.QuarkusKafkaSslEngineFactory;
import io.quarkus.runtime.ApplicationConfig;
import io.smallrye.config.SmallRyeConfig;
import io.smallrye.config.SmallRyeConfigBuilder;

public class KafkaRuntimeConfigProducerTest {

    @Test
    public void testTlsConfigurationNameWithHyphens() {
        // Test when property is set via application.yaml as kafka.tls-configuration-name
        Config config = new SmallRyeConfigBuilder()
                .withDefaultValues(Map.of("kafka.tls-configuration-name", "my-tls-config"))
                .build();
        ApplicationConfig appConfig = mock(ApplicationConfig.class);
        when(appConfig.name()).thenReturn(Optional.empty());

        KafkaRuntimeConfigProducer producer = new KafkaRuntimeConfigProducer();
        Map<String, Object> result = producer.createKafkaRuntimeConfig(config, appConfig);

        assertThat(result)
                .containsEntry("tls-configuration-name", "my-tls-config")
                .containsEntry("ssl.engine.factory.class", QuarkusKafkaSslEngineFactory.class.getName());
    }

    @Test
    public void testTlsConfigurationNameWithUnderscores() {
        // Test when property is set via environment variable as KAFKA_TLS_CONFIGURATION_NAME
        Config config = new SmallRyeConfigBuilder()
                .withDefaultValues(Map.of("KAFKA_TLS_CONFIGURATION_NAME", "my-tls-config"))
                .build();
        ApplicationConfig appConfig = mock(ApplicationConfig.class);
        when(appConfig.name()).thenReturn(Optional.empty());

        KafkaRuntimeConfigProducer producer = new KafkaRuntimeConfigProducer();
        Map<String, Object> result = producer.createKafkaRuntimeConfig(config, appConfig);

        assertThat(result)
                .containsEntry("tls-configuration-name", "my-tls-config")
                .containsEntry("ssl.engine.factory.class", QuarkusKafkaSslEngineFactory.class.getName());
    }

    @Test
    public void testOtherKafkaProperties() {
        // Test that other Kafka properties are processed correctly
        Config config = new SmallRyeConfigBuilder()
                .withDefaultValues(Map.of("kafka.bootstrap.servers", "localhost:9092",
                        "KAFKA_CONSUMER_MAX_POLL_RECORDS", "100"))
                .build();
        ApplicationConfig appConfig = mock(ApplicationConfig.class);
        when(appConfig.name()).thenReturn(Optional.empty());

        KafkaRuntimeConfigProducer producer = new KafkaRuntimeConfigProducer();
        Map<String, Object> result = producer.createKafkaRuntimeConfig(config, appConfig);

        assertThat(result)
                .containsEntry("bootstrap.servers", "localhost:9092")
                .containsEntry("consumer.max.poll.records", "100");
    }

    @Test
    public void testGroupIdDefaultsToApplicationName() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withDefaultValues(Map.of("kafka.bootstrap.servers", "localhost:9092"))
                .build();
        ApplicationConfig appConfig = mock(ApplicationConfig.class);
        when(appConfig.name()).thenReturn(Optional.of("my-app"));

        KafkaRuntimeConfigProducer producer = new KafkaRuntimeConfigProducer();
        Map<String, Object> result = producer.createKafkaRuntimeConfig(config, appConfig);

        assertThat(result)
                .containsEntry("bootstrap.servers", "localhost:9092")
                .containsEntry("group.id", "my-app");
    }
}
