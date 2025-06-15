package io.quarkus.micrometer.deployment.binder;

import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.redis.runtime.client.ObservableRedisMetrics;
import io.quarkus.test.QuarkusUnitTest;

public class RedisClientMetricsDisabledTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest().withConfigurationResource("test-logging.properties")
            .overrideConfigKey("quarkus.micrometer.binder.redis.enabled", "false")
            .overrideConfigKey("quarkus.micrometer.binder-enabled-default", "false")
            .overrideConfigKey("quarkus.micrometer.registry-enabled-default", "false")
            .overrideConfigKey("quarkus.redis.devservices.enabled", "false").withEmptyApplication();

    @Inject
    Instance<ObservableRedisMetrics> bean;

    @Test
    void testNoInstancePresentIfNoRedisClientsClass() {
        assertTrue(bean.isUnsatisfied(), "No redis metrics bean");
    }

}
