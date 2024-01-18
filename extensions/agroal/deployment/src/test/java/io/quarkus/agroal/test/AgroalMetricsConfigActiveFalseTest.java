package io.quarkus.agroal.test;

import jakarta.inject.Inject;

import org.eclipse.microprofile.metrics.Counter;
import org.eclipse.microprofile.metrics.Gauge;
import org.eclipse.microprofile.metrics.MetricID;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.Tag;
import org.eclipse.microprofile.metrics.annotation.RegistryType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class AgroalMetricsConfigActiveFalseTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withConfigurationResource("application-metrics-enabled.properties")
            .overrideConfigKey("quarkus.datasource.active", "false")
            .overrideConfigKey("quarkus.datasource.ds1.active", "false");

    @Inject
    @RegistryType(type = MetricRegistry.Type.VENDOR)
    MetricRegistry registry;

    @Test
    public void testMetricsOfDefaultDS() {
        Counter acquireCount = registry.getCounters()
                .get(new MetricID("agroal.acquire.count", new Tag("datasource", "default")));
        Gauge<?> maxUsed = registry.getGauges()
                .get(new MetricID("agroal.max.used.count", new Tag("datasource", "default")));

        Assertions.assertNull(acquireCount, "Agroal metrics should not be registered for deactivated datasources eagerly");
        Assertions.assertNull(maxUsed, "Agroal metrics should not be registered for deactivated datasources eagerly");
    }

    @Test
    public void testMetricsOfDs1() {
        Counter acquireCount = registry.getCounters().get(new MetricID("agroal.acquire.count",
                new Tag("datasource", "ds1")));
        Gauge<?> maxUsed = registry.getGauges().get(new MetricID("agroal.max.used.count",
                new Tag("datasource", "ds1")));

        Assertions.assertNull(acquireCount, "Agroal metrics should not be registered for deactivated datasources eagerly");
        Assertions.assertNull(maxUsed, "Agroal metrics should not be registered for deactivated datasources eagerly");
    }

}
