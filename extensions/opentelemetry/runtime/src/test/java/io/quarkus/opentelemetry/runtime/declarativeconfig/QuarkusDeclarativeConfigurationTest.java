package io.quarkus.opentelemetry.runtime.declarativeconfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.UnaryOperator;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import org.junit.jupiter.api.Test;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.Value;
import io.opentelemetry.api.incubator.config.DeclarativeConfigException;
import io.opentelemetry.api.incubator.config.DeclarativeConfigProperties;
import io.opentelemetry.common.ComponentLoader;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.YamlDeclarativeConfigProperties;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.AlwaysOffSamplerModel;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.AlwaysOnSamplerModel;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.CardinalityLimitsModel;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.MeterProviderModel;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.MetricReaderModel;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.OpenTelemetryConfigurationModel;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.ParentBasedSamplerModel;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.PullMetricExporterModel;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.PullMetricReaderModel;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.SamplerModel;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.TraceIdRatioBasedSamplerModel;
import io.opentelemetry.sdk.autoconfigure.spi.internal.ComponentProvider;
import io.opentelemetry.sdk.autoconfigure.spi.internal.DefaultConfigProperties;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.common.InternalTelemetryVersion;
import io.opentelemetry.sdk.metrics.InstrumentType;
import io.opentelemetry.sdk.metrics.data.AggregationTemporality;
import io.opentelemetry.sdk.metrics.data.MetricData;
import io.opentelemetry.sdk.metrics.export.CollectionRegistration;
import io.opentelemetry.sdk.metrics.export.MetricExporter;
import io.opentelemetry.sdk.metrics.export.MetricReader;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.ReadableSpan;
import io.opentelemetry.sdk.trace.SpanLimits;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import io.quarkus.opentelemetry.runtime.declarativeconfig.spi.QuarkusCompositeLogRecordExporterProvider;
import io.quarkus.opentelemetry.runtime.declarativeconfig.spi.QuarkusCompositeSpanExporterProvider;
import io.quarkus.opentelemetry.runtime.declarativeconfig.spi.QuarkusDropAllSpanProcessorProvider;
import io.quarkus.opentelemetry.runtime.declarativeconfig.spi.QuarkusDropTargetsSamplerProvider;
import io.quarkus.opentelemetry.runtime.declarativeconfig.spi.QuarkusPeriodicMetricReaderProvider;
import io.quarkus.opentelemetry.runtime.tracing.DropTargetsSampler;

class QuarkusDeclarativeConfigurationTest {

    private static final ComponentLoader NO_COMPONENTS = new StubComponentLoader(Map.of());

    @Test
    void disabledModelIsNotInterpreted() {
        // an exporter that could never resolve: proof that a disabled model short-circuits before any
        // component resolution, mirroring upstream's empty-SDK behavior
        OpenTelemetryConfigurationModel model = QuarkusOTelModelBuilderTest.buildModel(Map.of(
                "quarkus.otel.sdk.disabled", "true",
                "quarkus.otel.traces.exporter", "unresolvable-exporter"));

        try (OpenTelemetrySdk sdk = QuarkusDeclarativeConfiguration.create(model,
                NO_COMPONENTS, InternalTelemetryVersion.LATEST, Resource.getDefault())) {

            assertThat(sdk).isNotNull();
            // As upstream, the disabled SDK has no propagators; QuarkusDeclarativeSdk builds them itself
            assertThat(sdk.getPropagators().getTextMapPropagator().fields()).isEmpty();
        }
    }

    @Test
    void typedSamplersAreBuiltDirectly() {
        assertThat(sampler(new SamplerModel().setAlwaysOn(new AlwaysOnSamplerModel())))
                .isEqualTo(Sampler.alwaysOn());
        assertThat(sampler(new SamplerModel().setAlwaysOff(new AlwaysOffSamplerModel())))
                .isEqualTo(Sampler.alwaysOff());
        assertThat(sampler(new SamplerModel()
                .setTraceIdRatioBased(new TraceIdRatioBasedSamplerModel().setRatio(0.25d))))
                .isEqualTo(Sampler.traceIdRatioBased(0.25d));
        assertThat(sampler(new SamplerModel().setParentBased(new ParentBasedSamplerModel()
                .setRoot(new SamplerModel().setAlwaysOff(new AlwaysOffSamplerModel())))))
                .isEqualTo(Sampler.parentBased(Sampler.alwaysOff()));
    }

    @Test
    void customSamplerResolvesViaComponentProvider() {
        Sampler custom = Sampler.traceIdRatioBased(0.75d);
        ComponentLoader loader = new StubComponentLoader(Map.of(
                ComponentProvider.class, List.of(componentProvider(Sampler.class, "my_sampler", custom))));

        SamplerModel model = new SamplerModel().setExtensionProperty("my_sampler", Map.of());
        assertThat(QuarkusDeclarativeConfiguration.createSampler(model, loader)).isSameAs(custom);
    }

    @Test
    void unknownSamplerNameFails() {
        SamplerModel model = new SamplerModel().setExtensionProperty("missing", Map.of());
        assertThatThrownBy(() -> QuarkusDeclarativeConfiguration.createSampler(model, NO_COMPONENTS))
                .isInstanceOf(DeclarativeConfigException.class)
                .hasMessageContaining("missing");
    }

    @Test
    void samplerIsBuiltFromDeclarativeConfigProperties() {
        assertThat(QuarkusDeclarativeConfiguration.createSampler(samplerNode(Map.of("always_on", Map.of()))))
                .isEqualTo(Sampler.alwaysOn());
        assertThat(QuarkusDeclarativeConfiguration.createSampler(
                samplerNode(Map.of("trace_id_ratio_based", Map.of("ratio", 0.25d)))))
                .isEqualTo(Sampler.traceIdRatioBased(0.25d));
        assertThat(QuarkusDeclarativeConfiguration.createSampler(
                samplerNode(Map.of("parent_based", Map.of("root", Map.of("always_off", Map.of()))))))
                .isEqualTo(Sampler.parentBased(Sampler.alwaysOff()));
    }

    @Test
    void dropTargetsSamplerProviderWrapsDelegate() {
        DeclarativeConfigProperties node = samplerNode(Map.of(
                "delegate", Map.of("always_on", Map.of()),
                "drop_targets", List.of("/q/health", "/q/metrics")));

        Object sampler = new QuarkusDropTargetsSamplerProvider().create(node);

        assertThat(sampler).isInstanceOf(DropTargetsSampler.class);
    }

    @Test
    void emptySamplerNodeKeepsTheComponentLoader() {
        List<ComponentLoader> seen = new ArrayList<>();
        ComponentLoader loader = new StubComponentLoader(Map.of(ComponentProvider.class, List.of(new ComponentProvider() {
            @Override
            public Class<?> getType() {
                return Sampler.class;
            }

            @Override
            public String getName() {
                return "my_sampler";
            }

            @Override
            public Object create(DeclarativeConfigProperties config) {
                seen.add(config.getComponentLoader());
                return Sampler.alwaysOn();
            }
        })));
        Map<String, Object> properties = new HashMap<>();
        properties.put("my_sampler", null);

        QuarkusDeclarativeConfiguration.createSampler(YamlDeclarativeConfigProperties.create(properties, loader));

        assertThat(seen).containsExactly(loader);
    }

    @Test
    void extensionSamplerConfigIsReadWithoutTypeMismatchWarnings() {
        ComponentLoader loader = new StubComponentLoader(Map.of(
                ComponentProvider.class, List.of(new QuarkusDropTargetsSamplerProvider())));
        DeclarativeConfigProperties node = YamlDeclarativeConfigProperties.create(Map.of(
                QuarkusOTelModelBuilder.QUARKUS_DROP_TARGETS, Map.of(
                        "delegate", Map.of("trace_id_ratio_based", Map.of("ratio", 1.0d)),
                        "drop_targets", List.of("/q/health"))),
                loader);
        List<LogRecord> warnings = new ArrayList<>();
        Handler handler = new Handler() {
            @Override
            public void publish(LogRecord record) {
                if (record.getLevel().intValue() >= Level.WARNING.intValue()) {
                    warnings.add(record);
                }
            }

            @Override
            public void flush() {
            }

            @Override
            public void close() {
            }
        };
        Logger logger = Logger.getLogger(YamlDeclarativeConfigProperties.class.getName());
        logger.addHandler(handler);
        try {
            assertThat(QuarkusDeclarativeConfiguration.createSampler(node)).isInstanceOf(DropTargetsSampler.class);
        } finally {
            logger.removeHandler(handler);
        }

        assertThat(warnings).extracting(LogRecord::getMessage).isEmpty();
    }

    /**
     * Port of the former {@code DeclarativeConfiguration.create()} round-trip test: a model with all exporters set
     * to {@code none} builds a real SDK with working (but non-exporting) providers.
     */
    @Test
    void noneModelBuildsARealSdk() {
        OpenTelemetryConfigurationModel model = QuarkusOTelModelBuilderTest.buildModel(Map.of(
                "quarkus.otel.traces.exporter", "none",
                "quarkus.otel.metrics.exporter", "none",
                "quarkus.otel.logs.exporter", "none",
                // the default sampler is parentbased_traceidratio with a 0.1 ratio: sampling (and so the recording
                // assertion below) would be a coin flip
                "quarkus.otel.traces.sampler", "always_on"));

        try (OpenTelemetrySdk sdk = QuarkusDeclarativeConfiguration.create(model,
                NO_COMPONENTS, InternalTelemetryVersion.LATEST, Resource.getDefault())) {

            assertThat(sdk.getSdkTracerProvider()).isNotNull();
            assertThat(sdk.getTracer("test").spanBuilder("span").startSpan().isRecording()).isTrue();
            assertThat(sdk.getPropagators().getTextMapPropagator().fields()).contains("traceparent", "baggage");
        }
    }

    @Test
    void dropTargetsSamplerModelBuildsARealSdk() {
        OpenTelemetryConfigurationModel model = QuarkusOTelModelBuilderTest.buildModel(Map.of(
                "quarkus.otel.traces.exporter", "none",
                "quarkus.otel.metrics.exporter", "none",
                "quarkus.otel.logs.exporter", "none",
                "quarkus.otel.traces.sampler", "always_on"),
                Set.of("/q/health"), Set.of());

        ComponentLoader loader = new StubComponentLoader(Map.of(
                ComponentProvider.class, List.of(new QuarkusDropTargetsSamplerProvider())));
        Sampler sampler = QuarkusDeclarativeConfiguration.createSampler(model.getTracerProvider().getSampler(), loader);
        assertThat(sampler).isInstanceOf(DropTargetsSampler.class);

        try (OpenTelemetrySdk sdk = QuarkusDeclarativeConfiguration.create(model,
                loader, InternalTelemetryVersion.LATEST, Resource.getDefault())) {
            assertThat(sdk.getSdkTracerProvider()).isNotNull();
        }
    }

    @SuppressWarnings("resource")
    @Test
    void multiKeyExtensionSamplerNodeFails() {
        SamplerModel model = new SamplerModel()
                .setExtensionProperty("first", Map.of())
                .setExtensionProperty("second", Map.of());
        assertThatThrownBy(() -> QuarkusDeclarativeConfiguration.createSampler(model, NO_COMPONENTS))
                .isInstanceOf(DeclarativeConfigException.class)
                .hasMessageContaining("exactly one");

        assertThatThrownBy(() -> QuarkusDeclarativeConfiguration.createSampler(
                samplerNode(Map.of("first", Map.of(), "second", Map.of()))))
                .isInstanceOf(DeclarativeConfigException.class)
                .hasMessageContaining("exactly one");
    }

    @Test
    void customExporterNameResolvesViaComponentProvider() {
        AtomicBoolean created = new AtomicBoolean();
        ComponentLoader loader = new StubComponentLoader(Map.of(
                ComponentProvider.class, List.of(componentProvider(SpanExporter.class, "my-exporter",
                        exporter(created)))));

        OpenTelemetryConfigurationModel model = QuarkusOTelModelBuilderTest.buildModel(Map.of(
                "quarkus.otel.traces.exporter", "my-exporter",
                "quarkus.otel.metrics.exporter", "none",
                "quarkus.otel.logs.exporter", "none"));

        try (OpenTelemetrySdk sdk = QuarkusDeclarativeConfiguration.create(model,
                loader, InternalTelemetryVersion.LATEST, Resource.getDefault())) {
            assertThat(created).isTrue();
        }
    }

    @Test
    void generalAttributeLimitsApplyWhenSpanLimitIsUnset() {
        OpenTelemetryConfigurationModel model = QuarkusOTelModelBuilderTest.buildModel(Map.of(
                "quarkus.otel.attribute.value.length.limit", "4",
                "quarkus.otel.traces.exporter", "none",
                "quarkus.otel.metrics.exporter", "none",
                "quarkus.otel.logs.exporter", "none"));

        try (OpenTelemetrySdk sdk = QuarkusDeclarativeConfiguration.create(model,
                NO_COMPONENTS, InternalTelemetryVersion.LATEST, Resource.getDefault())) {

            assertThat(sdk.getSdkTracerProvider().getSpanLimits().getMaxAttributeValueLength()).isEqualTo(4);
        }
    }

    @Test
    void spanAttributeLimitShadowsGeneralLimit() {
        OpenTelemetryConfigurationModel model = QuarkusOTelModelBuilderTest.buildModel(Map.of(
                "quarkus.otel.attribute.value.length.limit", "4",
                "quarkus.otel.span.attribute.value.length.limit", "8",
                "quarkus.otel.traces.exporter", "none",
                "quarkus.otel.metrics.exporter", "none",
                "quarkus.otel.logs.exporter", "none"));

        try (OpenTelemetrySdk sdk = QuarkusDeclarativeConfiguration.create(model,
                NO_COMPONENTS, InternalTelemetryVersion.LATEST, Resource.getDefault())) {

            assertThat(sdk.getSdkTracerProvider().getSpanLimits().getMaxAttributeValueLength()).isEqualTo(8);
        }
    }

    @Test
    void mergedResourceIsUsedAsIs() {
        // The model cannot carry VALUE attributes; the recorder hands the SDK the merged Resource itself
        Resource merged = Resource.create(
                Attributes.of(AttributeKey.valueKey("structured"), Value.of(new byte[] { 1 })),
                "https://opentelemetry.io/schemas/1.40.0");
        OpenTelemetryConfigurationModel model = QuarkusOTelModelBuilderTest.buildModel(Map.of(
                "quarkus.otel.traces.exporter", "none",
                "quarkus.otel.metrics.exporter", "none",
                "quarkus.otel.logs.exporter", "none",
                "quarkus.otel.traces.sampler", "always_on"));

        try (OpenTelemetrySdk sdk = QuarkusDeclarativeConfiguration.create(model,
                NO_COMPONENTS, InternalTelemetryVersion.LATEST, merged)) {

            ReadableSpan span = (ReadableSpan) sdk.getTracer("test").spanBuilder("span").startSpan();
            assertThat(span.toSpanData().getResource()).isEqualTo(merged);
        }
    }

    @Test
    void generalAttributeCountLimitReachesEventAndLinkLimits() {
        OpenTelemetryConfigurationModel model = QuarkusOTelModelBuilderTest.buildModel(Map.of(
                "quarkus.otel.traces.exporter", "none",
                "quarkus.otel.metrics.exporter", "none",
                "quarkus.otel.logs.exporter", "none",
                "quarkus.otel.attribute.count.limit", "64",
                "quarkus.otel.span.attribute.count.limit", "32"));

        try (OpenTelemetrySdk sdk = QuarkusDeclarativeConfiguration.create(model,
                NO_COMPONENTS, InternalTelemetryVersion.LATEST, Resource.getDefault())) {

            SpanLimits limits = sdk.getSdkTracerProvider().getSpanLimits();
            assertThat(limits.getMaxNumberOfAttributes()).isEqualTo(32);
            assertThat(limits.getMaxNumberOfAttributesPerEvent()).isEqualTo(64);
            assertThat(limits.getMaxNumberOfAttributesPerLink()).isEqualTo(64);
        }
    }

    @Test
    void exemplarFilterAndCardinalityLimitBuildARealSdk() {
        // The SDK exposes neither value; this proves the interpreter applies them without failing
        // the quarkus_periodic reader node resolves through its Quarkus provider
        ComponentLoader loader = new QuarkusComponentLoader(new StubComponentLoader(Map.of(ComponentProvider.class,
                List.of(componentProvider(MetricExporter.class, "my-exporter", new CumulativeMetricExporter())))),
                DefaultConfigProperties.createFromMap(Map.of()),
                List.of(new QuarkusPeriodicMetricReaderProvider(null, UnaryOperator.identity())));
        OpenTelemetryConfigurationModel model = QuarkusOTelModelBuilderTest.buildModel(Map.of(
                "quarkus.otel.traces.exporter", "none",
                "quarkus.otel.logs.exporter", "none",
                "quarkus.otel.metrics.exporter", "my-exporter",
                "otel.metrics.exemplar.filter", "always_off",
                "otel.java.metrics.cardinality.limit", "10"));

        try (OpenTelemetrySdk sdk = QuarkusDeclarativeConfiguration.create(model,
                loader, InternalTelemetryVersion.LATEST, Resource.getDefault())) {

            assertThat(sdk.getSdkMeterProvider()).isNotNull();
        }
    }

    @Test
    void pullReaderResolvesAMetricReaderComponentWithItsCardinalityLimit() {
        // As upstream's PullMetricReaderFactory: the exporter node names a MetricReader component
        RegistrationCapturingMetricReader reader = new RegistrationCapturingMetricReader();
        ComponentLoader loader = new StubComponentLoader(Map.of(ComponentProvider.class,
                List.of(componentProvider(MetricReader.class, "my-reader", reader))));
        OpenTelemetryConfigurationModel model = QuarkusOTelModelBuilderTest.buildModel(Map.of(
                "quarkus.otel.traces.exporter", "none",
                "quarkus.otel.logs.exporter", "none",
                "quarkus.otel.metrics.exporter", "none"));
        model.setMeterProvider(new MeterProviderModel().setReaders(List.of(new MetricReaderModel()
                .setPull(new PullMetricReaderModel()
                        .setExporter(new PullMetricExporterModel().setExtensionProperty("my-reader", Map.of()))
                        .setCardinalityLimits(new CardinalityLimitsModel().setDefault(1))))));

        try (OpenTelemetrySdk sdk = QuarkusDeclarativeConfiguration.create(model,
                loader, InternalTelemetryVersion.LATEST, Resource.getDefault())) {
            sdk.getMeter("test").counterBuilder("test.counter").build().add(1);
            sdk.getMeter("test").counterBuilder("test.counter").build().add(1, Attributes.of(
                    AttributeKey.stringKey("key"), "value"));

            assertThat(reader.registration).isNotNull();
            // a limit of 1 leaves room for the overflow point only
            assertThat(reader.registration.collectAllMetrics()).singleElement()
                    .satisfies(metric -> assertThat(metric.getLongSumData().getPoints()).hasSize(1));
        }
    }

    @Test
    void unknownPullReaderNameFails() {
        OpenTelemetryConfigurationModel model = QuarkusOTelModelBuilderTest.buildModel(Map.of(
                "quarkus.otel.traces.exporter", "none",
                "quarkus.otel.logs.exporter", "none",
                "quarkus.otel.metrics.exporter", "none"));
        model.setMeterProvider(new MeterProviderModel().setReaders(List.of(new MetricReaderModel()
                .setPull(new PullMetricReaderModel()
                        .setExporter(new PullMetricExporterModel().setExtensionProperty("missing", Map.of()))))));

        assertThatThrownBy(() -> QuarkusDeclarativeConfiguration.create(model,
                NO_COMPONENTS, InternalTelemetryVersion.LATEST, Resource.getDefault()))
                .isInstanceOf(DeclarativeConfigException.class)
                .hasMessageContaining("No MetricReader named 'missing'");
    }

    private static Sampler sampler(SamplerModel model) {
        return QuarkusDeclarativeConfiguration.createSampler(model, NO_COMPONENTS);
    }

    @SuppressWarnings("resource")
    @Test
    void componentsBuiltBeforeAFailureAreClosed() {
        ShutdownTrackingSpanExporter tracked = new ShutdownTrackingSpanExporter();
        ComponentLoader loader = new StubComponentLoader(Map.of(
                ComponentProvider.class, List.of(componentProvider(SpanExporter.class, "tracked", tracked))));
        OpenTelemetryConfigurationModel model = QuarkusOTelModelBuilderTest.buildModel(Map.of(
                "quarkus.otel.traces.exporter", "tracked,unresolvable",
                "quarkus.otel.metrics.exporter", "none",
                "quarkus.otel.logs.exporter", "none"));

        assertThatThrownBy(() -> QuarkusDeclarativeConfiguration.create(model,
                loader, InternalTelemetryVersion.LATEST, Resource.getDefault()))
                .isInstanceOf(DeclarativeConfigException.class)
                .hasMessageContaining("unresolvable");

        assertThat(tracked.shutdownCalled).as("components built before the failure are closed").isTrue();
    }

    private static DeclarativeConfigProperties samplerNode(Map<String, Object> properties) {
        return YamlDeclarativeConfigProperties.create(properties, NO_COMPONENTS);
    }

    private static ComponentProvider componentProvider(Class<?> type, String name, Object component) {
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

    private static SpanExporter exporter(AtomicBoolean created) {
        created.set(true);
        return SpanExporter.composite();
    }

    private static final class ShutdownTrackingSpanExporter implements SpanExporter {

        volatile boolean shutdownCalled;

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
            shutdownCalled = true;
            return CompletableResultCode.ofSuccess();
        }
    }

    private static final class CumulativeMetricExporter implements MetricExporter {

        @Override
        public CompletableResultCode export(Collection<MetricData> metrics) {
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

        @Override
        public AggregationTemporality getAggregationTemporality(InstrumentType instrumentType) {
            return AggregationTemporality.CUMULATIVE;
        }
    }

    private static final class RegistrationCapturingMetricReader implements MetricReader {

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
    }

    // Loads the given services. The ComponentProvider services also include the providers the module's service file
    // registers for nodes the model always carries: the drop-all processor whenever traces are on and nothing
    // exports, and the composite exporters, which do not customize the exporters here.
    private record StubComponentLoader(Map<Class<?>, List<Object>> services) implements ComponentLoader {

        @SuppressWarnings("unchecked")
        @Override
        public <T> Iterable<T> load(Class<T> type) {
            List<Object> loaded = new ArrayList<>(services.getOrDefault(type, List.of()));
            if (type == ComponentProvider.class) {
                loaded.add(new QuarkusDropAllSpanProcessorProvider());
                loaded.add(new QuarkusCompositeSpanExporterProvider());
                loaded.add(new QuarkusCompositeLogRecordExporterProvider());
            }
            return (Iterable<T>) loaded;
        }
    }
}
