package io.quarkus.opentelemetry.deployment.traces;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.stringContainsInOrder;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.trace.data.LinkData;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import io.opentelemetry.sdk.trace.samplers.SamplingResult;
import io.quarkus.opentelemetry.deployment.common.TestUtil;
import io.quarkus.opentelemetry.deployment.common.TracerRouter;
import io.quarkus.opentelemetry.deployment.common.exporter.TestSpanExporter;
import io.quarkus.opentelemetry.deployment.common.exporter.TestSpanExporterProvider;
import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class OpenTelemetryCustomSamplerBeanTest {

    private static final String TEST_SAMPLER = "testSampler";

    @RegisterExtension
    static final QuarkusUnitTest unitTest = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClass(TracerRouter.class)
                    .addClass(TestSpanExporter.class)
                    .addClass(TestSpanExporterProvider.class)
                    .addClass(TestUtil.class)
                    .addAsResource(new StringAsset(TestSpanExporterProvider.class.getCanonicalName()),
                            "META-INF/services/io.opentelemetry.sdk.autoconfigure.spi.traces.ConfigurableSpanExporterProvider"))
            .withConfigurationResource("resource-config/application-no-metrics.properties");

    @Inject
    OpenTelemetry openTelemetry;

    @Inject
    TestSpanExporter testSpanExporter;

    @Test
    void testHealthEndpointNotTraced() {
        RestAssured.when().get("/q/health").then()
                .statusCode(200)
                .body(containsString("\"status\": \"UP\""));

        RestAssured.when().get("/q/health/live").then()
                .statusCode(200)
                .body(containsString("\"status\": \"UP\""));

        RestAssured.when().get("/q/health/ready").then()
                .statusCode(200)
                .body(containsString("\"status\": \"UP\""));

        RestAssured.when().get("/tracer").then()
                .statusCode(200)
                .body(is("Hello Tracer!"));

        testSpanExporter.assertSpanCount(2);
    }

    @Test
    void test() throws NoSuchFieldException, IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        Sampler sampler = TestUtil.getSampler(openTelemetry);

        assertThat(sampler.getDescription(), stringContainsInOrder(TEST_SAMPLER));
    }

    @ApplicationScoped
    public static class OtelCDISamplerProvider {

        @Produces
        public Sampler sampler() {
            return new Sampler() {
                @Override
                public SamplingResult shouldSample(Context context, String s, String s1, SpanKind spanKind,
                        Attributes attributes,
                        List<LinkData> list) {
                    return SamplingResult.recordAndSample();
                }

                @Override
                public String getDescription() {
                    return TEST_SAMPLER;
                }
            };
        }
    }
}
