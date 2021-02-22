package io.quarkus.micrometer.deployment.export;

import java.util.Set;

import javax.inject.Inject;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.composite.CompositeMeterRegistry;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import io.quarkus.test.QuarkusUnitTest;

public class PreferredPrometheusTest {
    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withConfigurationResource("test-logging.properties")
            .overrideConfigKey("quarkus.micrometer.binder-enabled-default", "false")
            .overrideConfigKey("quarkus.micrometer.export.prometheus.enabled", "true")
            .overrideConfigKey("quarkus.micrometer.registry-enabled-default", "false")
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClass(PrometheusRegistryProcessor.REGISTRY_CLASS)
                    .addClass(PreferredPrometheusProvider.MyPrometheusMeterRegistry.class)
                    .addClass(PreferredPrometheusProvider.class));

    @Inject
    MeterRegistry registry;

    @Test
    public void testMeterRegistryPresent() {
        // We want a composite that contains both registries.
        Assertions.assertNotNull(registry, "A registry should be configured");
        Set<MeterRegistry> subRegistries = ((CompositeMeterRegistry) registry).getRegistries();
        Assertions.assertEquals(1, subRegistries.size());
        PrometheusMeterRegistry subPromRegistry = (PrometheusMeterRegistry) subRegistries.iterator().next();
        Assertions.assertEquals(PreferredPrometheusProvider.MyPrometheusMeterRegistry.class, subPromRegistry.getClass(),
                "Should be MyPrometheusMeterRegistry");
    }
}
