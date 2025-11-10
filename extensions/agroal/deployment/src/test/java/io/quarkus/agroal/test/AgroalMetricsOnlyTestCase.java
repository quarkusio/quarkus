package io.quarkus.agroal.test;

import jakarta.inject.Inject;

import org.eclipse.microprofile.metrics.*;
import org.eclipse.microprofile.metrics.annotation.RegistryType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

/**
 * Test to verify that Agroal metrics are exposed when only quarkus.datasource.metrics.enabled=true is set
 */
public class AgroalMetricsOnlyTestCase {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withConfigurationResource("base.properties")
            .overrideConfigKey("quarkus.datasource.metrics.enabled", "true");

    @Inject
    @RegistryType(type = MetricRegistry.Type.VENDOR)
    MetricRegistry registry;

    @Test
    public void testMetricsAreExposed() {
        Counter acquireCount = registry.getCounters()
                .get(new MetricID("agroal.acquire.count", new Tag("datasource", "default")));
        Gauge<?> maxUsed = registry.getGauges()
                .get(new MetricID("agroal.max.used.count", new Tag("datasource", "default")));

        Assertions.assertNotNull(acquireCount, "Agroal metrics should be registered eagerly");
        Assertions.assertNotNull(maxUsed, "Agroal metrics should be registered eagerly");
    }

}
