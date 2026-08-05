package io.quarkus.micrometer.deployment.binder;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.micrometer.core.instrument.MeterRegistry;
import io.quarkus.test.QuarkusExtensionTest;

@DisabledOnOs(OS.WINDOWS)
public class AgroalMetricsOnlyTestCase {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .overrideConfigKey("quarkus.datasource.db-kind", "h2")
            .overrideConfigKey("quarkus.datasource.metrics.enabled", "true")
            .overrideRuntimeConfigKey("quarkus.datasource.username", "username-named");

    @Inject
    MeterRegistry registry;

    @Test
    public void testMetricsAreExposed() {
        assertNotNull(registry.get("agroal.acquire.count").tag("datasource", "default").functionCounter());
        assertNotNull(registry.get("agroal.max.used.count").tag("datasource", "default").gauge());
    }
}
