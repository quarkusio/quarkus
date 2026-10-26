package io.quarkus.opentelemetry.runtime.declarativeconfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.incubator.config.DeclarativeConfigException;
import io.opentelemetry.api.incubator.config.DeclarativeConfigProperties;
import io.opentelemetry.common.ComponentLoader;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.YamlDeclarativeConfigProperties;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.OpenTelemetryConfigurationModel;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.SimpleSpanProcessorModel;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.SpanExporterModel;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.SpanProcessorModel;
import io.opentelemetry.sdk.autoconfigure.spi.internal.ComponentProvider;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.common.InternalTelemetryVersion;
import io.opentelemetry.sdk.common.export.MemoryMode;
import io.opentelemetry.sdk.logs.data.LogRecordData;
import io.opentelemetry.sdk.logs.export.LogRecordExporter;
import io.opentelemetry.sdk.metrics.InstrumentType;
import io.opentelemetry.sdk.metrics.data.AggregationTemporality;
import io.opentelemetry.sdk.metrics.data.MetricData;
import io.opentelemetry.sdk.metrics.export.CollectionRegistration;
import io.opentelemetry.sdk.metrics.export.MetricReader;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import io.quarkus.opentelemetry.runtime.declarativeconfig.spi.QuarkusCompositeLogRecordExporterProvider;
import io.quarkus.opentelemetry.runtime.declarativeconfig.spi.QuarkusCompositeSpanExporterProvider;
import io.quarkus.opentelemetry.runtime.declarativeconfig.spi.QuarkusDevUiMetricReaderProvider;
import io.quarkus.opentelemetry.runtime.declarativeconfig.spi.QuarkusDropAllSpanProcessorProvider;
import io.quarkus.opentelemetry.runtime.declarativeconfig.spi.QuarkusSimpleLogRecordProcessorProvider;
import io.quarkus.opentelemetry.runtime.declarativeconfig.spi.QuarkusSimpleSpanProcessorProvider;

/**
 * The {@code quarkus_simple} processors of {@code quarkus.otel.simple}. As on the autoconfigure path, they are created
 * without the SDK meter provider, so they record no processor self-metrics.
 */
class QuarkusSimpleProcessorProviderTest {

    private static final AttributeKey<String> COMPONENT_TYPE = AttributeKey.stringKey("otel.component.type");
    private static final AttributeKey<String> COMPONENT_NAME = AttributeKey.stringKey("otel.component.name");

    @Test
    void simpleSpanProcessorExportsWithoutSelfMetrics() {
        CapturingSpanExporter exporter = new CapturingSpanExporter();
        CapturingMetricReader metrics = new CapturingMetricReader();
        OpenTelemetryConfigurationModel model = QuarkusOTelModelBuilderTest.modelBuilder(Map.of(
                "quarkus.otel.simple", "true",
                "quarkus.otel.traces.exporter", "test",
                "quarkus.otel.traces.sampler", "always_on",
                "quarkus.otel.metrics.exporter", "none",
                "quarkus.otel.logs.exporter", "none"))
                .devUiMetricReaderPresent(true)
                .build();

        try (OpenTelemetrySdk sdk = QuarkusDeclarativeConfiguration.create(model,
                loader(
                        provider(SpanExporter.class, "test", exporter),
                        new QuarkusDevUiMetricReaderProvider(metrics)),
                InternalTelemetryVersion.LATEST, Resource.getDefault())) {
            sdk.getTracer("test").spanBuilder("span").startSpan().end();

            // exported on the calling thread
            assertThat(exporter.exported).hasSize(1);
            assertThat(componentTypes(metrics)).doesNotContain("simple_span_processor");
        }
    }

    @Test
    void theModelsOwnSimpleSpanProcessorRecordsSelfMetrics() {
        // The control for the test above: upstream's simple processor, which console and logging get, records them
        CapturingSpanExporter exporter = new CapturingSpanExporter();
        CapturingMetricReader metrics = new CapturingMetricReader();
        OpenTelemetryConfigurationModel model = QuarkusOTelModelBuilderTest.modelBuilder(Map.of(
                "quarkus.otel.traces.sampler", "always_on",
                "quarkus.otel.metrics.exporter", "none",
                "quarkus.otel.logs.exporter", "none"))
                .devUiMetricReaderPresent(true)
                .build();
        model.getTracerProvider().setProcessors(List.of(new SpanProcessorModel().setSimple(
                new SimpleSpanProcessorModel().setExporter(new SpanExporterModel().setExtensionProperty("test", Map.of())))));

        try (OpenTelemetrySdk sdk = QuarkusDeclarativeConfiguration.create(model,
                loader(
                        provider(SpanExporter.class, "test", exporter),
                        new QuarkusDevUiMetricReaderProvider(metrics)),
                InternalTelemetryVersion.LATEST, Resource.getDefault())) {
            sdk.getTracer("test").spanBuilder("span").startSpan().end();

            assertThat(componentTypes(metrics)).contains("simple_span_processor");
        }
    }

    @Test
    void simpleLogRecordProcessorUsesTheBeanAloneWithoutSelfMetrics() {
        CapturingLogRecordExporter bean = new CapturingLogRecordExporter();
        CapturingLogRecordExporter logging = new CapturingLogRecordExporter();
        CapturingMetricReader metrics = new CapturingMetricReader();
        OpenTelemetryConfigurationModel model = QuarkusOTelModelBuilderTest.modelBuilder(Map.of(
                "quarkus.otel.simple", "true",
                "quarkus.otel.traces.exporter", "none",
                "quarkus.otel.metrics.exporter", "none",
                // "unresolvable" would fail if the replaced batch processor's exporters were built
                "quarkus.otel.logs.exporter", "unresolvable,logging"))
                .defaultLogRecordExporterPresent(true)
                .devUiMetricReaderPresent(true)
                .build();

        try (OpenTelemetrySdk sdk = QuarkusDeclarativeConfiguration.create(model,
                loader(
                        new QuarkusSimpleLogRecordProcessorProvider(bean),
                        provider(LogRecordExporter.class, "logging", logging),
                        new QuarkusDevUiMetricReaderProvider(metrics)),
                InternalTelemetryVersion.LATEST, Resource.getDefault())) {
            sdk.getLogsBridge().get("test").logRecordBuilder().setBody("record").emit();

            // Both exported on the calling thread; the logging exporter keeps its own simple processor, the only one
            // that records self-metrics
            assertThat(bean.exported).hasSize(1);
            assertThat(logging.exported).hasSize(1);
            assertThat(componentNames(metrics, "simple_log_processor")).hasSize(1);
        }
    }

    @Test
    void simpleSpanProcessorRequiresAnExporter() {
        assertThatThrownBy(() -> new QuarkusSimpleSpanProcessorProvider()
                .create(YamlDeclarativeConfigProperties.create(Map.of(), loader())))
                .isInstanceOf(DeclarativeConfigException.class)
                .hasMessageContaining("exactly one component");
    }

    // The distinct otel.component.name of the metric points the SDK recorded for components of type
    private static Set<String> componentNames(CapturingMetricReader metrics, String type) {
        Set<String> names = new HashSet<>();
        for (MetricData metric : metrics.registration.collectAllMetrics()) {
            metric.getData().getPoints().forEach(point -> {
                if (type.equals(point.getAttributes().get(COMPONENT_TYPE))) {
                    names.add(point.getAttributes().get(COMPONENT_NAME));
                }
            });
        }
        return names;
    }

    // The otel.component.type of every metric point the SDK recorded
    private static List<String> componentTypes(CapturingMetricReader metrics) {
        List<String> types = new ArrayList<>();
        for (MetricData metric : metrics.registration.collectAllMetrics()) {
            metric.getData().getPoints().forEach(point -> {
                String type = point.getAttributes().get(COMPONENT_TYPE);
                if (type != null) {
                    types.add(type);
                }
            });
        }
        return types;
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

    // providers and the ones the module's service file registers
    private static ComponentLoader loader(ComponentProvider... providers) {
        List<ComponentProvider> all = new ArrayList<>(List.of(providers));
        all.add(new QuarkusSimpleSpanProcessorProvider());
        all.add(new QuarkusDropAllSpanProcessorProvider());
        all.add(new QuarkusCompositeSpanExporterProvider());
        all.add(new QuarkusCompositeLogRecordExporterProvider());
        return new ComponentLoader() {
            @SuppressWarnings("unchecked")
            @Override
            public <T> Iterable<T> load(Class<T> type) {
                return type == ComponentProvider.class ? (Iterable<T>) all : List.of();
            }
        };
    }

    private static final class CapturingSpanExporter implements SpanExporter {

        final List<SpanData> exported = new ArrayList<>();

        @Override
        public CompletableResultCode export(Collection<SpanData> spans) {
            exported.addAll(spans);
            return CompletableResultCode.ofSuccess();
        }

        @Override
        public CompletableResultCode flush() {
            return CompletableResultCode.ofSuccess();
        }

        @Override
        public CompletableResultCode shutdown() {
            return CompletableResultCode.ofSuccess();
        }
    }

    private static final class CapturingLogRecordExporter implements LogRecordExporter {

        final List<LogRecordData> exported = new ArrayList<>();

        @Override
        public CompletableResultCode export(Collection<LogRecordData> logs) {
            exported.addAll(logs);
            return CompletableResultCode.ofSuccess();
        }

        @Override
        public CompletableResultCode flush() {
            return CompletableResultCode.ofSuccess();
        }

        @Override
        public CompletableResultCode shutdown() {
            return CompletableResultCode.ofSuccess();
        }
    }

    private static final class CapturingMetricReader implements MetricReader {

        CollectionRegistration registration;

        @Override
        public void register(CollectionRegistration registration) {
            this.registration = registration;
        }

        @Override
        public CompletableResultCode forceFlush() {
            return CompletableResultCode.ofSuccess();
        }

        @Override
        public CompletableResultCode shutdown() {
            return CompletableResultCode.ofSuccess();
        }

        @Override
        public AggregationTemporality getAggregationTemporality(InstrumentType instrumentType) {
            return AggregationTemporality.CUMULATIVE;
        }

        @Override
        public MemoryMode getMemoryMode() {
            return MemoryMode.IMMUTABLE_DATA;
        }
    }
}
