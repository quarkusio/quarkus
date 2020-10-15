package io.quarkus.micrometer.deployment.export;

import java.util.Arrays;

import javax.inject.Inject;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.config.validate.ValidationException;
import io.quarkus.micrometer.runtime.MicrometerRecorder;
import io.quarkus.test.QuarkusUnitTest;

public class SignalFxEnabledInvalidUriTest {
    static final String REGISTRY_CLASS_NAME = "io.micrometer.signalfx.SignalFxMeterRegistry";
    static final Class<?> REGISTRY_CLASS = MicrometerRecorder.getClassForName(REGISTRY_CLASS_NAME);

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withConfigurationResource("test-logging.properties")
            .overrideConfigKey("quarkus.micrometer.export.signalfx.enabled", "true")
            .overrideConfigKey("quarkus.micrometer.export.signalfx.access-token", "required")
            .overrideConfigKey("quarkus.micrometer.export.signalfx.uri", "intentionally-bad-uri")
            .overrideConfigKey("quarkus.micrometer.registry-enabled-default", "false")
            .overrideConfigKey("quarkus.micrometer.binder-enabled-default", "false")
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClass(StackdriverRegistryProcessor.REGISTRY_CLASS))
            .assertException(t -> {
                Assertions.assertEquals(ValidationException.class.getName(), t.getClass().getName(),
                        "Unexpected exception" + stackToString(t));
            });

    @Inject
    MeterRegistry registry;

    @Test
    public void testMeterRegistryPresent() {
        // SignalFx is enabled (alone, all others disabled)
        Assertions.assertNotNull(registry, "A registry should be configured");
        Assertions.assertEquals(REGISTRY_CLASS, registry.getClass(), "Should be SignalFxMeterRegistry");
    }

    static String stackToString(Throwable t) {
        StringBuilder sb = new StringBuilder().append("\n");
        while (t.getCause() != null) {
            t = t.getCause();
        }
        sb.append(t.getClass()).append(": ").append(t.getMessage()).append("\n");
        Arrays.asList(t.getStackTrace()).forEach(x -> sb.append("\t").append(x.toString()).append("\n"));
        return sb.toString();
    }
}
