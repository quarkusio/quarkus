package io.quarkus.opentelemetry.deployment;

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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.quarkus.arc.Unremovable;
import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class OpenTelemetryMDCTest {
    @RegisterExtension
    static final QuarkusUnitTest unitTest = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClass(MdcEntry.class)
                    .addClass(TestMdcCapturer.class)
                    .addClass(TestSpanExporter.class)
                    .addClass(TestResource.class));

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

        assertEquals("something", spans.get(0).getName());
        assertEquals("/hello", spans.get(1).getName());
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

        assertEquals("child", spans.get(0).getName());
        assertEquals("parent", spans.get(1).getName());
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
