package io.quarkus.opentelemetry.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;

import jakarta.inject.Inject;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizer;
import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizerProvider;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.ResourceProvider;
import io.opentelemetry.sdk.autoconfigure.spi.internal.AutoConfigureListener;
import io.opentelemetry.sdk.resources.Resource;
import io.quarkus.opentelemetry.runtime.declarativeconfig.QuarkusDeclarativeSdk;
import io.quarkus.test.QuarkusExtensionTest;

/**
 * A {@link ResourceProvider} and an {@link AutoConfigurationCustomizerProvider} that implement
 * {@link AutoConfigureListener} receive the built SDK, as on the autoconfigure path, even when another listener
 * throws.
 */
public class SpiAutoConfigureListenerTest {

    @RegisterExtension
    static final QuarkusExtensionTest TEST = new QuarkusExtensionTest()
            .setArchiveProducer(
                    () -> ShrinkWrap.create(JavaArchive.class)
                            .addClasses(ThrowingResourceProvider.class, ListeningResourceProvider.class,
                                    ListeningCustomizerProvider.class)
                            .addAsResource(new StringAsset(ThrowingResourceProvider.class.getName() + "\n"
                                    + ListeningResourceProvider.class.getName()),
                                    "META-INF/services/io.opentelemetry.sdk.autoconfigure.spi.ResourceProvider")
                            .addAsResource(new StringAsset(ListeningCustomizerProvider.class.getName()),
                                    "META-INF/services/io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizerProvider")
                            .add(new StringAsset(
                                    """
                                            quarkus.otel.traces.exporter=none
                                            quarkus.otel.metrics.exporter=none
                                            quarkus.otel.logs.exporter=none
                                            quarkus.datasource.devservices.enabled=false
                                            """),
                                    "application.properties"))
            .setLogRecordPredicate(record -> QuarkusDeclarativeSdk.class.getName().equals(record.getLoggerName())
                    && record.getLevel().intValue() >= Level.WARNING.intValue())
            .assertLogRecords(records -> {
                // the declarative engine calls the listeners, not the legacy one
                assertThat(records).noneSatisfy(record -> assertThat(record.getMessage()).contains("legacy"));
                // the failing listener is named, and the others are still called
                assertThat(records).anySatisfy(record -> assertThat(record.getParameters())
                        .contains(ThrowingResourceProvider.class.getName()));
            });

    @Inject
    OpenTelemetry openTelemetry;

    @Test
    void listenersReceiveTheSdk() {
        // forces the synthetic OpenTelemetry bean
        assertThat(openTelemetry.getTracer("test")).isNotNull();

        assertThat(ListeningResourceProvider.SDK.get()).isNotNull();
        assertThat(ListeningResourceProvider.SDK.get().getTracerProvider()).isSameAs(openTelemetry.getTracerProvider());
        assertThat(ListeningCustomizerProvider.SDK.get()).isSameAs(ListeningResourceProvider.SDK.get());
    }

    public static class ThrowingResourceProvider implements ResourceProvider, AutoConfigureListener {

        @Override
        public Resource createResource(ConfigProperties config) {
            return Resource.empty();
        }

        @Override
        public int order() {
            return 1;
        }

        @Override
        public void afterAutoConfigure(OpenTelemetrySdk sdk) {
            throw new IllegalStateException("listener failure");
        }
    }

    public static class ListeningResourceProvider implements ResourceProvider, AutoConfigureListener {

        static final AtomicReference<OpenTelemetrySdk> SDK = new AtomicReference<>();

        @Override
        public Resource createResource(ConfigProperties config) {
            return Resource.empty();
        }

        @Override
        public int order() {
            return 2;
        }

        @Override
        public void afterAutoConfigure(OpenTelemetrySdk sdk) {
            SDK.set(sdk);
        }
    }

    public static class ListeningCustomizerProvider implements AutoConfigurationCustomizerProvider, AutoConfigureListener {

        static final AtomicReference<OpenTelemetrySdk> SDK = new AtomicReference<>();

        @Override
        public void customize(AutoConfigurationCustomizer autoConfiguration) {
        }

        @Override
        public void afterAutoConfigure(OpenTelemetrySdk sdk) {
            SDK.set(sdk);
        }
    }
}
