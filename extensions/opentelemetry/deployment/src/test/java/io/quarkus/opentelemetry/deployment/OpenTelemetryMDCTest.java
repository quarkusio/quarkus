package io.quarkus.opentelemetry.deployment;

import static io.opentelemetry.api.trace.SpanKind.INTERNAL;
import static io.opentelemetry.api.trace.SpanKind.SERVER;
import static io.quarkus.opentelemetry.deployment.common.exporter.TestSpanExporter.getSpanByKindAndParentId;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.stream.Collectors;

import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.container.PreMatching;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.ext.Provider;

import org.eclipse.microprofile.context.ManagedExecutor;
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
import io.quarkus.vertx.core.runtime.VertxMDC;
import io.restassured.RestAssured;

public class OpenTelemetryMDCTest {
    @RegisterExtension
    static final QuarkusUnitTest unitTest = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addPackage(TestSpanExporter.class.getPackage())
                    .addClass(MdcTracingEntry.class)
                    .addClass(TestMdcCapturer.class)
                    .addClass(TestResource.class)
                    .addClass(GreetingResource.class)
                    .addClass(ReqRespFilter.class)
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
        List<MdcTracingEntry> mdcEntries = testMdcCapturer.getCapturedMdcTracingEntries();
        List<MdcTracingEntry> fromSpans = getExpectedMDCEntries(spans);

        final SpanData server = getSpanByKindAndParentId(spans, SERVER, "0000000000000000");
        assertEquals("GET /hello", server.getName());

        final SpanData programmatic = getSpanByKindAndParentId(spans, INTERNAL, server.getSpanId());
        assertEquals("something", programmatic.getName());

        assertEquals(fromSpans, mdcEntries);

        // ReqRespFilter MDC entries are carried.
        testMdcCapturer.getFullMDCEntries().stream()
                .forEach(map -> {
                    assertThat(map).isNotEmpty();
                    assertThat(map.get(ReqRespFilter.REQUEST_METHOD_FIELD))
                            .withFailMessage(() -> "Failing map: " + map)
                            .isEqualTo(ReqRespFilter.REQUEST_METHOD_FIELD_VALUE);
                });
    }

    @Test
    void vertxAsync() {
        RestAssured.when()
                .get("/async").then()
                .statusCode(200)
                .body(is("Hello from Quarkus REST"));

        List<SpanData> spans = spanExporter.getFinishedSpanItems(5);

        final SpanData server = getSpanByKindAndParentId(spans, SERVER, "0000000000000000");
        assertEquals("GET /async", server.getName());

        List<MdcTracingEntry> expectedMdcEntriesFromSpans = getExpectedMDCEntries(spans);
        assertThat(testMdcCapturer.getCapturedMdcTracingEntries().size()).isEqualTo(6);

        List<MdcTracingEntry> mdcEntries = testMdcCapturer.getCapturedMdcTracingEntries();
        // 2 mdcEntries are repeated.
        assertThat(expectedMdcEntriesFromSpans).containsAll(mdcEntries.stream().distinct().toList());

        assertThat(testMdcCapturer.getCapturedMdcTracingEntries().stream()
                .filter(mdcTracingEntry -> mdcTracingEntry.parentId.equals("null"))
                .count())
                .withFailMessage("There must be 2 MDC entries for the parent span")
                .isEqualTo(2);

        assertThat(testMdcCapturer.getCapturedMdcTracingEntries().stream()
                .filter(mdcTracingEntry -> mdcTracingEntry.parentId.equals(server.getSpanId()))
                .count())
                .withFailMessage("There must be 4 MDC entries in child spans")
                .isEqualTo(4);

        // ReqRespFilter MDC entries are carried.
        testMdcCapturer.getFullMDCEntries().stream()
                .forEach(map -> {
                    assertThat(map).isNotEmpty();
                    assertThat(map.get(ReqRespFilter.REQUEST_METHOD_FIELD))
                            .withFailMessage(() -> "Failing map: " + map)
                            .isEqualTo(ReqRespFilter.REQUEST_METHOD_FIELD_VALUE);
                });
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
        List<MdcTracingEntry> mdcEntries = testMdcCapturer.getCapturedMdcTracingEntries();
        List<MdcTracingEntry> fromSpans = getExpectedMDCEntries(spans);

        final SpanData parent = getSpanByKindAndParentId(spans, INTERNAL, "0000000000000000");
        assertEquals("parent", parent.getName());

        final SpanData child = getSpanByKindAndParentId(spans, INTERNAL, parent.getSpanId());
        assertEquals("child", child.getName());

        assertEquals(fromSpans, mdcEntries);
    }

    private List<MdcTracingEntry> getExpectedMDCEntries(List<SpanData> spans) {
        return spans.stream()
                .map(spanData -> new MdcTracingEntry(spanData.getSpanContext().isSampled(),
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

    @Path("/async")
    public static class GreetingResource {
        @Inject
        TestMdcCapturer testMdcCapturer;

        @Inject
        ManagedExecutor managedExecutor;

        @Inject
        Tracer tracer;

        @GET
        @Produces(MediaType.TEXT_PLAIN)
        public String hello() {
            // 1 span from REST
            testMdcCapturer.captureMdc();
            for (int i = 0; i < 3; i++) {
                managedExecutor.execute(() -> {
                    Span asyncSpan = tracer.spanBuilder("async hello").startSpan();
                    try (Scope scope = asyncSpan.makeCurrent()) {
                        executeWorkOnWorkerThread();
                        // 3 manual async spans
                        testMdcCapturer.captureMdc();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    } finally {
                        asyncSpan.end();
                    }
                });
            }

            Span syncSpan = tracer.spanBuilder("sync hello").startSpan();
            try (Scope scope = syncSpan.makeCurrent()) {
                executeWorkOnWorkerThread();
                // 1 sync span
                testMdcCapturer.captureMdc();
            } catch (Exception e) {
                throw new RuntimeException(e);
            } finally {
                syncSpan.end();
            }
            // 5 spans total, 6 MDC captures
            testMdcCapturer.captureMdc();
            return "Hello from Quarkus REST";
        }

        private void executeWorkOnWorkerThread() {
            try {
                Random random = new Random();
                Thread.sleep(100 + random.nextInt(400));
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @PreMatching
    @ApplicationScoped
    @Provider
    @Priority(Integer.MAX_VALUE)
    public static class ReqRespFilter implements ContainerResponseFilter, ContainerRequestFilter {
        public static final String REQUEST_METHOD_FIELD = "request.filter.field";
        public static final String REQUEST_METHOD_FIELD_VALUE = "from the request filter";

        @Override
        public void filter(ContainerRequestContext requestContext) {
            VertxMDC.INSTANCE.put(REQUEST_METHOD_FIELD, REQUEST_METHOD_FIELD_VALUE);
        }

        @Override
        public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) {
        }
    }

    @Unremovable
    @ApplicationScoped
    public static class TestMdcCapturer {
        private final List<MdcTracingEntry> mdcTracingEntries = Collections.synchronizedList(new ArrayList<>());
        private final List<Map<String, Object>> fullMDCEntries = Collections.synchronizedList(new ArrayList<>());

        public void reset() {
            mdcTracingEntries.clear();
            fullMDCEntries.clear();
        }

        public void captureMdc() {
            Map<String, Object> map = new HashMap<>(MDC.getMap());
            fullMDCEntries.add(map);
            mdcTracingEntries.add(new MdcTracingEntry(
                    Boolean.parseBoolean(String.valueOf(map.get("sampled"))),
                    String.valueOf(map.get("parentId")),
                    String.valueOf(map.get("spanId")),
                    String.valueOf(map.get("traceId"))));
        }

        public List<MdcTracingEntry> getCapturedMdcTracingEntries() {
            return List.copyOf(mdcTracingEntries);
        }

        public List<Map> getFullMDCEntries() {
            return List.copyOf(fullMDCEntries);
        }
    }

    public static class MdcTracingEntry {
        public final boolean isSampled;
        public final String parentId;
        public final String spanId;
        public final String traceId;

        public MdcTracingEntry(boolean isSampled, String parentId, String spanId, String traceId) {
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
            if (!(o instanceof MdcTracingEntry)) {
                return false;
            }
            MdcTracingEntry mdcTracingEntry = (MdcTracingEntry) o;
            return isSampled == mdcTracingEntry.isSampled &&
                    Objects.equals(parentId, mdcTracingEntry.parentId) &&
                    Objects.equals(spanId, mdcTracingEntry.spanId) &&
                    Objects.equals(traceId, mdcTracingEntry.traceId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(isSampled, parentId, spanId, traceId);
        }

        @Override
        public String toString() {
            return "spanId: " + spanId + " traceId: " + traceId + " parentId: " + parentId;
        }
    }
}
