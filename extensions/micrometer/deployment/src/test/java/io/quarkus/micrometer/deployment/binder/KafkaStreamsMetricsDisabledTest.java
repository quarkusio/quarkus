package io.quarkus.micrometer.deployment.binder;

import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.micrometer.runtime.binder.kafka.KafkaStreamsEventObserver;
import io.quarkus.test.QuarkusUnitTest;

public class KafkaStreamsMetricsDisabledTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withConfigurationResource("test-logging.properties")
            .overrideConfigKey("quarkus.micrometer.binder.kafka.enabled", "true")
            .overrideConfigKey("quarkus.micrometer.binder-enabled-default", "false")
            .overrideConfigKey("quarkus.micrometer.registry-enabled-default", "false")
            .overrideConfigKey("quarkus.redis.devservices.enabled", "false")
            .withEmptyApplication();

    @Inject
    Instance<KafkaStreamsEventObserver> kafkaStreamEventObservers;

    @Test
    void testNoInstancePresentIfNoKafkaStreamsClass() {
        assertTrue(kafkaStreamEventObservers.isUnsatisfied(),
                "No kafkaStreamEventObservers expected, because we don't have dependency to kafka-streams");
    }

}
