package io.quarkus.test.kafka;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import io.quarkus.test.common.DevServicesContext;

public class KafkaCompanionResourceTest {

    private static DevServicesContext contextWith(Map<String, String> properties) {
        return new DevServicesContext() {
            @Override
            public Map<String, String> devServicesProperties() {
                return properties;
            }

            @Override
            public Optional<String> containerNetworkId() {
                return Optional.empty();
            }
        };
    }

    @AfterEach
    void cleanup() {
        System.clearProperty("kafka.bootstrap.servers");
    }

    @Test
    void testCompanionInitializedFromDevServices() {
        KafkaCompanionResource resource = new KafkaCompanionResource();
        resource.setIntegrationTestContext(contextWith(Map.of("kafka.bootstrap.servers", "localhost:9092")));

        assertNotNull(resource.kafkaCompanion,
                "kafkaCompanion should be set when Dev Services provide bootstrap servers");
    }

    @Test
    void testCompanionInitializedFromSystemProperty() {
        System.setProperty("kafka.bootstrap.servers", "localhost:9092");

        KafkaCompanionResource resource = new KafkaCompanionResource();
        resource.setIntegrationTestContext(contextWith(Collections.emptyMap()));

        assertNotNull(resource.kafkaCompanion,
                "kafkaCompanion should be set from system property when Dev Services are not running");
    }

    @Test
    void testNoContainerStartedWhenBootstrapServersExplicitlySet() {
        System.setProperty("kafka.bootstrap.servers", "localhost:9092");

        KafkaCompanionResource resource = new KafkaCompanionResource();
        resource.setIntegrationTestContext(contextWith(Collections.emptyMap()));
        resource.init(Collections.emptyMap());

        assertNull(resource.kafka,
                "No Kafka container should be created when bootstrap servers are explicitly set");
    }
}
