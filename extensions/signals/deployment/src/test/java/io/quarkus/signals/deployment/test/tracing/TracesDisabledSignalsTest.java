package io.quarkus.signals.deployment.test.tracing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.List;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.quarkus.builder.Version;
import io.quarkus.maven.dependency.Dependency;
import io.quarkus.signals.Receives;
import io.quarkus.signals.Signal;
import io.quarkus.test.QuarkusExtensionTest;

/**
 * Verifies that no signal spans are created when tracing is disabled via configuration, even though the OpenTelemetry
 * extension is present.
 */
public class TracesDisabledSignalsTest {

    @RegisterExtension
    static final QuarkusExtensionTest test = new QuarkusExtensionTest()
            .withApplicationRoot(root -> root
                    .addClasses(PingReceiver.class, Ping.class, InMemorySpanExporterProducer.class)
                    .addAsResource(new StringAsset("""
                            quarkus.signals.telemetry.traces.enabled=false
                            quarkus.otel.traces.sampler=always_on
                            quarkus.otel.bsp.export.timeout=1s
                            quarkus.otel.bsp.schedule.delay=50
                            """), "application.properties"))
            .setForcedDependencies(
                    List.of(Dependency.of("io.quarkus", "quarkus-opentelemetry-deployment", Version.getVersion())));

    @Inject
    Signal<Ping> ping;

    @Inject
    InMemorySpanExporter exporter;

    @Inject
    Tracer tracer;

    @Test
    public void testNoSignalSpanWhenDisabled() {
        Span parent = tracer.spanBuilder("test-parent").startSpan();
        String result;
        try (Scope scope = parent.makeCurrent()) {
            result = ping.reactive().request(new Ping("hello"), String.class)
                    .ifNoItem().after(Duration.ofSeconds(5)).fail()
                    .await().indefinitely();
        } finally {
            parent.end();
        }
        assertEquals("hello", result);

        // Give any (unexpected) span a chance to be exported
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        List<SpanData> receiveSpans = exporter.getFinishedSpanItems().stream()
                .filter(s -> s.getName().equals("signal receive"))
                .toList();
        assertTrue(receiveSpans.isEmpty(), "No receiver span must be created when tracing is disabled");
    }

    record Ping(String value) {
    }

    @Singleton
    public static class PingReceiver {

        String ping(@Receives Ping ping) {
            return ping.value();
        }
    }
}
