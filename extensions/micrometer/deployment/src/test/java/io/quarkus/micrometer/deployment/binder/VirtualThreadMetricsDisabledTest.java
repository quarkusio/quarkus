package io.quarkus.micrometer.deployment.binder;

import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.micrometer.runtime.binder.virtualthreads.VirtualThreadCollector;
import io.quarkus.test.QuarkusUnitTest;

public class VirtualThreadMetricsDisabledTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withConfigurationResource("test-logging.properties")
            .overrideConfigKey("quarkus.micrometer.binder.virtual-threads.enabled", "true")

            .overrideConfigKey("quarkus.micrometer.binder-enabled-default", "false")
            .overrideConfigKey("quarkus.micrometer.registry-enabled-default", "false")
            .overrideConfigKey("quarkus.redis.devservices.enabled", "false")
            .withEmptyApplication();

    @Inject
    BeanManager beans;

    @Test
    void testNoInstancePresentIfDisabled() {
        assertTrue(
                beans.createInstance().select()
                        .stream().filter(this::isVirtualThreadCollector).findAny().isEmpty(),
                "No VirtualThreadCollector expected");
    }

    private boolean isVirtualThreadCollector(Object bean) {
        return bean.getClass().toString().equals(VirtualThreadCollector.class.toString());
    }

}
