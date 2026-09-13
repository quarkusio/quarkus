package io.quarkus.opentelemetry.deployment;

import static io.opentelemetry.api.trace.SpanKind.INTERNAL;
import static io.opentelemetry.api.trace.SpanKind.SERVER;
import static io.quarkus.opentelemetry.deployment.common.exporter.TestSpanExporter.getSpanByKindAndParentId;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;

import jakarta.inject.Inject;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.quarkus.opentelemetry.deployment.OpenTelemetryBaggageAttributesTest.BaggageResource;
import io.quarkus.opentelemetry.deployment.common.exporter.TestSpanExporter;
import io.quarkus.opentelemetry.deployment.common.exporter.TestSpanExporterProvider;
import io.quarkus.test.QuarkusExtensionTest;
import io.restassured.RestAssured;

/**
 * By default, baggage entries are not copied to span attributes.
 */
public class OpenTelemetryBaggageAttributesDisabledTest {

    @RegisterExtension
    static final QuarkusExtensionTest TEST = new QuarkusExtensionTest().setArchiveProducer(
            () -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(TestSpanExporter.class, TestSpanExporterProvider.class, BaggageResource.class)
                    .addAsResource(new StringAsset(""
                            + "quarkus.otel.bsp.schedule.delay=50\n"
                            + "quarkus.otel.traces.sampler.arg=1.0d\n"
                            + "quarkus.datasource.devservices.enabled=false\n"), "application.properties"));

    @Inject
    TestSpanExporter spanExporter;

    @Test
    void testBaggageEntriesAreNotCopiedByDefault() {
        RestAssured.when().get("/baggage").then().statusCode(200);

        List<SpanData> spans = spanExporter.getFinishedSpanItems(2);
        SpanData server = getSpanByKindAndParentId(spans, SERVER, "0000000000000000");
        SpanData child = getSpanByKindAndParentId(spans, INTERNAL, server.getSpanId());

        assertNull(child.getAttributes().get(AttributeKey.stringKey("baggage.x-correlation-id")));
        assertNull(child.getAttributes().get(AttributeKey.stringKey("baggage.tenant")));
    }
}
