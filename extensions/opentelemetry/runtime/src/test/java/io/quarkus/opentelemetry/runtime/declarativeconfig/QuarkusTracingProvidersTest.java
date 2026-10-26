package io.quarkus.opentelemetry.runtime.declarativeconfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.UnaryOperator;

import org.junit.jupiter.api.Test;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.incubator.config.DeclarativeConfigException;
import io.opentelemetry.api.incubator.config.DeclarativeConfigProperties;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.common.ComponentLoader;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.YamlDeclarativeConfigProperties;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.OpenTelemetryConfigurationModel;
import io.opentelemetry.sdk.autoconfigure.spi.internal.ComponentProvider;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.common.InternalTelemetryVersion;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.IdGenerator;
import io.opentelemetry.sdk.trace.ReadWriteSpan;
import io.opentelemetry.sdk.trace.ReadableSpan;
import io.opentelemetry.sdk.trace.SpanProcessor;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import io.opentelemetry.sdk.trace.samplers.SamplingDecision;
import io.quarkus.opentelemetry.runtime.declarativeconfig.spi.QuarkusCdiIdGeneratorProvider;
import io.quarkus.opentelemetry.runtime.declarativeconfig.spi.QuarkusCdiSamplerProvider;
import io.quarkus.opentelemetry.runtime.declarativeconfig.spi.QuarkusCdiSpanProcessorProvider;
import io.quarkus.opentelemetry.runtime.declarativeconfig.spi.QuarkusCompositeSpanExporterProvider;
import io.quarkus.opentelemetry.runtime.declarativeconfig.spi.QuarkusCustomizedSamplerProvider;
import io.quarkus.opentelemetry.runtime.declarativeconfig.spi.QuarkusDropAllSpanProcessorProvider;
import io.quarkus.opentelemetry.runtime.declarativeconfig.spi.QuarkusDropTargetsSamplerProvider;
import io.quarkus.opentelemetry.runtime.tracing.DropAllSpanProcessor;
import io.quarkus.opentelemetry.runtime.tracing.DropTargetsSampler;

/**
 * The Quarkus tracing {@link ComponentProvider}s, alone and resolved from the nodes {@link QuarkusOTelModelBuilder}
 * emits.
 */
class QuarkusTracingProvidersTest {

    private static final String EXPORTER = "test_exporter";

    private static final Map<String, String> NO_EXPORTERS = Map.of(
            "quarkus.otel.traces.exporter", "none",
            "quarkus.otel.metrics.exporter", "none",
            "quarkus.otel.logs.exporter", "none",
            "quarkus.otel.traces.sampler", "always_off");

    @Test
    void cdiSamplerReplacesTheConfiguredSamplerAndKeepsTheDropTargets() {
        OpenTelemetryConfigurationModel model = QuarkusOTelModelBuilderTest.modelBuilder(NO_EXPORTERS)
                .nonApplicationUriTargets(Set.of("/q/health"))
                .cdiSamplerPresent(true)
                .build();

        Sampler sampler = sdkSampler(model,
                new QuarkusCdiSamplerProvider(Sampler.alwaysOn()), new QuarkusDropTargetsSamplerProvider());

        // always_off from the configuration was replaced by the bean, still wrapped for the drop targets
        assertThat(decision(sampler, "/hello")).isEqualTo(SamplingDecision.RECORD_AND_SAMPLE);
        assertThat(decision(sampler, "/q/health")).isEqualTo(SamplingDecision.DROP);
    }

    @Test
    void cdiSamplerReplacesTheConfiguredSamplerWithoutDropTargets() {
        OpenTelemetryConfigurationModel model = QuarkusOTelModelBuilderTest.modelBuilder(NO_EXPORTERS)
                .cdiSamplerPresent(true)
                .build();

        assertThat(sdkSampler(model, new QuarkusCdiSamplerProvider(Sampler.alwaysOn()))).isSameAs(Sampler.alwaysOn());
    }

    @Test
    void samplerCustomizerRunsOverTheDropTargetsSampler() {
        OpenTelemetryConfigurationModel model = QuarkusOTelModelBuilderTest.modelBuilder(NO_EXPORTERS)
                .nonApplicationUriTargets(Set.of("/q/health"))
                .cdiSamplerPresent(true)
                .samplerCustomized(true)
                .build();
        List<Sampler> customized = new ArrayList<>();
        Sampler replacement = Sampler.traceIdRatioBased(0.5d);

        Sampler sampler = sdkSampler(model,
                new QuarkusCdiSamplerProvider(Sampler.alwaysOn()),
                new QuarkusDropTargetsSamplerProvider(),
                new QuarkusCustomizedSamplerProvider(recording(customized, replacement)));

        assertThat(sampler).isSameAs(replacement);
        assertThat(customized).singleElement().isInstanceOf(DropTargetsSampler.class);
    }

    @Test
    void samplerCustomizerRunsOverTheDisabledTracesSampler() {
        Map<String, String> properties = new HashMap<>(NO_EXPORTERS);
        properties.put("quarkus.otel.traces.enabled", "false");
        OpenTelemetryConfigurationModel model = QuarkusOTelModelBuilderTest.modelBuilder(properties)
                .samplerCustomized(true)
                .build();
        List<Sampler> customized = new ArrayList<>();

        Sampler sampler = sdkSampler(model,
                new QuarkusCustomizedSamplerProvider(recording(customized, Sampler.alwaysOn())));

        assertThat(sampler).isSameAs(Sampler.alwaysOn());
        assertThat(customized).containsExactly(Sampler.alwaysOff());
    }

    @Test
    void customizedSamplerRequiresADelegate() {
        QuarkusCustomizedSamplerProvider provider = new QuarkusCustomizedSamplerProvider(UnaryOperator.identity());

        assertThatThrownBy(() -> provider.create(YamlDeclarativeConfigProperties.create(Map.of(), loader())))
                .isInstanceOf(DeclarativeConfigException.class)
                .hasMessageContaining("delegate");
    }

    @Test
    void cdiIdGeneratorGeneratesTheIds() {
        OpenTelemetryConfigurationModel model = QuarkusOTelModelBuilderTest.modelBuilder(exporting())
                .cdiIdGeneratorPresent(true)
                .build();
        IdGenerator fixed = new IdGenerator() {
            @Override
            public String generateSpanId() {
                return "0000000000000002";
            }

            @Override
            public String generateTraceId() {
                return "00000000000000000000000000000003";
            }
        };

        try (OpenTelemetrySdk sdk = QuarkusDeclarativeConfiguration.create(model,

                loader(new QuarkusCdiIdGeneratorProvider(fixed), spanExporterProvider()), InternalTelemetryVersion.LATEST,
                Resource.getDefault())) {
            SpanContext span = sdk.getTracer("test").spanBuilder("span").startSpan().getSpanContext();

            assertThat(span.getTraceId()).isEqualTo("00000000000000000000000000000003");
            assertThat(span.getSpanId()).isEqualTo("0000000000000002");
        }
    }

    @Test
    void cdiSpanProcessorsAreCreatedByTheProviderInIterationOrder() {
        List<String> calls = new ArrayList<>();
        int[] iterations = { 0 };
        Iterable<SpanProcessor> beans = () -> {
            iterations[0]++;
            return List.of(recording("first", calls), recording("second", calls)).iterator();
        };

        QuarkusCdiSpanProcessorProvider provider = new QuarkusCdiSpanProcessorProvider(beans);
        assertThat(iterations[0]).as("the beans are not looked up before the SDK is built").isZero();

        SpanProcessor processor = (SpanProcessor) provider.create(DeclarativeConfigProperties.empty());
        processor.onStart(Context.root(), null);

        assertThat(iterations[0]).isOne();
        assertThat(calls).containsExactly("first", "second");
    }

    @Test
    void cdiSpanProcessorsReceiveTheSpansWhenNothingExports() {
        Map<String, String> properties = new HashMap<>(NO_EXPORTERS);
        properties.put("quarkus.otel.traces.sampler", "always_on");
        OpenTelemetryConfigurationModel model = QuarkusOTelModelBuilderTest.modelBuilder(properties)
                .cdiSpanProcessorsPresent(true)
                .build();
        List<String> calls = new ArrayList<>();

        try (OpenTelemetrySdk sdk = QuarkusDeclarativeConfiguration.create(model,

                loader(new QuarkusCdiSpanProcessorProvider(List.of(recording("bean", calls)))), InternalTelemetryVersion.LATEST,
                Resource.getDefault())) {
            sdk.getTracer("test").spanBuilder("span").startSpan().end();
        }

        assertThat(calls).containsExactly("bean");
    }

    @Test
    void dropAllKeepsTheTracersRealWhenNothingExports() {
        Map<String, String> properties = new HashMap<>(NO_EXPORTERS);
        properties.put("quarkus.otel.traces.sampler", "always_on");
        OpenTelemetryConfigurationModel model = QuarkusOTelModelBuilderTest.modelBuilder(properties).build();

        try (OpenTelemetrySdk sdk = QuarkusDeclarativeConfiguration.create(model,
                loader(), InternalTelemetryVersion.LATEST, Resource.getDefault())) {
            // spans get valid ids for log correlation and context propagation, but nothing is exported
            assertThat(sdk.getTracer("test").spanBuilder("span").startSpan().getSpanContext().isValid()).isTrue();
        }
    }

    @Test
    void dropAllProviderCreatesADropAllSpanProcessor() {
        assertThat(new QuarkusDropAllSpanProcessorProvider().create(DeclarativeConfigProperties.empty()))
                .isInstanceOf(DropAllSpanProcessor.class);
    }

    // A processor that records name when a span starts
    private static SpanProcessor recording(String name, List<String> calls) {
        return new SpanProcessor() {
            @Override
            public void onStart(Context parentContext, ReadWriteSpan span) {
                calls.add(name);
            }

            @Override
            public boolean isStartRequired() {
                return true;
            }

            @Override
            public void onEnd(ReadableSpan span) {
            }

            @Override
            public boolean isEndRequired() {
                return false;
            }
        };
    }

    // Traces exported to the component spanExporterProvider() supplies, every span sampled
    private static Map<String, String> exporting() {
        Map<String, String> properties = new HashMap<>(NO_EXPORTERS);
        properties.put("quarkus.otel.traces.exporter", EXPORTER);
        properties.put("quarkus.otel.traces.sampler", "always_on");
        return properties;
    }

    private static ComponentProvider spanExporterProvider() {
        return new ComponentProvider() {
            @Override
            public Class<?> getType() {
                return SpanExporter.class;
            }

            @Override
            public String getName() {
                return EXPORTER;
            }

            @Override
            public Object create(DeclarativeConfigProperties config) {
                return new SpanExporter() {
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
                        return CompletableResultCode.ofSuccess();
                    }
                };
            }
        };
    }

    // The sampler of the SDK built from model, with providers as the only components
    private static Sampler sdkSampler(OpenTelemetryConfigurationModel model, ComponentProvider... providers) {
        try (OpenTelemetrySdk sdk = QuarkusDeclarativeConfiguration.create(model,
                loader(providers), InternalTelemetryVersion.LATEST, Resource.getDefault())) {
            return sdk.getSdkTracerProvider().getSampler();
        }
    }

    private static SamplingDecision decision(Sampler sampler, String path) {
        return sampler.shouldSample(Context.root(), "00000000000000000000000000000001", "span", SpanKind.SERVER,
                Attributes.of(AttributeKey.stringKey("url.path"), path), List.of()).getDecision();
    }

    private static UnaryOperator<Sampler> recording(List<Sampler> received, Sampler result) {
        return sampler -> {
            received.add(sampler);
            return result;
        };
    }

    // providers and the drop-all and composite exporter providers, which the module's service file registers
    private static ComponentLoader loader(ComponentProvider... providers) {
        List<ComponentProvider> all = new ArrayList<>(List.of(providers));
        all.add(new QuarkusDropAllSpanProcessorProvider());
        all.add(new QuarkusCompositeSpanExporterProvider());
        return new ComponentLoader() {
            @SuppressWarnings("unchecked")
            @Override
            public <T> Iterable<T> load(Class<T> type) {
                return type == ComponentProvider.class ? (Iterable<T>) all : List.of();
            }
        };
    }
}
