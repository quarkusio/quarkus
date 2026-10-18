package io.quarkus.observation.opentelemetry.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

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
import io.micrometer.observation.ObservationHandler.FirstMatchingCompositeObservationHandler;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.observation.tck.TestObservationRegistry;
import io.micrometer.observation.transport.Kind;
import io.micrometer.observation.transport.ReceiverContext;
import io.micrometer.observation.transport.SenderContext;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.quarkus.arc.Unremovable;
import io.quarkus.observation.opentelemetry.deployment.common.TestSpanExporter;
import io.quarkus.observation.opentelemetry.deployment.common.TestSpanExporterProvider;
import io.quarkus.observation.opentelemetry.handler.OpenTelemetryObservationHandler;
import io.quarkus.observation.opentelemetry.handler.PropagatingReceiverTracingObservationHandler;
import io.quarkus.observation.opentelemetry.handler.PropagatingSenderTracingObservationHandler;
import io.quarkus.test.QuarkusExtensionTest;

public class PropagatingTracingObservationHandlerTest {

    @RegisterExtension
    static final QuarkusExtensionTest TEST = new QuarkusExtensionTest()
            .setArchiveProducer(
                    () -> ShrinkWrap.create(JavaArchive.class)
                            .addClasses(TestSpanExporter.class, TestSpanExporterProvider.class,
                                    TestRegistryProducer.class)
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

    @AfterEach
    void tearDown() {
        spanExporter.reset();
        simpleMeterRegistry.clear();
    }

    @Test
    void senderInjectsTraceContextAsClient() {
        Map<String, String> carrier = new HashMap<>();
        SenderContext<Map<String, String>> senderContext = new SenderContext<>(
                (c, key, value) -> c.put(key, value), Kind.CLIENT);
        senderContext.setCarrier(carrier);

        Observation observation = Observation.createNotStarted("client-call", () -> senderContext, registry);
        observation.start();
        try (Observation.Scope ignored = observation.openScope()) {
            // simulate work
        }
        observation.stop();

        List<SpanData> spans = spanExporter.getFinishedSpanItems(1);
        SpanData span = spans.get(0);
        assertThat(span.getName()).isEqualTo("client-call");
        assertThat(span.getKind()).isEqualTo(SpanKind.CLIENT);
        assertThat(span.getStatus().getStatusCode()).isEqualTo(StatusCode.OK);

        assertThat(carrier).containsKey("traceparent");
        String traceparent = carrier.get("traceparent");
        assertThat(traceparent).contains(span.getTraceId());
        assertThat(traceparent).contains(span.getSpanId());

        Timer timer = simpleMeterRegistry.find("client-call").timer();
        assertThat(timer).isNotNull();
        assertThat(timer.count()).isEqualTo(1);
    }

    @Test
    void senderCreatesProducerSpan() {
        Map<String, String> carrier = new HashMap<>();
        SenderContext<Map<String, String>> senderContext = new SenderContext<>(
                (c, key, value) -> c.put(key, value), Kind.PRODUCER);
        senderContext.setCarrier(carrier);

        Observation observation = Observation.createNotStarted("produce-message", () -> senderContext, registry);
        observation.start();
        try (Observation.Scope ignored = observation.openScope()) {
            // simulate sending
        }
        observation.stop();

        List<SpanData> spans = spanExporter.getFinishedSpanItems(1);
        SpanData span = spans.get(0);
        assertThat(span.getName()).isEqualTo("produce-message");
        assertThat(span.getKind()).isEqualTo(SpanKind.PRODUCER);
        assertThat(carrier).containsKey("traceparent");

        Timer timer = simpleMeterRegistry.find("produce-message").timer();
        assertThat(timer).isNotNull();
        assertThat(timer.count()).isEqualTo(1);
    }

    @Test
    void receiverExtractsTraceContextAsServer() {
        Map<String, String> carrier = new HashMap<>();
        SenderContext<Map<String, String>> senderContext = new SenderContext<>(
                (c, key, value) -> c.put(key, value), Kind.CLIENT);
        senderContext.setCarrier(carrier);

        Observation sender = Observation.createNotStarted("client-request", () -> senderContext, registry);
        sender.start();
        try (Observation.Scope ignored = sender.openScope()) {
            // simulate sending
        }
        sender.stop();

        ReceiverContext<Map<String, String>> receiverContext = new ReceiverContext<>(
                (c, key) -> c.get(key), Kind.SERVER);
        receiverContext.setCarrier(carrier);

        Observation receiver = Observation.createNotStarted("server-handle", () -> receiverContext, registry);
        receiver.start();
        try (Observation.Scope ignored = receiver.openScope()) {
            // simulate handling
        }
        receiver.stop();

        List<SpanData> spans = spanExporter.getFinishedSpanItems(2);
        SpanData senderSpan = spans.stream()
                .filter(s -> "client-request".equals(s.getName())).findFirst().orElseThrow();
        SpanData receiverSpan = spans.stream()
                .filter(s -> "server-handle".equals(s.getName())).findFirst().orElseThrow();

        assertThat(senderSpan.getKind()).isEqualTo(SpanKind.CLIENT);
        assertThat(receiverSpan.getKind()).isEqualTo(SpanKind.SERVER);
        assertThat(receiverSpan.getTraceId()).isEqualTo(senderSpan.getTraceId());
        assertThat(receiverSpan.getParentSpanId()).isEqualTo(senderSpan.getSpanId());

        Timer clientTimer = simpleMeterRegistry.find("client-request").timer();
        assertThat(clientTimer).isNotNull();
        assertThat(clientTimer.count()).isEqualTo(1);

        Timer serverTimer = simpleMeterRegistry.find("server-handle").timer();
        assertThat(serverTimer).isNotNull();
        assertThat(serverTimer.count()).isEqualTo(1);
    }

    @Test
    void receiverCreatesConsumerSpan() {
        Map<String, String> carrier = new HashMap<>();
        SenderContext<Map<String, String>> senderContext = new SenderContext<>(
                (c, key, value) -> c.put(key, value), Kind.PRODUCER);
        senderContext.setCarrier(carrier);

        Observation sender = Observation.createNotStarted("produce", () -> senderContext, registry);
        sender.start();
        try (Observation.Scope ignored = sender.openScope()) {
            // simulate sending
        }
        sender.stop();

        ReceiverContext<Map<String, String>> receiverContext = new ReceiverContext<>(
                (c, key) -> c.get(key), Kind.CONSUMER);
        receiverContext.setCarrier(carrier);

        Observation receiver = Observation.createNotStarted("consume", () -> receiverContext, registry);
        receiver.start();
        try (Observation.Scope ignored = receiver.openScope()) {
            // simulate sending
        }
        receiver.stop();

        List<SpanData> spans = spanExporter.getFinishedSpanItems(2);
        SpanData producerSpan = spans.stream()
                .filter(s -> "produce".equals(s.getName())).findFirst().orElseThrow();
        SpanData consumerSpan = spans.stream()
                .filter(s -> "consume".equals(s.getName())).findFirst().orElseThrow();

        assertThat(producerSpan.getKind()).isEqualTo(SpanKind.PRODUCER);
        assertThat(consumerSpan.getKind()).isEqualTo(SpanKind.CONSUMER);
        assertThat(consumerSpan.getTraceId()).isEqualTo(producerSpan.getTraceId());
        assertThat(consumerSpan.getParentSpanId()).isEqualTo(producerSpan.getSpanId());

        Timer producerTimer = simpleMeterRegistry.find("produce").timer();
        assertThat(producerTimer).isNotNull();
        assertThat(producerTimer.count()).isEqualTo(1);

        Timer consumerTimer = simpleMeterRegistry.find("consume").timer();
        assertThat(consumerTimer).isNotNull();
        assertThat(consumerTimer.count()).isEqualTo(1);
    }

    @ApplicationScoped
    public static class TestRegistryProducer {

        @Produces
        @Singleton
        @Alternative
        @Unremovable
        @Priority(Integer.MAX_VALUE)
        TestObservationRegistry testObservationRegistry(
                OpenTelemetryObservationHandler tracingHandler,
                PropagatingSenderTracingObservationHandler senderHandler,
                PropagatingReceiverTracingObservationHandler receiverHandler) {
            Metrics.addRegistry(simpleMeterRegistry);
            TestObservationRegistry registry = TestObservationRegistry.create();
            registry.observationConfig().observationHandler(
                    new FirstMatchingCompositeObservationHandler(
                            List.of(receiverHandler, senderHandler, tracingHandler)));
            registry.observationConfig().observationHandler(
                    new DefaultMeterObservationHandler(simpleMeterRegistry));
            return registry;
        }
    }
}
