package io.quarkus.opentelemetry.runtime.declarativeconfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.incubator.config.DeclarativeConfigException;
import io.opentelemetry.api.incubator.config.DeclarativeConfigProperties;
import io.opentelemetry.common.ComponentLoader;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.YamlDeclarativeConfigProperties;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.AlwaysOffSamplerModel;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.AlwaysOnSamplerModel;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.OpenTelemetryConfigurationModel;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.ParentBasedSamplerModel;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.SamplerModel;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.SamplerPropertyModel;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.TraceIdRatioBasedSamplerModel;
import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizer;
import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizerProvider;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.ResourceProvider;
import io.opentelemetry.sdk.autoconfigure.spi.internal.ComponentProvider;
import io.opentelemetry.sdk.autoconfigure.spi.internal.ConditionalResourceProvider;
import io.opentelemetry.sdk.autoconfigure.spi.internal.DefaultConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.traces.ConfigurableSamplerProvider;
import io.opentelemetry.sdk.autoconfigure.spi.traces.ConfigurableSpanExporterProvider;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SpanProcessor;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import io.quarkus.opentelemetry.runtime.declarativeconfig.spi.QuarkusDropTargetsSamplerProvider;
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
                QuarkusDeclarativeConfiguration.Customizations.builder(NO_COMPONENTS).build())) {

            assertThat(sdk).isNotNull();
        }
    }

    @Test
    void typedSamplersAreBuiltDirectly() {
        assertThat(sampler(new SamplerModel().withAlwaysOn(new AlwaysOnSamplerModel())))
                .isEqualTo(Sampler.alwaysOn());
        assertThat(sampler(new SamplerModel().withAlwaysOff(new AlwaysOffSamplerModel())))
                .isEqualTo(Sampler.alwaysOff());
        assertThat(sampler(new SamplerModel()
                .withTraceIdRatioBased(new TraceIdRatioBasedSamplerModel().withRatio(0.25d))))
                .isEqualTo(Sampler.traceIdRatioBased(0.25d));
        assertThat(sampler(new SamplerModel().withParentBased(new ParentBasedSamplerModel()
                .withRoot(new SamplerModel().withAlwaysOff(new AlwaysOffSamplerModel())))))
                .isEqualTo(Sampler.parentBased(Sampler.alwaysOff()));
    }

    @Test
    void customSamplerResolvesViaComponentProvider() {
        Sampler custom = Sampler.traceIdRatioBased(0.75d);
        ComponentLoader loader = new StubComponentLoader(Map.of(
                ComponentProvider.class, List.of(componentProvider(Sampler.class, "my_sampler", custom))));

        SamplerModel model = new SamplerModel().withAdditionalProperty("my_sampler", new SamplerPropertyModel());
        assertThat(QuarkusDeclarativeConfiguration.createSampler(model, loader)).isSameAs(custom);
    }

    @Test
    void customSamplerFallsBackToLegacySpi() {
        Sampler custom = Sampler.traceIdRatioBased(0.75d);
        ComponentLoader loader = new StubComponentLoader(Map.of(
                ConfigurableSamplerProvider.class, List.of(new ConfigurableSamplerProvider() {
                    @Override
                    public Sampler createSampler(ConfigProperties config) {
                        return custom;
                    }

                    @Override
                    public String getName() {
                        return "my_sampler";
                    }
                })));

        SamplerModel model = new SamplerModel().withAdditionalProperty("my_sampler", new SamplerPropertyModel());
        assertThat(QuarkusDeclarativeConfiguration.createSampler(model, loader)).isSameAs(custom);
    }

    @SuppressWarnings("resource")
    @Test
    void unknownSamplerNameFails() {
        SamplerModel model = new SamplerModel().withAdditionalProperty("missing", new SamplerPropertyModel());
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
                QuarkusDeclarativeConfiguration.Customizations.builder(NO_COMPONENTS).build())) {

            assertThat(sdk.getSdkTracerProvider()).isNotNull();
            assertThat(sdk.getTracer("test").spanBuilder("span").startSpan().isRecording()).isTrue();
            assertThat(sdk.getPropagators().getTextMapPropagator().fields()).contains("traceparent", "baggage");
        }
    }

    /**
     * Port of the former {@code DeclarativeConfiguration.create()} round-trip test: the
     * {@code quarkus_drop_targets} extension node resolves inline, wrapping the delegate sampler so that suppressed
     * URIs are not sampled.
     */
    @Test
    void dropTargetsSamplerModelBuildsARealSdk() {
        OpenTelemetryConfigurationModel model = QuarkusOTelModelBuilderTest.buildModel(Map.of(
                "quarkus.otel.traces.exporter", "none",
                "quarkus.otel.metrics.exporter", "none",
                "quarkus.otel.logs.exporter", "none",
                "quarkus.otel.traces.sampler", "always_on"),
                Set.of("/q/health"), Set.of());

        Sampler sampler = QuarkusDeclarativeConfiguration.createSampler(model.getTracerProvider().getSampler(),
                NO_COMPONENTS);
        assertThat(sampler).isInstanceOf(DropTargetsSampler.class);

        try (OpenTelemetrySdk sdk = QuarkusDeclarativeConfiguration.create(model,
                QuarkusDeclarativeConfiguration.Customizations.builder(NO_COMPONENTS).build())) {
            assertThat(sdk.getSdkTracerProvider()).isNotNull();
        }
    }

    @Test
    void cdiSamplerBeanReplacesConfiguredSamplerAndKeepsDropTargets() {
        OpenTelemetryConfigurationModel model = QuarkusOTelModelBuilderTest.buildModel(Map.of(
                "quarkus.otel.traces.exporter", "none",
                "quarkus.otel.metrics.exporter", "none",
                "quarkus.otel.logs.exporter", "none",
                "quarkus.otel.traces.sampler", "always_off"),
                Set.of("/q/health"), Set.of());

        try (OpenTelemetrySdk sdk = QuarkusDeclarativeConfiguration.create(model,
                QuarkusDeclarativeConfiguration.Customizations.builder(NO_COMPONENTS)
                        .sampler(Sampler.alwaysOn())
                        .build())) {

            // always_off from the configuration was replaced by the CDI-provided always-on sampler (still wrapped for
            // drop targets, which only affects the suppressed URIs)
            assertThat(sdk.getTracer("test").spanBuilder("span").startSpan().isRecording()).isTrue();
        }
    }

    @Test
    void cdiSamplerBeanReplacesConfiguredAlwaysOffSampler() {
        // With no drop targets the model carries a bare always_off node; the CDI bean replaces it like any other
        // configured sampler. Disabled traces still end up always_off because the recorder does not supply a
        // sampler customization when tracing is disabled.
        OpenTelemetryConfigurationModel model = QuarkusOTelModelBuilderTest.buildModel(Map.of(
                "quarkus.otel.traces.exporter", "none",
                "quarkus.otel.metrics.exporter", "none",
                "quarkus.otel.logs.exporter", "none",
                "quarkus.otel.traces.sampler", "always_off"));

        try (OpenTelemetrySdk sdk = QuarkusDeclarativeConfiguration.create(model,
                QuarkusDeclarativeConfiguration.Customizations.builder(NO_COMPONENTS)
                        .sampler(Sampler.alwaysOn())
                        .build())) {

            assertThat(sdk.getTracer("test").spanBuilder("span").startSpan().isRecording()).isTrue();
        }
    }

    @SuppressWarnings("resource")
    @Test
    void multiKeyExtensionSamplerNodeFails() {
        SamplerModel model = new SamplerModel()
                .withAdditionalProperty("first", new SamplerPropertyModel())
                .withAdditionalProperty("second", new SamplerPropertyModel());
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
                QuarkusDeclarativeConfiguration.Customizations.builder(loader).build())) {
            assertThat(created).isTrue();
        }
    }

    @Test
    void customExporterNameFallsBackToLegacySpi() {
        AtomicBoolean created = new AtomicBoolean();
        ComponentLoader loader = new StubComponentLoader(Map.of(
                ConfigurableSpanExporterProvider.class, List.of(new ConfigurableSpanExporterProvider() {
                    @Override
                    public SpanExporter createExporter(ConfigProperties config) {
                        return exporter(created);
                    }

                    @Override
                    public String getName() {
                        return "my-exporter";
                    }
                })));

        OpenTelemetryConfigurationModel model = QuarkusOTelModelBuilderTest.buildModel(Map.of(
                "quarkus.otel.traces.exporter", "my-exporter",
                "quarkus.otel.metrics.exporter", "none",
                "quarkus.otel.logs.exporter", "none"));

        try (OpenTelemetrySdk sdk = QuarkusDeclarativeConfiguration.create(model,
                QuarkusDeclarativeConfiguration.Customizations.builder(loader).build())) {
            assertThat(sdk.getSdkTracerProvider()).isNotNull();
        }
    }

    @Test
    void quarkusPropagatorCustomizersRunBeforeSpiPropagatorCustomizers() {
        // Matches autoconfigure, which invoked the SPI AutoConfigurationCustomizerProviders at build time, after
        // the Quarkus customizer beans had registered: the SPI function wraps the Quarkus-customized propagator.
        List<String> order = new ArrayList<>();

        AutoConfigurationCustomizerProvider spiProvider = new AutoConfigurationCustomizerProvider() {
            @Override
            public void customize(AutoConfigurationCustomizer autoConfiguration) {
                autoConfiguration.addPropagatorCustomizer((propagator, config) -> {
                    order.add("spi");
                    return propagator;
                });
            }
        };
        ComponentLoader loader = new StubComponentLoader(Map.of(
                AutoConfigurationCustomizerProvider.class, List.of(spiProvider)));

        OpenTelemetryConfigurationModel model = QuarkusOTelModelBuilderTest.buildModel(Map.of(
                "quarkus.otel.propagators", "tracecontext",
                "quarkus.otel.traces.exporter", "none",
                "quarkus.otel.metrics.exporter", "none",
                "quarkus.otel.logs.exporter", "none"));

        try (OpenTelemetrySdk sdk = QuarkusDeclarativeConfiguration.create(model,
                QuarkusDeclarativeConfiguration.Customizations.builder(loader)
                        .propagatorCustomizer(propagator -> {
                            order.add("quarkus");
                            return propagator;
                        })
                        .autoConfigurationCustomizers(AutoConfigurationCustomizers.load(loader))
                        .build())) {
            assertThat(order).containsExactly("quarkus", "spi");
        }
    }

    @Test
    void propagatorCustomizerIsAppliedToEachPropagator() {
        List<String> seen = new ArrayList<>();

        OpenTelemetryConfigurationModel model = QuarkusOTelModelBuilderTest.buildModel(Map.of(
                "quarkus.otel.traces.exporter", "none",
                "quarkus.otel.metrics.exporter", "none",
                "quarkus.otel.logs.exporter", "none"));

        try (OpenTelemetrySdk sdk = QuarkusDeclarativeConfiguration.create(model,
                QuarkusDeclarativeConfiguration.Customizations.builder(NO_COMPONENTS)
                        .propagatorCustomizer(propagator -> {
                            seen.add(propagator.getClass().getSimpleName());
                            return propagator;
                        })
                        .build())) {
            // the default propagators, customized individually
            assertThat(seen).containsExactlyInAnyOrder("W3CTraceContextPropagator", "W3CBaggagePropagator");
        }
    }

    @Test
    void legacySpiComponentsReceiveLegacyConfigProperties() {
        AtomicReference<ConfigProperties> received = new AtomicReference<>();
        ComponentLoader loader = new StubComponentLoader(Map.of(
                ConfigurableSamplerProvider.class, List.of(new ConfigurableSamplerProvider() {
                    @Override
                    public Sampler createSampler(ConfigProperties config) {
                        received.set(config);
                        return Sampler.alwaysOn();
                    }

                    @Override
                    public String getName() {
                        return "my_sampler";
                    }
                })));

        OpenTelemetryConfigurationModel model = QuarkusOTelModelBuilderTest.buildModel(Map.of(
                "quarkus.otel.traces.sampler", "my_sampler",
                "quarkus.otel.traces.exporter", "none",
                "quarkus.otel.metrics.exporter", "none",
                "quarkus.otel.logs.exporter", "none"));

        try (OpenTelemetrySdk sdk = QuarkusDeclarativeConfiguration.create(model,
                QuarkusDeclarativeConfiguration.Customizations.builder(loader)
                        .otelConfigProperties(DefaultConfigProperties.createFromMap(
                                Map.of("otel.traces.sampler.arg", "endpoint=http://localhost:5778")))
                        .build())) {
            // the sampler is wrapped for drop targets by default, so this also covers the nested resolution
            assertThat(received.get().getString("otel.traces.sampler.arg")).isEqualTo("endpoint=http://localhost:5778");
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
                QuarkusDeclarativeConfiguration.Customizations.builder(NO_COMPONENTS).build())) {

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
                QuarkusDeclarativeConfiguration.Customizations.builder(NO_COMPONENTS).build())) {

            assertThat(sdk.getSdkTracerProvider().getSpanLimits().getMaxAttributeValueLength()).isEqualTo(8);
        }
    }

    @Test
    void noopExporterGetsNoBatchProcessor() {
        // SpanExporter.composite() with no delegates is the NoopSpanExporter, as "cdi" produces with no beans
        ComponentLoader loader = new StubComponentLoader(Map.of(
                ComponentProvider.class, List.of(componentProvider(SpanExporter.class, "my-exporter",
                        SpanExporter.composite()))));

        OpenTelemetryConfigurationModel model = QuarkusOTelModelBuilderTest.buildModel(Map.of(
                "quarkus.otel.traces.exporter", "my-exporter",
                "quarkus.otel.metrics.exporter", "none",
                "quarkus.otel.logs.exporter", "none"));

        try (OpenTelemetrySdk sdk = QuarkusDeclarativeConfiguration.create(model,
                QuarkusDeclarativeConfiguration.Customizations.builder(loader).build())) {

            assertThat(sdk.getSdkTracerProvider().toString()).doesNotContain("BatchSpanProcessor");
        }
    }

    @Test
    void noopExporterKeepsBatchProcessorWhenCdiSpanProcessorsExist() {
        ComponentLoader loader = new StubComponentLoader(Map.of(
                ComponentProvider.class, List.of(componentProvider(SpanExporter.class, "my-exporter",
                        SpanExporter.composite()))));

        OpenTelemetryConfigurationModel model = QuarkusOTelModelBuilderTest.buildModel(Map.of(
                "quarkus.otel.traces.exporter", "my-exporter",
                "quarkus.otel.metrics.exporter", "none",
                "quarkus.otel.logs.exporter", "none"));

        try (OpenTelemetrySdk sdk = QuarkusDeclarativeConfiguration.create(model,
                QuarkusDeclarativeConfiguration.Customizations.builder(loader)
                        .addSpanProcessor(SpanProcessor.composite())
                        .build())) {

            assertThat(sdk.getSdkTracerProvider().toString()).contains("BatchSpanProcessor");
        }
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
                QuarkusDeclarativeConfiguration.Customizations.builder(loader).build()))
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

    @Test
    void spiResourceProvidersAreMergedInOrder() {
        // FirstResourceProvider has the lower order, so SecondResourceProvider's value wins the shared key
        ComponentLoader loader = new StubComponentLoader(Map.of(
                ResourceProvider.class, List.of(new SecondResourceProvider(), new FirstResourceProvider())));

        Resource resource = QuarkusDeclarativeConfiguration.createSpiResource(loader,
                DefaultConfigProperties.createFromMap(Map.of()));

        assertThat(resource.getAttribute(AttributeKey.stringKey("first.key"))).isEqualTo("first");
        assertThat(resource.getAttribute(AttributeKey.stringKey("shared.key"))).isEqualTo("second");
    }

    @Test
    void disabledSpiResourceProviderIsSkipped() {
        ComponentLoader loader = new StubComponentLoader(Map.of(
                ResourceProvider.class, List.of(new FirstResourceProvider(), new SecondResourceProvider())));

        Resource resource = QuarkusDeclarativeConfiguration.createSpiResource(loader,
                DefaultConfigProperties.createFromMap(Map.of(
                        "otel.java.disabled.resource.providers", SecondResourceProvider.class.getName())));

        assertThat(resource.getAttribute(AttributeKey.stringKey("first.key"))).isEqualTo("first");
        assertThat(resource.getAttribute(AttributeKey.stringKey("shared.key"))).isEqualTo("first");
    }

    @Test
    void enabledSpiResourceProvidersActAsAllowList() {
        ComponentLoader loader = new StubComponentLoader(Map.of(
                ResourceProvider.class, List.of(new FirstResourceProvider(), new SecondResourceProvider())));

        Resource resource = QuarkusDeclarativeConfiguration.createSpiResource(loader,
                DefaultConfigProperties.createFromMap(Map.of(
                        "otel.java.enabled.resource.providers", SecondResourceProvider.class.getName())));

        assertThat(resource.getAttribute(AttributeKey.stringKey("first.key"))).isNull();
        assertThat(resource.getAttribute(AttributeKey.stringKey("shared.key"))).isEqualTo("second");
    }

    @Test
    void conditionalSpiResourceProviderIsConsulted() {
        ComponentLoader loader = new StubComponentLoader(Map.of(
                ResourceProvider.class, List.of(new NeverApplyResourceProvider())));

        Resource resource = QuarkusDeclarativeConfiguration.createSpiResource(loader,
                DefaultConfigProperties.createFromMap(Map.of()));

        assertThat(resource.getAttribute(AttributeKey.stringKey("never.key"))).isNull();
    }

    @Test
    void disabledResourceKeysAreFiltered() {
        ComponentLoader loader = new StubComponentLoader(Map.of(
                ResourceProvider.class, List.of(new FirstResourceProvider())));

        Resource resource = QuarkusDeclarativeConfiguration.createSpiResource(loader,
                DefaultConfigProperties.createFromMap(Map.of("otel.resource.disabled.keys", "first.key")));

        assertThat(resource.getAttribute(AttributeKey.stringKey("first.key"))).isNull();
        assertThat(resource.getAttribute(AttributeKey.stringKey("shared.key"))).isEqualTo("first");
    }

    public static class FirstResourceProvider implements ResourceProvider {
        @Override
        public Resource createResource(ConfigProperties config) {
            return Resource.create(Attributes.of(
                    AttributeKey.stringKey("first.key"), "first",
                    AttributeKey.stringKey("shared.key"), "first"));
        }

        @Override
        public int order() {
            return 1;
        }
    }

    public static class SecondResourceProvider implements ResourceProvider {
        @Override
        public Resource createResource(ConfigProperties config) {
            return Resource.create(Attributes.of(AttributeKey.stringKey("shared.key"), "second"));
        }

        @Override
        public int order() {
            return 2;
        }
    }

    public static class NeverApplyResourceProvider implements ConditionalResourceProvider {
        @Override
        public Resource createResource(ConfigProperties config) {
            return Resource.create(Attributes.of(AttributeKey.stringKey("never.key"), "never"));
        }

        @Override
        public boolean shouldApply(ConfigProperties config, Resource existing) {
            return false;
        }
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

    private record StubComponentLoader(Map<Class<?>, List<Object>> services) implements ComponentLoader {

        @SuppressWarnings("unchecked")
        @Override
        public <T> Iterable<T> load(Class<T> type) {
            return (Iterable<T>) services.getOrDefault(type, List.of());
        }
    }
}
