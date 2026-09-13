package io.quarkus.opentelemetry.deployment;

import static io.opentelemetry.api.trace.SpanKind.INTERNAL;
import static io.opentelemetry.api.trace.SpanKind.SERVER;
import static io.quarkus.opentelemetry.deployment.common.exporter.TestSpanExporter.getSpanByKindAndParentId;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.opentelemetry.api.baggage.Baggage;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.quarkus.opentelemetry.deployment.common.exporter.TestSpanExporter;
import io.quarkus.opentelemetry.deployment.common.exporter.TestSpanExporterProvider;
import io.quarkus.test.QuarkusExtensionTest;
import io.restassured.RestAssured;

/**
 * With {@code quarkus.otel.traces.baggage-as-attributes.enabled=true}, the selected baggage entries of the parent context
 * are copied to the attributes of the spans started in that context.
 */
public class OpenTelemetryBaggageAttributesTest {

    @RegisterExtension
    static final QuarkusExtensionTest TEST = new QuarkusExtensionTest().setArchiveProducer(
            () -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(TestSpanExporter.class, TestSpanExporterProvider.class, BaggageResource.class)
                    .addAsResource(new StringAsset(""
                            + "quarkus.otel.bsp.schedule.delay=50\n"
                            + "quarkus.otel.traces.sampler.arg=1.0d\n"
                            + "quarkus.otel.traces.baggage-as-attributes.enabled=true\n"
                            + "quarkus.otel.traces.baggage-as-attributes.keys=x-correlation-id\n"
                            + "quarkus.datasource.devservices.enabled=false\n"), "application.properties"));

    @Inject
    TestSpanExporter spanExporter;

    @Test
    void testSelectedBaggageEntriesBecomeAttributes() {
        RestAssured.when().get("/baggage").then().statusCode(200);

        List<SpanData> spans = spanExporter.getFinishedSpanItems(2);
        SpanData server = getSpanByKindAndParentId(spans, SERVER, "0000000000000000");
        SpanData child = getSpanByKindAndParentId(spans, INTERNAL, server.getSpanId());

        assertEquals("abc", child.getAttributes().get(AttributeKey.stringKey("baggage.x-correlation-id")));
        assertNull(child.getAttributes().get(AttributeKey.stringKey("baggage.tenant")));
        assertNull(server.getAttributes().get(AttributeKey.stringKey("baggage.x-correlation-id")));
    }

    @Path("/baggage")
    public static class BaggageResource {

        @Inject
        Tracer tracer;

        @GET
        public String baggage() {
            Baggage baggage = Baggage.current().toBuilder()
                    .put("x-correlation-id", "abc")
                    .put("tenant", "acme")
                    .build();
            try (Scope scope = baggage.makeCurrent()) {
                Span child = tracer.spanBuilder("child").startSpan();
                child.end();
            }
            return "ok";
        }
    }
}
