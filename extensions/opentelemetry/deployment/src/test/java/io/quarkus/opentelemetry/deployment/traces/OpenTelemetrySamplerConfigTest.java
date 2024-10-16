package io.quarkus.opentelemetry.deployment.traces;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.InvocationTargetException;

import jakarta.inject.Inject;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import io.quarkus.opentelemetry.deployment.common.TestUtil;
import io.quarkus.opentelemetry.deployment.common.exporter.TestSpanExporter;
import io.quarkus.opentelemetry.deployment.common.exporter.TestSpanExporterProvider;
import io.quarkus.test.QuarkusUnitTest;

public class OpenTelemetrySamplerConfigTest {
    @RegisterExtension
    static final QuarkusUnitTest unitTest = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar.addClass(TestUtil.class)
                    .addClasses(TestSpanExporter.class, TestSpanExporterProvider.class)
                    .addAsResource(new StringAsset(TestSpanExporterProvider.class.getCanonicalName()),
                            "META-INF/services/io.opentelemetry.sdk.autoconfigure.spi.traces.ConfigurableSpanExporterProvider"))
            .withConfigurationResource("resource-config/application-no-metrics.properties")
            .overrideConfigKey("quarkus.otel.traces.sampler", "traceidratio")
            .overrideConfigKey("quarkus.otel.traces.sampler.arg", "0.5")
            .overrideConfigKey("quarkus.otel.traces.suppress-non-application-uris", "false");

    @Inject
    OpenTelemetry openTelemetry;

    @Test
    void test() throws NoSuchFieldException, IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        Sampler sampler = TestUtil.getSampler(openTelemetry);

        assertEquals(String.format("TraceIdRatioBased{%.6f}", 0.5d), sampler.getDescription());
    }
}
