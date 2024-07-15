package io.quarkus.opentelemetry.deployment.instrumentation;

import static io.opentelemetry.api.trace.SpanKind.SERVER;
import static io.opentelemetry.semconv.ClientAttributes.CLIENT_ADDRESS;
import static io.quarkus.opentelemetry.deployment.common.SemconvResolver.assertSemanticAttribute;
import static io.quarkus.opentelemetry.deployment.common.TestSpanExporter.getSpanByKindAndParentId;
import static org.hamcrest.Matchers.is;

import java.util.List;

import jakarta.inject.Inject;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.opentelemetry.sdk.trace.data.SpanData;
import io.quarkus.opentelemetry.deployment.common.InMemoryMetricExporter;
import io.quarkus.opentelemetry.deployment.common.InMemoryMetricExporterProvider;
import io.quarkus.opentelemetry.deployment.common.SemconvResolver;
import io.quarkus.opentelemetry.deployment.common.TestSpanExporter;
import io.quarkus.opentelemetry.deployment.common.TestSpanExporterProvider;
import io.quarkus.opentelemetry.deployment.common.TracerRouter;
import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class VertxOpenTelemetryXForwardedTest {
    @RegisterExtension
    static final QuarkusUnitTest unitTest = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClass(TracerRouter.class)
                    .addClasses(TestSpanExporter.class, TestSpanExporterProvider.class, SemconvResolver.class)
                    .addClasses(InMemoryMetricExporter.class, InMemoryMetricExporterProvider.class)
                    .addAsResource(new StringAsset(TestSpanExporterProvider.class.getCanonicalName()),
                            "META-INF/services/io.opentelemetry.sdk.autoconfigure.spi.traces.ConfigurableSpanExporterProvider")
                    .addAsResource(new StringAsset(InMemoryMetricExporterProvider.class.getCanonicalName()),
                            "META-INF/services/io.opentelemetry.sdk.autoconfigure.spi.metrics.ConfigurableMetricExporterProvider"))
            .withConfigurationResource("application-default.properties");

    @Inject
    TestSpanExporter testSpanExporter;

    @Test
    void trace() {
        RestAssured.given().header("X-Forwarded-For", "203.0.113.195, 70.41.3.18, 150.172.238.178")
                .when().get("/tracer").then()
                .statusCode(200)
                .body(is("Hello Tracer!"));

        List<SpanData> spans = testSpanExporter.getFinishedSpanItems(2);

        SpanData server = getSpanByKindAndParentId(spans, SERVER, "0000000000000000");
        assertSemanticAttribute(server, "203.0.113.195", CLIENT_ADDRESS);
    }
}
