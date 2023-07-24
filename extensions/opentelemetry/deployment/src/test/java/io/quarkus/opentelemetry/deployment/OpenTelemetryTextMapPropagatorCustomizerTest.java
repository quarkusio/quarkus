package io.quarkus.opentelemetry.deployment;

import static org.assertj.core.api.Assertions.*;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.opentelemetry.api.baggage.propagation.W3CBaggagePropagator;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.quarkus.opentelemetry.TextMapPropagatorCustomizer;
import io.quarkus.opentelemetry.deployment.common.TestSpanExporter;
import io.quarkus.opentelemetry.deployment.common.TestSpanExporterProvider;
import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class OpenTelemetryTextMapPropagatorCustomizerTest {

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest().setArchiveProducer(
            () -> ShrinkWrap.create(JavaArchive.class)
                    .addClass(TestSpanExporter.class)
                    .addClass(TestSpanExporterProvider.class)
                    .addClass(TestTextMapPropagatorCustomizer.class)
                    .addAsResource("resource-config/application.properties", "application.properties")
                    .addAsResource(
                            "META-INF/services-config/io.opentelemetry.sdk.autoconfigure.spi.traces.ConfigurableSpanExporterProvider",
                            "META-INF/services/io.opentelemetry.sdk.autoconfigure.spi.traces.ConfigurableSpanExporterProvider"));
    @Inject
    TestSpanExporter spanExporter;

    @Test
    void testSvcNameHasPriorityOverAppNameAndResourceAttr() {
        RestAssured.when()
                .get("/hello").then()
                .statusCode(200)
                .body(is("hello"));
        RestAssured.when()
                .get("/hello").then()
                .statusCode(200)
                .body(is("hello"));
        RestAssured.when()
                .get("/hello").then()
                .statusCode(200)
                .body(is("hello"));

        List<SpanData> spans = spanExporter.getFinishedSpanItems(3);
        final SpanData server = spans.get(0);
        assertEquals("GET /hello", server.getName());

        assertThat(TestTextMapPropagatorCustomizer.PROPAGATORS).containsOnly(W3CBaggagePropagator.class.getName(),
                W3CTraceContextPropagator.class.getName());
    }

    @Path("/hello")
    public static class HelloResource {
        @GET
        public String hello() {
            return "hello";
        }
    }

    @Singleton
    public static class TestTextMapPropagatorCustomizer implements TextMapPropagatorCustomizer {

        public static final Set<String> PROPAGATORS = ConcurrentHashMap.newKeySet();

        @Override
        public TextMapPropagator customize(Context context) {
            TextMapPropagator propagator = context.propagator();
            PROPAGATORS.add(propagator.getClass().getName());
            return propagator;
        }
    }
}
