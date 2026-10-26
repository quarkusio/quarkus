package io.quarkus.opentelemetry.runtime.declarativeconfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.UnaryOperator;

import org.junit.jupiter.api.Test;

import io.opentelemetry.api.incubator.config.ConfigProvider;
import io.opentelemetry.api.incubator.config.DeclarativeConfigException;
import io.opentelemetry.api.incubator.config.DeclarativeConfigProperties;
import io.opentelemetry.common.ComponentLoader;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.YamlDeclarativeConfigProperties;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.OpenTelemetryConfigurationModel;
import io.opentelemetry.sdk.autoconfigure.spi.internal.ComponentProvider;
import io.opentelemetry.sdk.autoconfigure.spi.internal.ExtendedDeclarativeConfigProperties;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.common.InternalTelemetryVersion;
import io.opentelemetry.sdk.logs.data.LogRecordData;
import io.opentelemetry.sdk.logs.export.LogRecordExporter;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import io.quarkus.opentelemetry.runtime.declarativeconfig.spi.QuarkusCompositeLogRecordExporterProvider;
import io.quarkus.opentelemetry.runtime.declarativeconfig.spi.QuarkusCompositeSpanExporterProvider;
import io.quarkus.opentelemetry.runtime.declarativeconfig.spi.QuarkusDropAllSpanProcessorProvider;
import io.quarkus.opentelemetry.runtime.declarativeconfig.spi.QuarkusExtendedConfigProperties;

class QuarkusCompositeExporterProviderTest {

    @Test
    void eachExporterIsCustomizedThenComposed() {
        TrackingSpanExporter first = new TrackingSpanExporter();
        TrackingSpanExporter second = new TrackingSpanExporter();
        List<SpanExporter> customized = new ArrayList<>();

        Object exporter = new QuarkusCompositeSpanExporterProvider(recording(customized))
                .create(node(loader(provider(SpanExporter.class, "first", first),
                        provider(SpanExporter.class, "second", second)), "first", "second"));

        // as autoconfigure customized each named exporter, then composed them
        assertThat(customized).containsExactly(first, second);
        ((SpanExporter) exporter).shutdown();
        assertThat(first.shutdown).isTrue();
        assertThat(second.shutdown).isTrue();
    }

    @Test
    void singleExporterIsNotWrapped() {
        TrackingSpanExporter only = new TrackingSpanExporter();

        Object exporter = new QuarkusCompositeSpanExporterProvider(UnaryOperator.identity())
                .create(node(loader(provider(SpanExporter.class, "only", only)), "only"));

        assertThat(exporter).isSameAs(only);
    }

    @Test
    void theCustomizedExporterIsUsed() {
        TrackingSpanExporter only = new TrackingSpanExporter();
        TrackingSpanExporter replacement = new TrackingSpanExporter();

        Object exporter = new QuarkusCompositeSpanExporterProvider(exporterToReplace -> replacement)
                .create(node(loader(provider(SpanExporter.class, "only", only)), "only"));

        assertThat(exporter).isSameAs(replacement);
    }

    @Test
    void builtExportersAreClosedWhenALaterOneIsMissing() {
        TrackingSpanExporter tracked = new TrackingSpanExporter();
        TrackingSpanExporter customizedTracked = new TrackingSpanExporter();

        assertThatThrownBy(() -> new QuarkusCompositeSpanExporterProvider(exporter -> customizedTracked)
                .create(node(loader(provider(SpanExporter.class, "tracked", tracked)), "tracked", "missing")))
                .isInstanceOf(DeclarativeConfigException.class)
                .hasMessageContaining("No SpanExporter named 'missing'");

        assertThat(tracked.shutdown).as("the exporter as resolved").isTrue();
        assertThat(customizedTracked.shutdown).as("the exporter as customized").isTrue();
    }

    @Test
    void exportersReceiveTheParentConfigProvider() {
        AtomicReference<DeclarativeConfigProperties> received = new AtomicReference<>();
        ConfigProvider configProvider = ConfigProvider.noop();
        ComponentProvider recordingProvider = new ComponentProvider() {
            @Override
            public Class<?> getType() {
                return SpanExporter.class;
            }

            @Override
            public String getName() {
                return "recording";
            }

            @Override
            public Object create(DeclarativeConfigProperties config) {
                received.set(config);
                return new TrackingSpanExporter();
            }
        };

        new QuarkusCompositeSpanExporterProvider(UnaryOperator.identity()).create(new QuarkusExtendedConfigProperties(
                node(loader(recordingProvider), "recording"), configProvider));

        // upstream components such as the OTLP exporters require the extended type
        assertThat(received.get()).isInstanceOf(ExtendedDeclarativeConfigProperties.class);
        assertThat(((ExtendedDeclarativeConfigProperties) received.get()).getConfigProvider()).isSameAs(configProvider);
    }

    @Test
    void anEmptyCompositeFails() {
        assertThatThrownBy(() -> new QuarkusCompositeSpanExporterProvider(UnaryOperator.identity())
                .create(node(loader())))
                .isInstanceOf(DeclarativeConfigException.class)
                .hasMessageContaining("at least one exporter");
    }

    @Test
    void logRecordExportersAreCustomizedThenComposed() {
        TrackingLogRecordExporter first = new TrackingLogRecordExporter();
        TrackingLogRecordExporter second = new TrackingLogRecordExporter();
        List<LogRecordExporter> customized = new ArrayList<>();

        Object exporter = new QuarkusCompositeLogRecordExporterProvider(recording(customized))
                .create(node(loader(provider(LogRecordExporter.class, "first", first),
                        provider(LogRecordExporter.class, "second", second)), "first", "second"));

        assertThat(customized).containsExactly(first, second);
        ((LogRecordExporter) exporter).shutdown();
        assertThat(first.shutdown).isTrue();
        assertThat(second.shutdown).isTrue();
    }

    @Test
    void modelExportersAreCustomizedIndividuallyBehindOneBatchProcessor() {
        TrackingSpanExporter first = new TrackingSpanExporter();
        TrackingSpanExporter second = new TrackingSpanExporter();
        List<SpanExporter> customized = new ArrayList<>();
        OpenTelemetryConfigurationModel model = QuarkusOTelModelBuilderTest.buildModel(Map.of(
                "quarkus.otel.traces.exporter", "first,second",
                "quarkus.otel.metrics.exporter", "none",
                "quarkus.otel.logs.exporter", "none"));

        try (OpenTelemetrySdk sdk = QuarkusDeclarativeConfiguration.create(model,
                loader(
                        new QuarkusCompositeSpanExporterProvider(recording(customized)),
                        provider(SpanExporter.class, "first", first),
                        provider(SpanExporter.class, "second", second)),
                InternalTelemetryVersion.LATEST, Resource.getDefault())) {

            assertThat(customized).containsExactly(first, second);
            assertThat(sdk.getSdkTracerProvider().toString()).containsOnlyOnce("BatchSpanProcessor");
        }
    }

    // A quarkus_composite node over the exporter names
    private static DeclarativeConfigProperties node(ComponentLoader loader, String... names) {
        List<Map<String, Object>> exporters = new ArrayList<>();
        for (String name : names) {
            exporters.add(Map.of(name, Map.of()));
        }
        return YamlDeclarativeConfigProperties.create(Map.of("exporters", exporters), loader);
    }

    private static <T> UnaryOperator<T> recording(List<T> received) {
        return exporter -> {
            received.add(exporter);
            return exporter;
        };
    }

    private static ComponentProvider provider(Class<?> type, String name, Object component) {
        return new ComponentProvider() {
            @Override
            public Class<?> getType() {
                return type;
            }

            @Override
            public String getName() {
                return name;
            }

            @Override
            public Object create(DeclarativeConfigProperties config) {
                return component;
            }
        };
    }

    // providers and the drop-all provider, which the module's service file registers
    private static ComponentLoader loader(ComponentProvider... providers) {
        List<ComponentProvider> all = new ArrayList<>(List.of(providers));
        all.add(new QuarkusDropAllSpanProcessorProvider());
        return new ComponentLoader() {
            @SuppressWarnings("unchecked")
            @Override
            public <T> Iterable<T> load(Class<T> type) {
                return type == ComponentProvider.class ? (Iterable<T>) all : List.of();
            }
        };
    }

    private static final class TrackingSpanExporter implements SpanExporter {

        boolean shutdown;

        @Override
        public CompletableResultCode export(Collection<SpanData> spans) {
            return CompletableResultCode.ofSuccess();
        }

        @Override
        public CompletableResultCode flush() {
            return CompletableResultCode.ofSuccess();
        }

        @Override
        public CompletableResultCode shutdown() {
            shutdown = true;
            return CompletableResultCode.ofSuccess();
        }
    }

    private static final class TrackingLogRecordExporter implements LogRecordExporter {

        boolean shutdown;

        @Override
        public CompletableResultCode export(Collection<LogRecordData> logs) {
            return CompletableResultCode.ofSuccess();
        }

        @Override
        public CompletableResultCode flush() {
            return CompletableResultCode.ofSuccess();
        }

        @Override
        public CompletableResultCode shutdown() {
            shutdown = true;
            return CompletableResultCode.ofSuccess();
        }
    }
}
