package io.quarkus.opentelemetry.deployment.traces;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.stringContainsInOrder;

import java.lang.reflect.InvocationTargetException;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
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

public class OpenTelemetrySamplerBeanTest {
    @RegisterExtension
    static final QuarkusUnitTest unitTest = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar.addClass(TestUtil.class)
                    .addClasses(TestSpanExporter.class, TestSpanExporterProvider.class)
                    .addAsResource(new StringAsset(TestSpanExporterProvider.class.getCanonicalName()),
                            "META-INF/services/io.opentelemetry.sdk.autoconfigure.spi.traces.ConfigurableSpanExporterProvider"))
            .withConfigurationResource("resource-config/application-no-metrics.properties");

    @Inject
    OpenTelemetry openTelemetry;

    @Test
    void test() throws NoSuchFieldException, IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        Sampler sampler = TestUtil.getSampler(openTelemetry);

        assertThat(sampler.getDescription(), stringContainsInOrder("AlwaysOffSampler"));
    }

    @ApplicationScoped
    public static class OtelConfiguration {

        @Produces
        public Sampler sampler() {
            return Sampler.alwaysOff();
        }
    }
}
