package io.quarkus.micrometer.deployment.binder;

import static org.junit.jupiter.api.Assertions.assertTrue;

import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.micrometer.runtime.binder.kafka.KafkaEventObserver;
import io.quarkus.test.QuarkusUnitTest;

public class KafkaClientMetricsDisabledTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withConfigurationResource("test-logging.properties")
            .overrideConfigKey("quarkus.micrometer.binder.kafka.enabled", "true")
            .overrideConfigKey("quarkus.micrometer.binder-enabled-default", "false")
            .overrideConfigKey("quarkus.micrometer.registry-enabled-default", "false")
            .withEmptyApplication();

    @Inject
    Instance<KafkaEventObserver> kafkaEventObservers;

    @Test
    void testNoInstancePresentIfNoKafkaClientsClass() {
        assertTrue(kafkaEventObservers.isUnsatisfied(),
                "No kafkaEventObservers expected, because we don't have dependency to kafka-clients");
    }

}
