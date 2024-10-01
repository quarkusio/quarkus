package io.quarkus.opentelemetry.deployment;

import static io.opentelemetry.api.trace.SpanKind.INTERNAL;
import static io.opentelemetry.api.trace.SpanKind.SERVER;
import static io.quarkus.opentelemetry.deployment.common.exporter.TestSpanExporter.getSpanByKindAndParentId;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.jboss.logging.MDC;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.quarkus.arc.Unremovable;
import io.quarkus.opentelemetry.deployment.common.exporter.InMemoryLogRecordExporterProvider;
import io.quarkus.opentelemetry.deployment.common.exporter.InMemoryMetricExporterProvider;
import io.quarkus.opentelemetry.deployment.common.exporter.TestSpanExporter;
import io.quarkus.opentelemetry.deployment.common.exporter.TestSpanExporterProvider;
import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class OpenTelemetryMDCTest {
    @RegisterExtension
    static final QuarkusUnitTest unitTest = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addPackage(TestSpanExporter.class.getPackage())
                    .addClass(MdcEntry.class)
                    .addClass(TestMdcCapturer.class)
                    .addClass(TestResource.class)
                    .addAsResource(new StringAsset(TestSpanExporterProvider.class.getCanonicalName()),
                            "META-INF/services/io.opentelemetry.sdk.autoconfigure.spi.traces.ConfigurableSpanExporterProvider")
                    .addAsResource(new StringAsset(InMemoryMetricExporterProvider.class.getCanonicalName()),
                            "META-INF/services/io.opentelemetry.sdk.autoconfigure.spi.metrics.ConfigurableMetricExporterProvider")
                    .addAsResource(new StringAsset(InMemoryLogRecordExporterProvider.class.getCanonicalName()),
                            "META-INF/services/io.opentelemetry.sdk.autoconfigure.spi.logs.ConfigurableLogRecordExporterProvider"))
            .withConfigurationResource("application-default.properties");

    @Inject
    TestSpanExporter spanExporter;
    @Inject
    TestMdcCapturer testMdcCapturer;
    @Inject
    Tracer tracer;

    @AfterEach
    void tearDown() {
        spanExporter.reset();
        testMdcCapturer.reset();
    }

    @Test
    void vertx() {
        RestAssured.when()
                .get("/hello").then()
                .statusCode(200)
                .body(is("hello"));

        List<SpanData> spans = spanExporter.getFinishedSpanItems(2);
        List<MdcEntry> mdcEntries = testMdcCapturer.getCapturedMdcEntries();
        List<MdcEntry> expectedMdcEntries = getExpectedMDCEntries(spans);

        final SpanData server = getSpanByKindAndParentId(spans, SERVER, "0000000000000000");
        assertEquals("GET /hello", server.getName());

        final SpanData programmatic = getSpanByKindAndParentId(spans, INTERNAL, server.getSpanId());
        assertEquals("something", programmatic.getName());

        assertEquals(expectedMdcEntries, mdcEntries);
    }

    @Test
    void nonVertx() {
        Span parentSpan = tracer.spanBuilder("parent").startSpan();
        try (Scope ignored = parentSpan.makeCurrent()) {
            testMdcCapturer.captureMdc();
            Span childSpan = tracer.spanBuilder("child").startSpan();
            try (Scope ignored1 = childSpan.makeCurrent()) {
                testMdcCapturer.captureMdc();
            } finally {
                childSpan.end();
            }
        } finally {
            parentSpan.end();
        }

        List<SpanData> spans = spanExporter.getFinishedSpanItems(2);
        List<MdcEntry> mdcEntries = testMdcCapturer.getCapturedMdcEntries();
        List<MdcEntry> expectedMdcEntries = getExpectedMDCEntries(spans);

        final SpanData parent = getSpanByKindAndParentId(spans, INTERNAL, "0000000000000000");
        assertEquals("parent", parent.getName());

        final SpanData child = getSpanByKindAndParentId(spans, INTERNAL, parent.getSpanId());
        assertEquals("child", child.getName());

        assertEquals(expectedMdcEntries, mdcEntries);
    }

    private List<MdcEntry> getExpectedMDCEntries(List<SpanData> spans) {
        return spans.stream()
                .map(spanData -> new MdcEntry(spanData.getSpanContext().isSampled(),
                        spanData.getParentSpanContext().isValid() ? spanData.getParentSpanId() : "null",
                        spanData.getSpanId(),
                        spanData.getTraceId()))
                .collect(Collectors.collectingAndThen(Collectors.toList(), l -> {
                    Collections.reverse(l);
                    return l;
                }));
    }

    @ApplicationScoped
    @Path("/")
    public static class TestResource {

        @Inject
        TestMdcCapturer testMdcCapturer;

        @Inject
        Tracer tracer;

        @GET
        @Path("/hello")
        public String hello() {
            testMdcCapturer.captureMdc();
            Span span = tracer.spanBuilder("something").startSpan();
            try (Scope ignored = span.makeCurrent()) {
                testMdcCapturer.captureMdc();
            } finally {
                span.end();
            }
            return "hello";
        }
    }

    @Unremovable
    @ApplicationScoped
    public static class TestMdcCapturer {
        private final List<MdcEntry> mdcEntries = Collections.synchronizedList(new ArrayList<>());

        public void reset() {
            mdcEntries.clear();
        }

        public void captureMdc() {
            mdcEntries.add(new MdcEntry(
                    Boolean.parseBoolean(String.valueOf(MDC.get("sampled"))),
                    String.valueOf(MDC.get("parentId")),
                    String.valueOf(MDC.get("spanId")),
                    String.valueOf(MDC.get("traceId"))));
        }

        public List<MdcEntry> getCapturedMdcEntries() {
            return List.copyOf(mdcEntries);
        }
    }

    public static class MdcEntry {
        public final boolean isSampled;
        public final String parentId;
        public final String spanId;
        public final String traceId;

        public MdcEntry(boolean isSampled, String parentId, String spanId, String traceId) {
            this.isSampled = isSampled;
            this.parentId = parentId;
            this.spanId = spanId;
            this.traceId = traceId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof MdcEntry)) {
                return false;
            }
            MdcEntry mdcEntry = (MdcEntry) o;
            return isSampled == mdcEntry.isSampled &&
                    Objects.equals(parentId, mdcEntry.parentId) &&
                    Objects.equals(spanId, mdcEntry.spanId) &&
                    Objects.equals(traceId, mdcEntry.traceId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(isSampled, parentId, spanId, traceId);
        }
    }
}
