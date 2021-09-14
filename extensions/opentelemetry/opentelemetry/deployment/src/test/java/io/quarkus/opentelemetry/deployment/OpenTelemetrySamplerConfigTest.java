package io.quarkus.opentelemetry.deployment;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.InvocationTargetException;

import javax.inject.Inject;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import io.quarkus.test.QuarkusUnitTest;

public class OpenTelemetrySamplerConfigTest {
    @RegisterExtension
    static final QuarkusUnitTest unitTest = new QuarkusUnitTest()
            .overrideConfigKey("quarkus.opentelemetry.tracer.sampler", "ratio")
            .overrideConfigKey("quarkus.opentelemetry.tracer.sampler.ratio", "0.5")
            .overrideConfigKey("quarkus.opentelemetry.tracer.sampler.parent-based", "false")
            .overrideConfigKey("quarkus.opentelemetry.tracer.suppress-non-application-uris", "false")
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class).addClass(TestUtil.class));

    @Inject
    OpenTelemetry openTelemetry;

    @Test
    void test() throws NoSuchFieldException, IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        Sampler sampler = TestUtil.getSampler(openTelemetry);

        assertEquals(String.format("TraceIdRatioBased{%.6f}", 0.5d), sampler.getDescription());
    }
}
