package io.quarkus.opentelemetry.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.atomic.AtomicReference;

import jakarta.inject.Inject;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.internal.AutoConfigureListener;
import io.opentelemetry.sdk.autoconfigure.spi.traces.ConfigurableSpanExporterProvider;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import io.quarkus.test.QuarkusExtensionTest;

/**
 * A {@link ConfigurableSpanExporterProvider} that implements {@link AutoConfigureListener} receives the built SDK, as
 * on the autoconfigure path. The OTLP exporter providers rely on this for their self-metrics.
 */
public class ConfigurableProviderAutoConfigureListenerTest {

    @RegisterExtension
    static final QuarkusExtensionTest TEST = new QuarkusExtensionTest()
            .setArchiveProducer(
                    () -> ShrinkWrap.create(JavaArchive.class)
                            .addClasses(ListeningSpanExporterProvider.class)
                            .addAsResource(new StringAsset(ListeningSpanExporterProvider.class.getName()),
                                    "META-INF/services/io.opentelemetry.sdk.autoconfigure.spi.traces.ConfigurableSpanExporterProvider")
                            .add(new StringAsset(
                                    """
                                            quarkus.otel.traces.exporter=listening
                                            quarkus.otel.metrics.exporter=none
                                            quarkus.otel.logs.exporter=none
                                            quarkus.datasource.devservices.enabled=false
                                            """),
                                    "application.properties"));

    @Inject
    OpenTelemetry openTelemetry;

    @Test
    void listenerReceivesTheSdk() {
        // forces the synthetic OpenTelemetry bean
        assertThat(openTelemetry.getTracer("test")).isNotNull();

        // the SDK behind the OpenTelemetry bean
        assertThat(ListeningSpanExporterProvider.SDK.get()).isNotNull();
        assertThat(ListeningSpanExporterProvider.SDK.get().getTracerProvider())
                .isSameAs(openTelemetry.getTracerProvider());
    }

    public static class ListeningSpanExporterProvider implements ConfigurableSpanExporterProvider, AutoConfigureListener {

        static final AtomicReference<OpenTelemetrySdk> SDK = new AtomicReference<>();

        @Override
        public SpanExporter createExporter(ConfigProperties config) {
            return SpanExporter.composite();
        }

        @Override
        public String getName() {
            return "listening";
        }

        @Override
        public void afterAutoConfigure(OpenTelemetrySdk sdk) {
            SDK.set(sdk);
        }
    }
}
