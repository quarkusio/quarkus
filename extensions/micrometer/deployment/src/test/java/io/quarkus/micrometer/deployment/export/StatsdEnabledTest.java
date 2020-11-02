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
import io.quarkus.micrometer.runtime.MicrometerRecorder;
import io.quarkus.test.QuarkusUnitTest;

public class StatsdEnabledTest {
    static final String REGISTRY_CLASS_NAME = "io.micrometer.statsd.StatsdMeterRegistry";
    static final Class<?> REGISTRY_CLASS = MicrometerRecorder.getClassForName(REGISTRY_CLASS_NAME);

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withConfigurationResource("test-logging.properties")
            .overrideConfigKey("quarkus.micrometer.binder-enabled-default", "false")
            .overrideConfigKey("quarkus.micrometer.export.statsd.enabled", "true")
            .overrideConfigKey("quarkus.micrometer.export.statsd.publish", "false")
            .overrideConfigKey("quarkus.micrometer.registry-enabled-default", "false")
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClass(StatsdRegistryProcessor.REGISTRY_CLASS));

    @Inject
    MeterRegistry registry;

    @Test
    public void testMeterRegistryPresent() {
        // Stackdriver is enabled (alone, all others disabled)
        Assertions.assertNotNull(registry, "A registry should be configured");
        Set<MeterRegistry> subRegistries = ((CompositeMeterRegistry) registry).getRegistries();
        Assertions.assertEquals(REGISTRY_CLASS, subRegistries.iterator().next().getClass(),
                "Should be StatsdMeterRegistry");
    }
}
