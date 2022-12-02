package io.quarkus.micrometer.runtime.binder.mpmetrics;

import javax.inject.Inject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.micrometer.core.instrument.MeterRegistry;
import io.quarkus.micrometer.test.MpColorResource;
import io.quarkus.test.QuarkusUnitTest;

public class MpMetricNamingTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withConfigurationResource("test-logging.properties")
            .overrideConfigKey("quarkus.micrometer.binder.mp-metrics.enabled", "true")
            .overrideConfigKey("quarkus.micrometer.binder.vertx.enabled", "false")
            .overrideConfigKey("quarkus.micrometer.registry-enabled-default", "false")
            .withApplicationRoot((jar) -> jar
                    .addClass(MpColorResource.class));

    @Inject
    MeterRegistry registry;

    @Inject
    MpColorResource colors;

    @Test
    public void testAnnotatedMeterNames() {
        colors.blue();
        colors.red();
        colors.green();
        colors.yellow();

        Assertions.assertNotNull(
                registry.find("io.quarkus.micrometer.test.MpColorResource.red").counter());
        Assertions.assertNotNull(
                registry.find("io.quarkus.micrometer.test.MpColorResource.blueCount").counter());
        Assertions.assertNotNull(
                registry.find("greenCount").counter());
        Assertions.assertNotNull(
                registry.find("yellow").gauge());
    }
}
