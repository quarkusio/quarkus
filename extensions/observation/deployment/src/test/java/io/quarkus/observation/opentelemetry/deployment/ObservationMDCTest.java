package io.quarkus.observation.opentelemetry.deployment;

import static io.opentelemetry.api.trace.SpanKind.INTERNAL;
import static io.opentelemetry.api.trace.SpanKind.SERVER;
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
import jakarta.enterprise.inject.Alternative;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
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
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.observation.DefaultMeterObservationHandler;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.observation.tck.TestObservationRegistry;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.quarkus.arc.Unremovable;
import io.quarkus.observation.opentelemetry.deployment.common.TestSpanExporter;
import io.quarkus.observation.opentelemetry.deployment.common.TestSpanExporterProvider;
import io.quarkus.observation.opentelemetry.handler.OpenTelemetryObservationHandler;
import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.vertx.core.runtime.VertxMDC;
import io.restassured.RestAssured;

/**
 * Based on OpenTelemetryMDCTest
 */
public class ObservationMDCTest {

    @RegisterExtension
    static final QuarkusExtensionTest TEST = new QuarkusExtensionTest()
            .setArchiveProducer(
                    () -> ShrinkWrap.create(JavaArchive.class)
                            .addClasses(TestSpanExporter.class, TestSpanExporterProvider.class,
                                    MdcTracingEntry.class, TestMdcCapturer.class,
                                    TestResource.class, GreetingResource.class,
                                    ReqRespFilter.class, TestRegistryProducer.class)
                            .addAsManifestResource(
                                    "META-INF/services-config/io.opentelemetry.sdk.autoconfigure.spi.traces.ConfigurableSpanExporterProvider",
                                    "services/io.opentelemetry.sdk.autoconfigure.spi.traces.ConfigurableSpanExporterProvider")
                            .addAsResource(new StringAsset(
                                    "quarkus.otel.traces.exporter=test-span-exporter\n" +
                                            "quarkus.otel.bsp.schedule.delay=50ms\n" +
                                            "quarkus.otel.metrics.exporter=none\n"),
                                    "application.properties"));

    static final SimpleMeterRegistry simpleMeterRegistry = new SimpleMeterRegistry();

    @Inject
    ObservationRegistry registry;

    @Inject
    TestSpanExporter spanExporter;

    @Inject
    TestMdcCapturer testMdcCapturer;

    @AfterEach
    void tearDown() {
        spanExporter.reset();
        testMdcCapturer.reset();
        simpleMeterRegistry.clear();
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
        assertThat(server.getName()).isEqualTo("GET /hello");

        final SpanData observation = getSpanByKindAndParentId(spans, INTERNAL, server.getSpanId());
        assertThat(observation.getName()).isEqualTo("something");

        assertThat(mdcEntries).isEqualTo(fromSpans);

        // ReqRespFilter MDC entries are carried.
        testMdcCapturer.getFullMDCEntries()
                .forEach(map -> {
                    assertThat(map).isNotEmpty();
                    assertThat(map.get(ReqRespFilter.REQUEST_METHOD_FIELD))
                            .withFailMessage(() -> "Failing map: " + map)
                            .isEqualTo(ReqRespFilter.REQUEST_METHOD_FIELD_VALUE);
                });

        // Metrics from the observation
        Timer somethingTimer = simpleMeterRegistry.find("something").timer();
        assertThat(somethingTimer).isNotNull();
        assertThat(somethingTimer.count()).isEqualTo(1);
    }

    @Test
    void vertxAsync() {
        RestAssured.when()
                .get("/async").then()
                .statusCode(200)
                .body(is("Hello from Quarkus REST"));

        List<SpanData> spans = spanExporter.getFinishedSpanItems(5);

        final SpanData server = getSpanByKindAndParentId(spans, SERVER, "0000000000000000");
        assertThat(server.getName()).isEqualTo("GET /async");

        List<MdcTracingEntry> expectedMdcEntriesFromSpans = getExpectedMDCEntries(spans);
        assertThat(testMdcCapturer.getCapturedMdcTracingEntries()).hasSize(6);

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
        testMdcCapturer.getFullMDCEntries()
                .forEach(map -> {
                    assertThat(map).isNotEmpty();
                    assertThat(map.get(ReqRespFilter.REQUEST_METHOD_FIELD))
                            .withFailMessage(() -> "Failing map: " + map)
                            .isEqualTo(ReqRespFilter.REQUEST_METHOD_FIELD_VALUE);
                    assertThat(map.get(ReqRespFilter.REQUEST_METHOD_NON_STRING_FIELD))
                            .withFailMessage(() -> "Failing map: " + map)
                            .isEqualTo(ReqRespFilter.REQUEST_METHOD_NON_STRING_VALUE);
                });

        // Metrics from the observations
        Timer asyncTimer = simpleMeterRegistry.find("async hello").timer();
        assertThat(asyncTimer).isNotNull();
        assertThat(asyncTimer.count()).isEqualTo(3);

        Timer syncTimer = simpleMeterRegistry.find("sync hello").timer();
        assertThat(syncTimer).isNotNull();
        assertThat(syncTimer.count()).isEqualTo(1);
    }

    @Test
    void nonVertx() {
        Observation parent = Observation.start("parent", registry);
        try (Observation.Scope ignored = parent.openScope()) {
            testMdcCapturer.captureMdc();
            Observation child = Observation.start("child", registry);
            try (Observation.Scope ignored1 = child.openScope()) {
                testMdcCapturer.captureMdc();
            }
            child.stop();
        }
        parent.stop();

        List<SpanData> spans = spanExporter.getFinishedSpanItems(2);
        List<MdcTracingEntry> mdcEntries = testMdcCapturer.getCapturedMdcTracingEntries();
        List<MdcTracingEntry> fromSpans = getExpectedMDCEntries(spans);

        final SpanData parentSpan = getSpanByKindAndParentId(spans, INTERNAL, "0000000000000000");
        assertThat(parentSpan.getName()).isEqualTo("parent");

        final SpanData childSpan = getSpanByKindAndParentId(spans, INTERNAL, parentSpan.getSpanId());
        assertThat(childSpan.getName()).isEqualTo("child");

        assertThat(mdcEntries).isEqualTo(fromSpans);

        // Metrics from the observations
        Timer parentTimer = simpleMeterRegistry.find("parent").timer();
        assertThat(parentTimer).isNotNull();
        assertThat(parentTimer.count()).isEqualTo(1);

        Timer childTimer = simpleMeterRegistry.find("child").timer();
        assertThat(childTimer).isNotNull();
        assertThat(childTimer.count()).isEqualTo(1);
    }

    private static SpanData getSpanByKindAndParentId(List<SpanData> spans, SpanKind kind, String parentSpanId) {
        List<SpanData> filtered = spans.stream()
                .filter(s -> s.getKind().equals(kind))
                .filter(s -> s.getParentSpanId().equals(parentSpanId))
                .toList();
        assertEquals(1, filtered.size(), "Expected 1 span with kind=" + kind + " and parentId=" + parentSpanId
                + ", got: " + spans);
        return filtered.get(0);
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
        ObservationRegistry registry;

        @GET
        @Path("/hello")
        public String hello() {
            testMdcCapturer.captureMdc();
            Observation observation = Observation.start("something", registry);
            try (Observation.Scope ignored = observation.openScope()) {
                testMdcCapturer.captureMdc();
            }
            observation.stop();
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
        ObservationRegistry registry;

        @GET
        @Produces(MediaType.TEXT_PLAIN)
        public String hello() {
            // 1 span from REST
            testMdcCapturer.captureMdc();
            for (int i = 0; i < 3; i++) {
                managedExecutor.execute(() -> {
                    Observation observation = Observation.start("async hello", registry);
                    try (Observation.Scope scope = observation.openScope()) {
                        executeWorkOnWorkerThread();
                        // 3 manual async spans
                        testMdcCapturer.captureMdc();
                    } catch (Exception e) {
                        observation.error(e);
                        throw new RuntimeException(e);
                    } finally {
                        observation.stop();
                    }
                });
            }

            Observation syncObservation = Observation.start("sync hello", registry);
            try (Observation.Scope scope = syncObservation.openScope()) {
                executeWorkOnWorkerThread();
                // 1 sync span
                testMdcCapturer.captureMdc();
            } catch (Exception e) {
                syncObservation.error(e);
                throw new RuntimeException(e);
            } finally {
                syncObservation.stop();
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
        public static final String REQUEST_METHOD_NON_STRING_FIELD = "request.filter.non_string_field";
        public static final Map<String, Integer> REQUEST_METHOD_NON_STRING_VALUE = Map.of("hello", 42);

        @Override
        public void filter(ContainerRequestContext requestContext) {
            VertxMDC.INSTANCE.put(REQUEST_METHOD_FIELD, REQUEST_METHOD_FIELD_VALUE);
            VertxMDC.INSTANCE.putObject(REQUEST_METHOD_NON_STRING_FIELD, REQUEST_METHOD_NON_STRING_VALUE);
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

    @ApplicationScoped
    public static class TestRegistryProducer {

        @jakarta.enterprise.inject.Produces
        @Singleton
        @Alternative
        @Unremovable
        @Priority(Integer.MAX_VALUE)
        TestObservationRegistry testObservationRegistry(OpenTelemetryObservationHandler tracingHandler) {
            Metrics.addRegistry(simpleMeterRegistry);
            TestObservationRegistry registry = TestObservationRegistry.create();
            registry.observationConfig().observationHandler(tracingHandler);
            registry.observationConfig().observationHandler(
                    new DefaultMeterObservationHandler(simpleMeterRegistry));
            return registry;
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
