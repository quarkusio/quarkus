package io.quarkus.opentelemetry.deployment.instrumentation;

import static io.opentelemetry.api.trace.SpanKind.CONSUMER;
import static io.opentelemetry.api.trace.SpanKind.PRODUCER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.opentelemetry.sdk.trace.data.SpanData;
import io.quarkus.opentelemetry.deployment.common.TestUtil;
import io.quarkus.opentelemetry.deployment.common.exporter.TestSpanExporter;
import io.quarkus.opentelemetry.deployment.common.exporter.TestSpanExporterProvider;
import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.vertx.ConsumeEvent;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.tracing.TracingPolicy;

/**
 * The event bus VertxTracer must honor the Vert.x {@link TracingPolicy}. A message sent with the default
 * {@code PROPAGATE} policy while no trace is active must not create a span; an {@code ALWAYS} message must
 * be traced regardless.
 *
 * @see <a href="https://github.com/quarkusio/quarkus/issues/25417">#25417</a>
 */
public class VertxEventBusTracingPolicyTest {

    @RegisterExtension
    static final QuarkusUnitTest unitTest = new QuarkusUnitTest()
            .withApplicationRoot(root -> root
                    .addClasses(Consumers.class, TestUtil.class, TestSpanExporter.class, TestSpanExporterProvider.class)
                    .addAsResource(new StringAsset(TestSpanExporterProvider.class.getCanonicalName()),
                            "META-INF/services/io.opentelemetry.sdk.autoconfigure.spi.traces.ConfigurableSpanExporterProvider"))
            .overrideConfigKey("quarkus.otel.traces.exporter", "test-span-exporter")
            .overrideConfigKey("quarkus.otel.metrics.exporter", "none")
            .overrideConfigKey("quarkus.otel.logs.exporter", "none")
            .overrideConfigKey("quarkus.otel.bsp.schedule.delay", "200");

    @Inject
    EventBus eventBus;

    @Inject
    TestSpanExporter spanExporter;

    @AfterEach
    void tearDown() {
        spanExporter.reset();
    }

    @Test
    void propagateWithoutActiveTrace_isNotTraced() {
        // No trace is active on the calling thread.
        eventBus.publish("propagate", "hello", new DeliveryOptions().setTracingPolicy(TracingPolicy.PROPAGATE));
        // ALWAYS is traced unconditionally, giving a deterministic span count to wait for.
        eventBus.publish("always", "hello", new DeliveryOptions().setTracingPolicy(TracingPolicy.ALWAYS));

        // Only the ALWAYS message is traced (PRODUCER + CONSUMER); the PROPAGATE message adds nothing.
        List<SpanData> spans = spanExporter.getFinishedSpanItems(2);
        assertTrue(spans.stream().noneMatch(span -> "propagate".equals(destination(span))),
                "The PROPAGATE message must not be traced, but got: " + spans);
    }

    @Test
    void alwaysWithoutActiveTrace_isTraced() {
        eventBus.publish("always", "hello", new DeliveryOptions().setTracingPolicy(TracingPolicy.ALWAYS));

        List<SpanData> spans = spanExporter.getFinishedSpanItems(2);
        assertEquals(1, spans.stream().filter(span -> span.getKind() == PRODUCER).count());
        assertEquals(1, spans.stream().filter(span -> span.getKind() == CONSUMER).count());
    }

    private static String destination(SpanData span) {
        return span.getAttributes().get(io.opentelemetry.api.common.AttributeKey.stringKey("messaging.destination.name"));
    }

    @Singleton
    public static class Consumers {

        @ConsumeEvent("propagate")
        void propagate(String ignored) {
        }

        @ConsumeEvent("always")
        void always(String ignored) {
        }
    }
}
