package io.quarkus.opentelemetry.deployment;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.InvocationTargetException;

import javax.inject.Inject;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import io.quarkus.opentelemetry.deployment.common.TestSpanExporter;
import io.quarkus.opentelemetry.deployment.common.TestSpanExporterProvider;
import io.quarkus.opentelemetry.deployment.common.TestUtil;
import io.quarkus.test.QuarkusUnitTest;
import io.smallrye.config.SmallRyeConfig;

public class OpenTelemetrySamplerConfigTest {
    @RegisterExtension
    static final QuarkusUnitTest unitTest = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar.addClass(TestUtil.class)
                    .addClasses(TestSpanExporter.class, TestSpanExporterProvider.class)
                    .addAsResource(new StringAsset(TestSpanExporterProvider.class.getCanonicalName()),
                            "META-INF/services/io.opentelemetry.sdk.autoconfigure.spi.traces.ConfigurableSpanExporterProvider"))
            .overrideConfigKey(SmallRyeConfig.SMALLRYE_CONFIG_MAPPING_VALIDATE_UNKNOWN, "false")// FIXME default config mapping
            .overrideConfigKey("otel.traces.exporter", "test-span-exporter")
            .overrideConfigKey("otel.metrics.exporter", "none")
            .overrideConfigKey("otel.logs.exporter", "none")
            .overrideConfigKey("otel.bsp.schedule.delay", "200")
            .overrideConfigKey("quarkus.opentelemetry.tracer.sampler", "ratio")
            .overrideConfigKey("quarkus.opentelemetry.tracer.sampler.ratio", "0.5")
            .overrideConfigKey("quarkus.opentelemetry.tracer.sampler.parent-based", "false")
            .overrideConfigKey("quarkus.opentelemetry.tracer.suppress-non-application-uris", "false");
    // TODO create test with new properties, not setting the late bound sampler
    @Inject
    OpenTelemetry openTelemetry;

    @Test
    void test() throws NoSuchFieldException, IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        Sampler sampler = TestUtil.getSampler(openTelemetry);

        assertEquals(String.format("TraceIdRatioBased{%.6f}", 0.5d), sampler.getDescription());
    }
}
