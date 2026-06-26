package io.quarkus.opentelemetry.observation.deployment;

import static io.opentelemetry.api.trace.SpanKind.INTERNAL;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import jakarta.inject.Inject;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.quarkus.opentelemetry.observation.deployment.common.TestSpanExporter;
import io.quarkus.opentelemetry.observation.deployment.common.TestSpanExporterProvider;
import io.quarkus.test.QuarkusExtensionTest;

public class BasicTracingTest {

    @RegisterExtension
    static final QuarkusExtensionTest TEST = new QuarkusExtensionTest()
            .setArchiveProducer(
                    () -> ShrinkWrap.create(JavaArchive.class)
                            .addClasses(TestSpanExporter.class, TestSpanExporterProvider.class)
                            .addAsManifestResource(
                                    "META-INF/services-config/io.opentelemetry.sdk.autoconfigure.spi.traces.ConfigurableSpanExporterProvider",
                                    "services/io.opentelemetry.sdk.autoconfigure.spi.traces.ConfigurableSpanExporterProvider")
                            .addAsResource(new StringAsset(
                                    "quarkus.otel.traces.exporter=test-span-exporter\n" +
                                            "quarkus.otel.bsp.schedule.delay=50ms\n" +
                                            "quarkus.otel.metrics.exporter=none\n"),
                                    "application.properties"));

    @Inject
    ObservationRegistry registry;

    @Inject
    TestSpanExporter spanExporter;

    @AfterEach
    void tearDown() {
        spanExporter.reset();
    }

    @Test
    void basicObservationProducesSpan() {
        Observation observation = Observation.createNotStarted("test-operation", registry);
        observation.lowCardinalityKeyValue("test.key", "test-value");
        observation.start();
        try (Observation.Scope scope = observation.openScope()) {
            // simulate work
        }
        observation.stop();

        List<SpanData> spans = spanExporter.getFinishedSpanItems(1);
        SpanData span = spans.get(0);
        assertThat(span.getName()).isEqualTo("test-operation");
        assertThat(span.getKind()).isEqualTo(INTERNAL);
        assertThat(span.getStatus().getStatusCode()).isEqualTo(StatusCode.OK);
        assertThat(span.getAttributes().get(io.opentelemetry.api.common.AttributeKey.stringKey("test.key")))
                .isEqualTo("test-value");
    }

    @Test
    void observationWithContextualNameUpdatesSpanName() {
        Observation observation = Observation.createNotStarted("metric-name", registry);
        observation.contextualName("GET /api/greeting");
        observation.start();
        try (Observation.Scope scope = observation.openScope()) {
            // simulate work
        }
        observation.stop();

        List<SpanData> spans = spanExporter.getFinishedSpanItems(1);
        assertThat(spans.get(0).getName()).isEqualTo("GET /api/greeting");
    }

    @Test
    void observationErrorSetsSpanStatus() {
        Observation observation = Observation.start("error-operation", registry);
        try (Observation.Scope scope = observation.openScope()) {
            throw new RuntimeException("test error");
        } catch (Exception e) {
            observation.error(e);
        } finally {
            observation.stop();
        }

        List<SpanData> spans = spanExporter.getFinishedSpanItems(1);
        SpanData span = spans.get(0);
        assertThat(span.getStatus().getStatusCode()).isEqualTo(StatusCode.ERROR);
        assertThat(span.getStatus().getDescription()).isEqualTo("test error");
        assertThat(span.getEvents()).isNotEmpty();
    }

    @Test
    void observationEventMapsToSpanEvent() {
        Observation observation = Observation.start("event-operation", registry);
        try (Observation.Scope scope = observation.openScope()) {
            observation.event(Observation.Event.of("checkpoint"));
        }
        observation.stop();

        List<SpanData> spans = spanExporter.getFinishedSpanItems(1);
        SpanData span = spans.get(0);
        assertThat(span.getEvents()).hasSize(1);
        assertThat(span.getEvents().get(0).getName()).isEqualTo("checkpoint");
    }

    @Test
    void scopelessObservationProducesSpan() {
        Observation observation = Observation.start("scopeless", registry);
        observation.highCardinalityKeyValue("result", "42");
        observation.stop();

        List<SpanData> spans = spanExporter.getFinishedSpanItems(1);
        SpanData span = spans.get(0);
        assertThat(span.getName()).isEqualTo("scopeless");
        assertThat(span.getAttributes().get(io.opentelemetry.api.common.AttributeKey.stringKey("result")))
                .isEqualTo("42");
    }

    @Test
    void nestedObservationsProduceParentChildSpans() {
        Observation parent = Observation.start("parent-op", registry);
        try (Observation.Scope parentScope = parent.openScope()) {
            Observation child = Observation.start("child-op", registry);
            try (Observation.Scope childScope = child.openScope()) {
                // nested work
            }
            child.stop();
        }
        parent.stop();

        List<SpanData> spans = spanExporter.getFinishedSpanItems(2);
        SpanData childSpan = spans.stream().filter(s -> s.getName().equals("child-op")).findFirst().orElseThrow();
        SpanData parentSpan = spans.stream().filter(s -> s.getName().equals("parent-op")).findFirst().orElseThrow();

        assertThat(childSpan.getTraceId()).isEqualTo(parentSpan.getTraceId());
        assertThat(childSpan.getParentSpanId()).isEqualTo(parentSpan.getSpanId());
    }

    @Test
    void registryIsInjectable() {
        assertThat(registry).isNotNull();
        assertThat(registry).isNotEqualTo(ObservationRegistry.NOOP);
    }
}
