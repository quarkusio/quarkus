package io.quarkus.micrometer.deployment.binder;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.micrometer.core.instrument.MeterRegistry;
import io.quarkus.test.QuarkusUnitTest;

@DisabledOnOs(OS.WINDOWS)
public class AgroalMetricsOnlyTestCase {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .overrideConfigKey("quarkus.datasource.db-kind", "h2")
            .overrideConfigKey("quarkus.datasource.metrics.enabled", "true")
            .overrideRuntimeConfigKey("quarkus.datasource.username", "username-named")
            .overrideRuntimeConfigKey("quarkus.datasource.jdbc.url", "jdbc:h2:tcp://localhost/mem:testing");

    @Inject
    MeterRegistry registry;

    @Test
    public void testMetricsAreExposed() {
        assertNotNull(registry.get("agroal.acquire.count").tag("datasource", "default").functionCounter());
        assertNotNull(registry.get("agroal.max.used.count").tag("datasource", "default").gauge());
    }
}
