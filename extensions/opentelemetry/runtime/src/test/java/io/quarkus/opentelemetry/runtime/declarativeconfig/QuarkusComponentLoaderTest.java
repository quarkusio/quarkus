package io.quarkus.opentelemetry.runtime.declarativeconfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

import io.opentelemetry.api.incubator.config.DeclarativeConfigProperties;
import io.opentelemetry.common.ComponentLoader;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.TextMapGetter;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.context.propagation.TextMapSetter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.OpenTelemetryConfigurationModel;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.SamplerModel;
import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizer;
import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizerProvider;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigurablePropagatorProvider;
import io.opentelemetry.sdk.autoconfigure.spi.ResourceProvider;
import io.opentelemetry.sdk.autoconfigure.spi.internal.AutoConfigureListener;
import io.opentelemetry.sdk.autoconfigure.spi.internal.ComponentProvider;
import io.opentelemetry.sdk.autoconfigure.spi.internal.DefaultConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.traces.ConfigurableSamplerProvider;
import io.opentelemetry.sdk.autoconfigure.spi.traces.ConfigurableSpanExporterProvider;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.common.InternalTelemetryVersion;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import io.quarkus.opentelemetry.runtime.declarativeconfig.spi.QuarkusCompositeLogRecordExporterProvider;
import io.quarkus.opentelemetry.runtime.declarativeconfig.spi.QuarkusCompositeSpanExporterProvider;
import io.quarkus.opentelemetry.runtime.declarativeconfig.spi.QuarkusDropAllSpanProcessorProvider;

class QuarkusComponentLoaderTest {

    private static final ConfigProperties NO_PROPERTIES = DefaultConfigProperties.createFromMap(Map.of());

    @Test
    void quarkusProviderReplacesSameNamedDelegateProvider() {
        ComponentProvider quarkus = componentProvider(SpanExporter.class, "cdi");
        ComponentProvider delegateSameName = componentProvider(SpanExporter.class, "cdi");
        ComponentProvider delegateOther = componentProvider(SpanExporter.class, "zipkin");
        QuarkusComponentLoader loader = new QuarkusComponentLoader(
                stubLoader(Map.of(ComponentProvider.class, () -> List.of(delegateSameName, delegateOther))),
                NO_PROPERTIES, List.of(quarkus));

        assertThat(loader.load(ComponentProvider.class)).containsExactly(quarkus, delegateOther);
    }

    @Test
    void addedQuarkusProviderWinsUntilTheProvidersAreLoaded() {
        ComponentProvider quarkus = componentProvider(SpanExporter.class, "cdi");
        ComponentProvider delegate = componentProvider(SpanExporter.class, "cdi");
        QuarkusComponentLoader loader = new QuarkusComponentLoader(
                stubLoader(Map.of(ComponentProvider.class, () -> List.of(delegate))), NO_PROPERTIES, List.of());

        loader.addComponentProvider(quarkus);
        assertThat(loader.load(ComponentProvider.class)).containsExactly(quarkus);

        assertThatThrownBy(() -> loader.addComponentProvider(componentProvider(SpanExporter.class, "late")))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void sameNameOfAnotherTypeIsKept() {
        ComponentProvider quarkus = componentProvider(SpanExporter.class, "custom");
        ComponentProvider delegate = componentProvider(Sampler.class, "custom");
        QuarkusComponentLoader loader = new QuarkusComponentLoader(
                stubLoader(Map.of(ComponentProvider.class, () -> List.of(delegate))), NO_PROPERTIES,
                List.of(quarkus));

        assertThat(loader.load(ComponentProvider.class)).containsExactly(quarkus, delegate);
    }

    @Test
    void componentProvidersAreLoadedOnce() {
        // a ServiceLoader-backed delegate creates new instances on every load
        QuarkusComponentLoader loader = new QuarkusComponentLoader(stubLoader(Map.of(ComponentProvider.class,
                () -> List.of(componentProvider(SpanExporter.class, "zipkin")))), NO_PROPERTIES, List.of());

        List<ComponentProvider> first = new ArrayList<>();
        loader.load(ComponentProvider.class).forEach(first::add);
        List<ComponentProvider> second = new ArrayList<>();
        loader.load(ComponentProvider.class).forEach(second::add);

        assertThat(second).hasSize(1);
        assertThat(second.get(0)).isSameAs(first.get(0));
    }

    @Test
    void otherSpisAreLoadedByTheDelegate() {
        ResourceProvider resourceProvider = config -> Resource.empty();
        QuarkusComponentLoader loader = new QuarkusComponentLoader(
                stubLoader(Map.of(ResourceProvider.class, () -> List.of(resourceProvider))),
                NO_PROPERTIES, List.of(componentProvider(SpanExporter.class, "cdi")));

        assertThat(loader.load(ResourceProvider.class)).containsExactly(resourceProvider);
    }

    @Test
    void customExporterNameResolvesThroughConfigurableProvider() {
        AtomicReference<ConfigProperties> received = new AtomicReference<>();
        QuarkusComponentLoader loader = new QuarkusComponentLoader(
                stubLoader(Map.of(ConfigurableSpanExporterProvider.class,
                        () -> List.of(spanExporterProvider("my-exporter", received)))),
                NO_PROPERTIES, modelProviders());

        OpenTelemetryConfigurationModel model = QuarkusOTelModelBuilderTest.buildModel(Map.of(
                "quarkus.otel.traces.exporter", "my-exporter",
                "quarkus.otel.metrics.exporter", "none",
                "quarkus.otel.logs.exporter", "none"));

        try (OpenTelemetrySdk sdk = QuarkusDeclarativeConfiguration.create(model,
                loader, InternalTelemetryVersion.LATEST, Resource.getDefault())) {
            assertThat(received.get()).isNotNull();
            assertThat(sdk.getSdkTracerProvider()).isNotNull();
        }
    }

    @Test
    void configurableProviderWinsOverASameNamedComponentProvider() {
        // Upstream artifacts such as opentelemetry-exporter-zipkin register both SPIs under one name. Autoconfigure
        // only knew the Configurable one, which reads the flat otel.* properties.
        AtomicReference<ConfigProperties> received = new AtomicReference<>();
        QuarkusComponentLoader loader = new QuarkusComponentLoader(
                stubLoader(Map.of(
                        ComponentProvider.class, () -> List.of(componentProvider(SpanExporter.class, "zipkin")),
                        ConfigurableSpanExporterProvider.class, () -> List.of(spanExporterProvider("zipkin", received)))),
                DefaultConfigProperties.createFromMap(
                        Map.of("otel.exporter.zipkin.endpoint", "http://zipkin:9411/api/v2/spans")),
                modelProviders());
        OpenTelemetryConfigurationModel model = QuarkusOTelModelBuilderTest.buildModel(Map.of(
                "quarkus.otel.traces.exporter", "zipkin",
                "quarkus.otel.metrics.exporter", "none",
                "quarkus.otel.logs.exporter", "none"));

        try (OpenTelemetrySdk sdk = QuarkusDeclarativeConfiguration.create(model,
                loader, InternalTelemetryVersion.LATEST, Resource.getDefault())) {

            assertThat(received.get()).isNotNull();
            assertThat(received.get().getString("otel.exporter.zipkin.endpoint"))
                    .isEqualTo("http://zipkin:9411/api/v2/spans");
        }
    }

    @Test
    void lastConfigurableProviderOfANameWins() {
        // autoconfigure's SpiHelper.loadConfigurable keeps the last provider loaded for a name
        AtomicReference<ConfigProperties> first = new AtomicReference<>();
        AtomicReference<ConfigProperties> last = new AtomicReference<>();
        QuarkusComponentLoader loader = new QuarkusComponentLoader(
                stubLoader(Map.of(ConfigurableSpanExporterProvider.class,
                        () -> List.of(spanExporterProvider("dup", first), spanExporterProvider("dup", last)))),
                NO_PROPERTIES, List.of());

        List<ComponentProvider> providers = new ArrayList<>();
        loader.load(ComponentProvider.class).forEach(providers::add);
        assertThat(providers).hasSize(1);
        providers.get(0).create(DeclarativeConfigProperties.empty());

        assertThat(first.get()).isNull();
        assertThat(last.get()).isNotNull();
    }

    @Test
    void customSamplerResolvesThroughConfigurableProvider() {
        Sampler custom = Sampler.traceIdRatioBased(0.75d);
        QuarkusComponentLoader loader = new QuarkusComponentLoader(
                stubLoader(Map.of(ConfigurableSamplerProvider.class,
                        () -> List.of(samplerProvider("my_sampler", custom, new AtomicReference<>())))),
                NO_PROPERTIES, List.of());

        SamplerModel model = new SamplerModel().setExtensionProperty("my_sampler", Map.of());
        assertThat(QuarkusDeclarativeConfiguration.createSampler(model, loader)).isSameAs(custom);
    }

    @Test
    void configurableSamplerReceivesTheFlatProperties() {
        AtomicReference<ConfigProperties> received = new AtomicReference<>();
        QuarkusComponentLoader loader = new QuarkusComponentLoader(
                stubLoader(Map.of(ConfigurableSamplerProvider.class,
                        () -> List.of(samplerProvider("my_sampler", Sampler.alwaysOn(), received)))),
                DefaultConfigProperties.createFromMap(
                        Map.of("otel.traces.sampler.arg", "endpoint=http://localhost:5778")),
                modelProviders());

        OpenTelemetryConfigurationModel model = QuarkusOTelModelBuilderTest.buildModel(Map.of(
                "quarkus.otel.traces.sampler", "my_sampler",
                "quarkus.otel.traces.exporter", "none",
                "quarkus.otel.metrics.exporter", "none",
                "quarkus.otel.logs.exporter", "none"));

        try (OpenTelemetrySdk sdk = QuarkusDeclarativeConfiguration.create(model,
                loader, InternalTelemetryVersion.LATEST, Resource.getDefault())) {
            // the sampler is wrapped for drop targets by default, so this also covers the nested resolution
            assertThat(received.get().getString("otel.traces.sampler.arg")).isEqualTo("endpoint=http://localhost:5778");
        }
    }

    @Test
    void customPropagatorResolvesThroughConfigurableProvider() {
        TextMapPropagator custom = TextMapPropagator.noop();
        AtomicReference<ConfigProperties> received = new AtomicReference<>();
        ConfigurablePropagatorProvider provider = new ConfigurablePropagatorProvider() {
            @Override
            public TextMapPropagator getPropagator(ConfigProperties config) {
                received.set(config);
                return custom;
            }

            @Override
            public String getName() {
                return "my_propagator";
            }
        };
        QuarkusComponentLoader loader = new QuarkusComponentLoader(
                stubLoader(Map.of(ConfigurablePropagatorProvider.class, () -> List.of(provider))),
                NO_PROPERTIES, List.of());

        List<ComponentProvider> providers = new ArrayList<>();
        loader.load(ComponentProvider.class).forEach(providers::add);

        assertThat(providers).singleElement().satisfies(adapted -> {
            assertThat(adapted.getType()).isEqualTo(TextMapPropagator.class);
            assertThat(adapted.getName()).isEqualTo("my_propagator");
            assertThat(adapted.create(DeclarativeConfigProperties.empty())).isSameAs(custom);
        });
        assertThat(received.get()).isSameAs(NO_PROPERTIES);
    }

    @Test
    void configurableProviderListenerReceivesTheSdk() {
        AtomicReference<OpenTelemetrySdk> received = new AtomicReference<>();
        class ListeningProvider implements ConfigurableSpanExporterProvider, AutoConfigureListener {
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
                received.set(sdk);
            }
        }
        // a new instance on every load: the listener must reach the instance that created the exporter
        QuarkusComponentLoader loader = new QuarkusComponentLoader(
                stubLoader(Map.of(ConfigurableSpanExporterProvider.class, () -> List.of(new ListeningProvider()))),
                NO_PROPERTIES, modelProviders());
        OpenTelemetryConfigurationModel model = QuarkusOTelModelBuilderTest.buildModel(Map.of(
                "quarkus.otel.traces.exporter", "listening",
                "quarkus.otel.metrics.exporter", "none",
                "quarkus.otel.logs.exporter", "none"));

        try (OpenTelemetrySdk sdk = QuarkusDeclarativeConfiguration.create(model,
                loader, InternalTelemetryVersion.LATEST, Resource.getDefault())) {
            assertThat(received.get()).isSameAs(sdk);
        }
    }

    @Test
    void unusedConfigurableProviderIsNotCalled() {
        // autoconfigure only loaded a signal's exporter providers when that signal exported, so a provider whose
        // component was never created must not receive the SDK
        AtomicReference<OpenTelemetrySdk> received = new AtomicReference<>();
        QuarkusComponentLoader loader = new QuarkusComponentLoader(
                stubLoader(Map.of(ConfigurableSpanExporterProvider.class,
                        () -> List.of(new ListeningSpanExporterProvider(received, null)))),
                NO_PROPERTIES, modelProviders());
        OpenTelemetryConfigurationModel model = QuarkusOTelModelBuilderTest.buildModel(Map.of(
                "quarkus.otel.traces.exporter", "none",
                "quarkus.otel.metrics.exporter", "none",
                "quarkus.otel.logs.exporter", "none"));

        try (OpenTelemetrySdk sdk = QuarkusDeclarativeConfiguration.create(model,
                loader, InternalTelemetryVersion.LATEST, Resource.getDefault())) {
            assertThat(received.get()).isNull();
        }
    }

    @Test
    void createdComponentListenerIsCalledWhenTheProviderListenerThrows() {
        AtomicReference<OpenTelemetrySdk> received = new AtomicReference<>();
        QuarkusComponentLoader loader = new QuarkusComponentLoader(
                stubLoader(Map.of(ConfigurableSpanExporterProvider.class,
                        () -> List.of(new ListeningSpanExporterProvider(null, received)))),
                NO_PROPERTIES, modelProviders());
        OpenTelemetryConfigurationModel model = QuarkusOTelModelBuilderTest.buildModel(Map.of(
                "quarkus.otel.traces.exporter", "listening",
                "quarkus.otel.metrics.exporter", "none",
                "quarkus.otel.logs.exporter", "none"));

        try (OpenTelemetrySdk sdk = QuarkusDeclarativeConfiguration.create(model,
                loader, InternalTelemetryVersion.LATEST, Resource.getDefault())) {
            assertThat(received.get()).isSameAs(sdk);
        }
    }

    @Test
    void listenersOfOtherSpisAreRecorded() {
        class ListeningResourceProvider implements ResourceProvider, AutoConfigureListener {
            @Override
            public Resource createResource(ConfigProperties config) {
                return Resource.empty();
            }

            @Override
            public void afterAutoConfigure(OpenTelemetrySdk sdk) {
            }
        }
        ResourceProvider plain = config -> Resource.empty();
        ListeningResourceProvider listening = new ListeningResourceProvider();
        QuarkusComponentLoader loader = new QuarkusComponentLoader(
                stubLoader(Map.of(ResourceProvider.class, () -> List.of(plain, listening))), NO_PROPERTIES, List.of());

        assertThat(loader.listeners()).isEmpty();
        loader.load(ResourceProvider.class);

        assertThat(loader.listeners()).containsExactly(listening);
    }

    @Test
    void listenersOfOtherSpisAreNotRecorded() {
        // whoever loads these SPIs calls their listeners: upstream's DeclarativeConfiguration loads its customizer
        // providers through this loader, so recording them would call those listeners twice
        class ListeningCustomizerProvider implements AutoConfigurationCustomizerProvider, AutoConfigureListener {
            @Override
            public void customize(AutoConfigurationCustomizer autoConfiguration) {
            }

            @Override
            public void afterAutoConfigure(OpenTelemetrySdk sdk) {
            }
        }
        QuarkusComponentLoader loader = new QuarkusComponentLoader(
                stubLoader(Map.of(AutoConfigurationCustomizerProvider.class,
                        () -> List.of(new ListeningCustomizerProvider()))),
                NO_PROPERTIES, List.of());

        loader.load(AutoConfigurationCustomizerProvider.class);

        assertThat(loader.listeners()).isEmpty();
    }

    @Test
    void providerThatIsItsOwnComponentIsCalledOnce() {
        AtomicInteger calls = new AtomicInteger();
        final class SelfExporterProvider implements ConfigurableSpanExporterProvider, SpanExporter, AutoConfigureListener {
            @Override
            public SpanExporter createExporter(ConfigProperties config) {
                return this;
            }

            @Override
            public String getName() {
                return "self";
            }

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

            @Override
            public void afterAutoConfigure(OpenTelemetrySdk sdk) {
                calls.incrementAndGet();
            }
        }
        QuarkusComponentLoader loader = new QuarkusComponentLoader(
                stubLoader(Map.of(ConfigurableSpanExporterProvider.class, () -> List.of(new SelfExporterProvider()))),
                NO_PROPERTIES, modelProviders());
        OpenTelemetryConfigurationModel model = QuarkusOTelModelBuilderTest.buildModel(Map.of(
                "quarkus.otel.traces.exporter", "self",
                "quarkus.otel.metrics.exporter", "none",
                "quarkus.otel.logs.exporter", "none"));

        try (OpenTelemetrySdk sdk = QuarkusDeclarativeConfiguration.create(model,
                loader, InternalTelemetryVersion.LATEST, Resource.getDefault())) {
            assertThat(calls).hasValue(1);
        }
    }

    @Test
    void customPropagatorIsResolvedEndToEnd() {
        TextMapPropagator custom = new TextMapPropagator() {
            @Override
            public Collection<String> fields() {
                return List.of("x-custom");
            }

            @Override
            public <C> void inject(Context context, C carrier, TextMapSetter<C> setter) {
            }

            @Override
            public <C> Context extract(Context context, C carrier, TextMapGetter<C> getter) {
                return context;
            }
        };
        ConfigurablePropagatorProvider provider = new ConfigurablePropagatorProvider() {
            @Override
            public TextMapPropagator getPropagator(ConfigProperties config) {
                return custom;
            }

            @Override
            public String getName() {
                return "my_propagator";
            }
        };
        QuarkusComponentLoader loader = new QuarkusComponentLoader(
                stubLoader(Map.of(ConfigurablePropagatorProvider.class, () -> List.of(provider))),
                NO_PROPERTIES, modelProviders());
        OpenTelemetryConfigurationModel model = QuarkusOTelModelBuilderTest.buildModel(Map.of(
                "quarkus.otel.propagators", "tracecontext,my_propagator",
                "quarkus.otel.traces.exporter", "none",
                "quarkus.otel.metrics.exporter", "none",
                "quarkus.otel.logs.exporter", "none"));

        try (OpenTelemetrySdk sdk = QuarkusDeclarativeConfiguration.create(model,
                loader, InternalTelemetryVersion.LATEST, Resource.getDefault())) {
            assertThat(sdk.getPropagators().getTextMapPropagator().fields()).contains("traceparent", "x-custom");
        }
    }

    // The providers the module's service file registers for nodes the model always carries
    private static List<ComponentProvider> modelProviders() {
        return List.of(new QuarkusDropAllSpanProcessorProvider(), new QuarkusCompositeSpanExporterProvider(),
                new QuarkusCompositeLogRecordExporterProvider());
    }

    static ComponentProvider componentProvider(Class<?> type, String name) {
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
                throw new UnsupportedOperationException();
            }
        };
    }

    static ConfigurableSpanExporterProvider spanExporterProvider(String name, AtomicReference<ConfigProperties> received) {
        return new ConfigurableSpanExporterProvider() {
            @Override
            public SpanExporter createExporter(ConfigProperties config) {
                received.set(config);
                return SpanExporter.composite();
            }

            @Override
            public String getName() {
                return name;
            }
        };
    }

    static ConfigurableSamplerProvider samplerProvider(String name, Sampler sampler,
            AtomicReference<ConfigProperties> received) {
        return new ConfigurableSamplerProvider() {
            @Override
            public Sampler createSampler(ConfigProperties config) {
                received.set(config);
                return sampler;
            }

            @Override
            public String getName() {
                return name;
            }
        };
    }

    // providerReceived null: the provider's listener throws. exporterReceived null: the exporter is not a listener.
    static final class ListeningSpanExporterProvider implements ConfigurableSpanExporterProvider, AutoConfigureListener {

        private final AtomicReference<OpenTelemetrySdk> providerReceived;
        private final AtomicReference<OpenTelemetrySdk> exporterReceived;

        ListeningSpanExporterProvider(AtomicReference<OpenTelemetrySdk> providerReceived,
                AtomicReference<OpenTelemetrySdk> exporterReceived) {
            this.providerReceived = providerReceived;
            this.exporterReceived = exporterReceived;
        }

        @Override
        public SpanExporter createExporter(ConfigProperties config) {
            return exporterReceived == null ? SpanExporter.composite() : new ListeningSpanExporter(exporterReceived);
        }

        @Override
        public String getName() {
            return "listening";
        }

        @Override
        public void afterAutoConfigure(OpenTelemetrySdk sdk) {
            if (providerReceived == null) {
                throw new IllegalStateException("provider listener failure");
            }
            providerReceived.set(sdk);
        }
    }

    record ListeningSpanExporter(AtomicReference<OpenTelemetrySdk> received) implements SpanExporter, AutoConfigureListener {

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

        @Override
        public void afterAutoConfigure(OpenTelemetrySdk sdk) {
            received.set(sdk);
        }
    }

    // Calls the supplier on every load, as a ServiceLoader-backed loader creates new instances each time
    @SuppressWarnings("unchecked")
    static ComponentLoader stubLoader(Map<Class<?>, Supplier<List<?>>> services) {
        return new ComponentLoader() {
            @Override
            public <T> Iterable<T> load(Class<T> spiClass) {
                Supplier<List<?>> supplier = services.get(spiClass);
                return supplier == null ? List.of() : (List<T>) supplier.get();
            }
        };
    }
}
