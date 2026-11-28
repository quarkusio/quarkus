package io.quarkus.opentelemetry.runtime.declarativeconfig;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.spi.CDI;
import jakarta.enterprise.util.TypeLiteral;

import org.jboss.logging.Logger;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.incubator.config.DeclarativeConfigException;
import io.opentelemetry.common.ComponentLoader;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.OpenTelemetryConfigurationModel;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigurationException;
import io.opentelemetry.sdk.autoconfigure.spi.internal.AutoConfigureListener;
import io.opentelemetry.sdk.autoconfigure.spi.internal.ComponentProvider;
import io.opentelemetry.sdk.autoconfigure.spi.internal.DefaultConfigProperties;
import io.opentelemetry.sdk.common.Clock;
import io.opentelemetry.sdk.common.InternalTelemetryVersion;
import io.opentelemetry.sdk.logs.export.LogRecordExporter;
import io.opentelemetry.sdk.metrics.export.MetricExporter;
import io.opentelemetry.sdk.metrics.export.MetricReader;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.IdGenerator;
import io.opentelemetry.sdk.trace.SpanProcessor;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import io.quarkus.arc.SyntheticCreationalContext;
import io.quarkus.opentelemetry.runtime.AutoConfiguredOpenTelemetrySdkBuilderCustomizer;
import io.quarkus.opentelemetry.runtime.DelayedAttributes;
import io.quarkus.opentelemetry.runtime.config.build.OTelBuildConfig;
import io.quarkus.opentelemetry.runtime.config.runtime.OTelRuntimeConfig;
import io.quarkus.opentelemetry.runtime.declarativeconfig.spi.QuarkusCdiIdGeneratorProvider;
import io.quarkus.opentelemetry.runtime.declarativeconfig.spi.QuarkusCdiSamplerProvider;
import io.quarkus.opentelemetry.runtime.declarativeconfig.spi.QuarkusCdiSpanProcessorProvider;
import io.quarkus.opentelemetry.runtime.declarativeconfig.spi.QuarkusCompositeLogRecordExporterProvider;
import io.quarkus.opentelemetry.runtime.declarativeconfig.spi.QuarkusCompositeSpanExporterProvider;
import io.quarkus.opentelemetry.runtime.declarativeconfig.spi.QuarkusCustomizedPropagatorProvider;
import io.quarkus.opentelemetry.runtime.declarativeconfig.spi.QuarkusCustomizedSamplerProvider;
import io.quarkus.opentelemetry.runtime.declarativeconfig.spi.QuarkusDevUiMetricReaderProvider;
import io.quarkus.opentelemetry.runtime.declarativeconfig.spi.QuarkusPeriodicMetricReaderProvider;
import io.quarkus.opentelemetry.runtime.declarativeconfig.spi.QuarkusSimpleLogRecordProcessorProvider;
import io.quarkus.opentelemetry.runtime.propagation.TextMapPropagatorCustomizer;
import io.quarkus.opentelemetry.runtime.tracing.TracerRecorder;
import io.quarkus.opentelemetry.runtime.tracing.TracerUtil;
import io.quarkus.runtime.ApplicationConfig;
import io.smallrye.common.annotation.Identifier;
import io.smallrye.common.net.HostName;
import io.smallrye.config.Config;

/**
 * Initializes the OpenTelemetry SDK from the declarative configuration model, wiring in the Quarkus CDI beans and the
 * autoconfigure SPIs the model cannot express. {@code OpenTelemetryRecorder} delegates here unless {@link #select}
 * chooses the legacy engine.
 */
public final class QuarkusDeclarativeSdk {

    /**
     * The {@link Identifier} of the Dev UI's in-memory {@link MetricReader} bean, the only {@code MetricReader}
     * bean registered on the SDK meter provider.
     */
    public static final String DEV_UI_METRIC_READER = "quarkus-otel-devui";

    private static final Logger LOG = Logger.getLogger(QuarkusDeclarativeSdk.class);

    private QuarkusDeclarativeSdk() {
    }

    /**
     * Selects the engine that initializes the SDK. The legacy {@code AutoConfiguredOpenTelemetrySdk} engine is
     * selected, and a warning naming the reason and its source is logged, when one of the following holds:
     * <ul>
     * <li>an application bean of the deprecated customizer type exists</li>
     * <li>an autoconfigure SPI implementation registers a customization the declarative configuration model has no
     * equivalent for</li>
     * <li>the SDK internal telemetry version is {@code legacy}</li>
     * <li>an autoconfigure SPI implementation customizes span exporters, and the spans of the {@code cdi} traces
     * exporter are dropped because it has no {@code SpanExporter} beans (see
     * {@link QuarkusOTelModelBuilder#dropsCdiSpans()})</li>
     * <li>metrics are enabled and a {@link Clock} bean replaces the default SDK clock</li>
     * </ul>
     * A disabled SDK selects the declarative engine unless the first condition holds. Inspecting the customizer
     * beans does not create them.
     * <p>
     * Unless the first condition holds, this method loads and runs the {@code AutoConfigurationCustomizerProvider}
     * SPI implementations. The declarative engine reuses them; the legacy engine runs them a second time.
     * </p>
     *
     * @param context the synthetic bean's creational context, for the injected bean lookups
     * @param builderCustomizers every bean of the deprecated customizer type, the built-in ones included
     * @param runtimeConfig the runtime OpenTelemetry configuration
     * @param otelConfigs the flat {@code otel.*} view of the configuration, as the legacy engine receives it
     * @return the selected engine
     * @throws ConfigurationException if the {@code otel.config.file} system property or the {@code OTEL_CONFIG_FILE}
     *         environment variable is set, as file-based configuration is not supported
     */
    @SuppressWarnings("removal")
    public static Engine select(SyntheticCreationalContext<OpenTelemetry> context,
            Instance<AutoConfiguredOpenTelemetrySdkBuilderCustomizer> builderCustomizers, OTelRuntimeConfig runtimeConfig,
            Map<String, String> otelConfigs) {
        final OTelBuildConfig buildConfig = Config.get().getConfigMapping(OTelBuildConfig.class);
        final ComponentLoader componentLoader = ComponentLoader
                .forClassLoader(Thread.currentThread().getContextClassLoader());

        // As autoconfigure, which read otel.config.file from system properties and environment variables only, before
        // any customization ran, and failed on it before checking otel.sdk.disabled. Neither engine reads the file.
        String configFile = DefaultConfigProperties.create(Map.of(), componentLoader).getString("otel.config.file");
        if (configFile != null && !configFile.isEmpty()) {
            throw new ConfigurationException(
                    "otel.config.file is not supported; configure the SDK with quarkus.otel.* properties");
        }

        List<String> userCustomizers = new ArrayList<>();
        for (Instance.Handle<AutoConfiguredOpenTelemetrySdkBuilderCustomizer> handle : builderCustomizers.handles()) {
            String className = handle.getBean().getBeanClass().getName();
            if (!isBuiltIn(className)) {
                userCustomizers.add(className);
            }
        }
        if (!userCustomizers.isEmpty()) {
            return legacy("Deprecated AutoConfiguredOpenTelemetrySdkBuilderCustomizer implementations are present",
                    userCustomizers);
        }

        // The AutoConfigurationCustomizerProvider ServiceLoader SPI, required by MicroProfile Telemetry:
        // the captured functions are applied to each component after the Quarkus wiring.
        final AutoConfigurationCustomizers autoConfigurationCustomizers = AutoConfigurationCustomizers
                .load(componentLoader);

        // The flat otel.* view of the configuration: quarkus.otel.* values merged with bare otel.* ones,
        // the otel.* spelling winning under MP compatibility, with the SPI-registered properties suppliers
        // and customizers applied. The model builder makes the autoconfigure-shaped decisions from it, and
        // it is passed to components resolved through the autoconfigure SPIs and to
        // TextMapPropagatorCustomizer implementations.
        final ConfigProperties otelConfigProperties = autoConfigurationCustomizers
                .resolveConfigProperties(otelConfigs, componentLoader);
        // The loader the SDK components resolve through. No Quarkus ComponentProvider is added yet.
        final QuarkusComponentLoader quarkusComponentLoader = new QuarkusComponentLoader(componentLoader,
                otelConfigProperties, List.of());

        // A disabled SDK applies none of the customizations below on the legacy engine either
        if (otelConfigProperties.getBoolean("otel.sdk.disabled", false)) {
            return new Engine(context, buildConfig, runtimeConfig, quarkusComponentLoader, autoConfigurationCustomizers,
                    otelConfigProperties);
        }
        if (!autoConfigurationCustomizers.providersWithoutSeam().isEmpty()) {
            return legacy("Autoconfigure SPI implementations register customizations the declarative configuration "
                    + "model has no equivalent for", autoConfigurationCustomizers.providersWithoutSeam());
        }
        // Upstream's declarative configuration wires the SDK's internal telemetry as autoconfigure does only for
        // "latest". A missing value means legacy to autoconfigure; Quarkus always supplies one.
        String telemetryVersion = otelConfigProperties.getString("otel.experimental.sdk.telemetry.version");
        if (telemetryVersion == null || "legacy".equalsIgnoreCase(telemetryVersion)) {
            return legacy("The SDK internal telemetry version is legacy",
                    List.of("otel.experimental.sdk.telemetry.version"));
        }
        // The legacy SpanProcessorCustomizer replaced a batch processor around the no-op exporter, which "cdi"
        // returns without SpanExporter beans, only after the SPI span exporter customizers ran. The model decides
        // before any exporter exists, so a customizer that could replace the no-op exporter needs the legacy engine.
        if (!autoConfigurationCustomizers.spanExporterCustomizerProviders().isEmpty()
                && QuarkusOTelModelBuilder.forConfig(buildConfig, runtimeConfig, otelConfigProperties)
                        .cdiSpanExportersPresent(cdiSpanExportersPresent())
                        .cdiSpanProcessorsPresent(!context.getInjectedReference(new TypeLiteral<Instance<SpanProcessor>>() {
                        }, Any.Literal.INSTANCE).isUnsatisfied())
                        .dropsCdiSpans()) {
            return legacy("Autoconfigure SPI implementations customize span exporters while the cdi traces exporter "
                    + "has no SpanExporter beans", autoConfigurationCustomizers.spanExporterCustomizerProviders());
        }
        if (buildConfig.metrics().enabled().orElse(Boolean.TRUE)) {
            Instance<Clock> clock = context.getInjectedReference(new TypeLiteral<>() {
            });
            if (clock.isUnsatisfied()) {
                throw new IllegalStateException("No Clock bean found");
            }
            // The model has no clock. MetricsProducer's default bean returns Clock.getDefault(), which the SDK uses
            // anyway; any other clock needs the legacy engine.
            Clock resolved = clock.get();
            if (resolved != Clock.getDefault()) {
                return legacy("A Clock bean replaces the default SDK clock", List.of(resolved.getClass().getName()));
            }
        }
        return new Engine(context, buildConfig, runtimeConfig, quarkusComponentLoader, autoConfigurationCustomizers,
                otelConfigProperties);
    }

    /**
     * Returns whether {@code className} names an {@link AutoConfiguredOpenTelemetrySdkBuilderCustomizer}
     * implementation Quarkus registers itself.
     *
     * @param className the binary class name, as returned by {@link Class#getName()}
     * @return {@code true} if the class is one of the Quarkus implementations
     */
    public static boolean isBuiltIn(String className) {
        return switch (className) {
            case "io.quarkus.opentelemetry.runtime.AutoConfiguredOpenTelemetrySdkBuilderCustomizer$SimpleLogRecordProcessorCustomizer",
                    "io.quarkus.opentelemetry.runtime.AutoConfiguredOpenTelemetrySdkBuilderCustomizer$ResourceCustomizer",
                    "io.quarkus.opentelemetry.runtime.AutoConfiguredOpenTelemetrySdkBuilderCustomizer$SamplerCustomizer",
                    "io.quarkus.opentelemetry.runtime.AutoConfiguredOpenTelemetrySdkBuilderCustomizer$SpanProcessorCustomizer",
                    "io.quarkus.opentelemetry.runtime.AutoConfiguredOpenTelemetrySdkBuilderCustomizer$TracerProviderCustomizer",
                    "io.quarkus.opentelemetry.runtime.AutoConfiguredOpenTelemetrySdkBuilderCustomizer$MetricProviderCustomizer",
                    "io.quarkus.opentelemetry.runtime.AutoConfiguredOpenTelemetrySdkBuilderCustomizer$TextMapPropagatorCustomizers",
                    // Lives in quarkus-opentelemetry-runtime-dev
                    "io.quarkus.opentelemetry.runtime.devui.DevUiMetricsSdkBuilderCustomizer" ->
                true;
            default -> false;
        };
    }

    // Creates the SDK from the resolved configuration and the CDI beans the synthetic OpenTelemetry bean injects.
    private static OpenTelemetrySdk create(SyntheticCreationalContext<OpenTelemetry> context, OTelBuildConfig buildConfig,
            OTelRuntimeConfig oTelRuntimeConfig, QuarkusComponentLoader componentLoader,
            AutoConfigurationCustomizers autoConfigurationCustomizers, ConfigProperties otelConfigProperties) {
        if (otelConfigProperties.getBoolean("otel.sdk.disabled", false)) {
            // Only the propagators are configured: propagation is part of the API and works without an SDK,
            // so autoconfigure configured them, TextMapPropagatorCustomizer beans included, before checking
            // otel.sdk.disabled. It skipped every other component, tolerating configuration values the
            // enabled path rejects, so the full model is not built.
            // Autoconfigure also ran the ResourceProvider SPI before that check; the result is not used, but the
            // providers run and their AutoConfigureListener callbacks follow.
            QuarkusSpiResource.create(componentLoader, otelConfigProperties);
            // Upstream's disabled SDK has no propagators, so they are built here, the same way the enabled model's
            // customized propagators are
            UnaryOperator<TextMapPropagator> customizer = propagatorCustomizer(context, autoConfigurationCustomizers,
                    otelConfigProperties);
            QuarkusCustomizedPropagatorProvider propagatorProvider = new QuarkusCustomizedPropagatorProvider(
                    customizer == null ? UnaryOperator.identity() : customizer);
            List<TextMapPropagator> propagators = new ArrayList<>();
            for (String name : QuarkusOTelModelBuilder.propagatorNames(otelConfigProperties.getList("otel.propagators"))) {
                propagators.add(propagatorProvider.create(name, componentLoader));
            }
            OpenTelemetrySdk sdk = OpenTelemetrySdk.builder()
                    .setPropagators(ContextPropagators.create(TextMapPropagator.composite(propagators)))
                    .build();
            // As upstream calls the listeners of the components it loads, for the propagators resolved by name
            List<AutoConfigureListener> listeners = new ArrayList<>();
            for (ComponentProvider provider : componentLoader.load(ComponentProvider.class)) {
                if (provider instanceof AutoConfigureListener listener) {
                    listeners.add(listener);
                }
            }
            callAutoConfigureListeners(listeners, sdk);
            return sdk;
        }

        Resource merged = mergedResource(context, oTelRuntimeConfig, componentLoader, autoConfigurationCustomizers,
                otelConfigProperties);

        // Resolved before the model is built (the call creates no bean): quarkus.otel.simple keeps the batch span
        // processor when SpanProcessor beans exist, and the beans are added after the configured processors.
        Instance<SpanProcessor> spanProcessors = context.getInjectedReference(new TypeLiteral<>() {
        }, Any.Literal.INSTANCE);

        boolean devUiMetricReaderPresent = false;
        if (buildConfig.metrics().enabled().orElse(Boolean.TRUE)) {
            Instance<ScheduledExecutorService> managedExecutor = context
                    .getInjectedReference(new TypeLiteral<>() {
                    });
            // Each periodic metric reader gets its own view of this container-managed pool
            componentLoader.addComponentProvider(new QuarkusPeriodicMetricReaderProvider(
                    managedExecutor.isResolvable() ? managedExecutor.get() : null,
                    new UnaryOperator<MetricExporter>() {
                        @Override
                        public MetricExporter apply(MetricExporter exporter) {
                            return autoConfigurationCustomizers.customizeMetricExporter(exporter, otelConfigProperties);
                        }
                    }));
            // In dev mode the Dev UI contributes an in-memory reader. Only that bean is looked up:
            // MetricReader beans are not an application extension point.
            Instance<MetricReader> devUiMetricReader = context.getInjectedReference(new TypeLiteral<>() {
            }, Identifier.Literal.of(DEV_UI_METRIC_READER));
            if (devUiMetricReader.isResolvable()) {
                componentLoader.addComponentProvider(new QuarkusDevUiMetricReaderProvider(devUiMetricReader.get()));
                devUiMetricReaderPresent = true;
            }
        }

        // Only consulted when tracing is enabled: a CDI Sampler bean must not replace the always_off
        // sampler the model carries for disabled traces, matching the legacy SamplerCustomizer.
        // The same holds for the IdGenerator bean, as in the legacy TracerProviderCustomizer.
        boolean cdiSamplerPresent = false;
        boolean cdiIdGeneratorPresent = false;
        if (buildConfig.traces().enabled().orElse(Boolean.TRUE)) {
            Instance<Sampler> samplers = context.getInjectedReference(new TypeLiteral<>() {
            }, Any.Literal.INSTANCE);
            if (!samplers.isUnsatisfied()) {
                // the iteration order matches @All lists: beans sorted by priority
                componentLoader.addComponentProvider(new QuarkusCdiSamplerProvider(samplers.iterator().next()));
                cdiSamplerPresent = true;
            }
            Instance<IdGenerator> idGenerators = context.getInjectedReference(new TypeLiteral<>() {
            }, Any.Literal.INSTANCE);
            if (!idGenerators.isUnsatisfied()) {
                componentLoader.addComponentProvider(new QuarkusCdiIdGeneratorProvider(idGenerators.iterator().next()));
                cdiIdGeneratorPresent = true;
            }
            if (!spanProcessors.isUnsatisfied()) {
                // The beans are created when the provider runs, as the SDK is built
                componentLoader.addComponentProvider(new QuarkusCdiSpanProcessorProvider(spanProcessors));
            }
        }
        // Every span and log record exporter node is a composite that applies the SPI exporter customizers to each
        // exporter, as autoconfigure did
        componentLoader.addComponentProvider(new QuarkusCompositeSpanExporterProvider(new UnaryOperator<SpanExporter>() {
            @Override
            public SpanExporter apply(SpanExporter exporter) {
                return autoConfigurationCustomizers.customizeSpanExporter(exporter, otelConfigProperties);
            }
        }));
        componentLoader.addComponentProvider(
                new QuarkusCompositeLogRecordExporterProvider(new UnaryOperator<LogRecordExporter>() {
                    @Override
                    public LogRecordExporter apply(LogRecordExporter exporter) {
                        return autoConfigurationCustomizers.customizeLogRecordExporter(exporter, otelConfigProperties);
                    }
                }));
        // As the legacy SimpleLogRecordProcessorCustomizer: quarkus.otel.simple affects logs only when a
        // default-qualified LogRecordExporter bean is resolvable, and then uses that bean alone.
        boolean defaultLogRecordExporterPresent = false;
        if (buildConfig.simple()) {
            Instance<LogRecordExporter> logRecordExporters = context.getInjectedReference(new TypeLiteral<>() {
            });
            if (logRecordExporters.isResolvable()) {
                componentLoader.addComponentProvider(
                        new QuarkusSimpleLogRecordProcessorProvider(logRecordExporters.get()));
                defaultLogRecordExporterPresent = true;
            }
        }
        UnaryOperator<TextMapPropagator> propagatorCustomizer = propagatorCustomizer(context,
                autoConfigurationCustomizers, otelConfigProperties);
        if (propagatorCustomizer != null) {
            componentLoader.addComponentProvider(new QuarkusCustomizedPropagatorProvider(propagatorCustomizer));
        }
        if (autoConfigurationCustomizers.customizesSampler()) {
            componentLoader.addComponentProvider(new QuarkusCustomizedSamplerProvider(new UnaryOperator<Sampler>() {
                @Override
                public Sampler apply(Sampler sampler) {
                    return autoConfigurationCustomizers.customizeSampler(sampler, otelConfigProperties);
                }
            }));
        }

        OpenTelemetryConfigurationModel model = QuarkusOTelModelBuilder
                .forConfig(buildConfig, oTelRuntimeConfig, otelConfigProperties)
                .resource(merged)
                .nonApplicationUriTargets(TracerRecorder.dropNonApplicationUriTargets)
                .staticResourceTargets(TracerRecorder.dropStaticResourceTargets)
                .cdiSpanProcessorsPresent(!spanProcessors.isUnsatisfied())
                .devUiMetricReaderPresent(devUiMetricReaderPresent)
                .cdiSamplerPresent(cdiSamplerPresent)
                .samplerCustomized(autoConfigurationCustomizers.customizesSampler())
                .cdiIdGeneratorPresent(cdiIdGeneratorPresent)
                .cdiSpanExportersPresent(cdiSpanExportersPresent())
                .defaultLogRecordExporterPresent(defaultLogRecordExporterPresent)
                .propagatorsCustomized(propagatorCustomizer != null)
                .build();

        // The SDK uses the merged Resource as-is, as the legacy ResourceCustomizer did; the model carries its
        // attributes and schema URL but cannot represent every attribute type or the resource's entities.
        return QuarkusDeclarativeConfiguration.create(model, componentLoader,
                internalTelemetryVersion(otelConfigProperties), merged);
    }

    // Same property as autoconfigure's InternalTelemetryConfiguration. select() sends a missing value and legacy to the
    // legacy engine.
    private static InternalTelemetryVersion internalTelemetryVersion(ConfigProperties otelConfigProperties) {
        String value = otelConfigProperties.getString("otel.experimental.sdk.telemetry.version");
        if (!"latest".equalsIgnoreCase(value)) {
            throw new DeclarativeConfigException("Invalid sdk telemetry version: " + value);
        }
        return InternalTelemetryVersion.LATEST;
    }

    // The TextMapPropagatorCustomizer beans in priority order, then the SPI propagator customizers, as autoconfigure
    // ran the SPI providers after the Quarkus customizer beans had registered. Null when there are neither.
    private static UnaryOperator<TextMapPropagator> propagatorCustomizer(SyntheticCreationalContext<OpenTelemetry> context,
            AutoConfigurationCustomizers autoConfigurationCustomizers, ConfigProperties otelConfigProperties) {
        Instance<TextMapPropagatorCustomizer> propagatorCustomizerInstances = context
                .getInjectedReference(new TypeLiteral<>() {
                }, Any.Literal.INSTANCE);
        if (propagatorCustomizerInstances.isUnsatisfied() && !autoConfigurationCustomizers.customizesPropagators()) {
            return null;
        }
        // materialized once: the operator runs for every propagator
        List<TextMapPropagatorCustomizer> propagatorCustomizers = propagatorCustomizerInstances.stream()
                .toList();
        return new UnaryOperator<TextMapPropagator>() {
            @Override
            public TextMapPropagator apply(TextMapPropagator propagator) {
                TextMapPropagator result = propagator;
                for (TextMapPropagatorCustomizer customizer : propagatorCustomizers) {
                    final TextMapPropagator current = result;
                    result = customizer.customize(new TextMapPropagatorCustomizer.Context() {
                        @Override
                        public TextMapPropagator propagator() {
                            return current;
                        }

                        @Override
                        public ConfigProperties otelConfigProperties() {
                            return otelConfigProperties;
                        }
                    });
                }
                return autoConfigurationCustomizers.customizePropagator(result, otelConfigProperties);
            }
        };
    }

    // The SDK resource: the resource SPI result, then the Quarkus-managed layers, then the SPI resource customizers.
    private static Resource mergedResource(SyntheticCreationalContext<OpenTelemetry> context,
            OTelRuntimeConfig oTelRuntimeConfig, ComponentLoader componentLoader,
            AutoConfigurationCustomizers autoConfigurationCustomizers, ConfigProperties otelConfigProperties) {
        Instance<DelayedAttributes> delayed = context.getInjectedReference(new TypeLiteral<>() {
        }, Any.Literal.INSTANCE);
        Instance<Resource> resources = context.getInjectedReference(new TypeLiteral<>() {
        }, Any.Literal.INSTANCE);
        ApplicationConfig appConfig = context.getInjectedReference(ApplicationConfig.class);

        String serviceName = oTelRuntimeConfig
                .serviceName()
                .filter(new Predicate<String>() {
                    @Override
                    public boolean test(String sn) {
                        return !sn.equals(appConfig.name().orElse("unset"));
                    }
                })
                .orElse(null);
        // SPI-provided resources first: the Quarkus-managed layers merged on top win conflicting keys,
        // matching the legacy ordering where the ResourceCustomizer merged onto autoconfigure's result.
        // The SPI resource customizers run last, over the fully merged resource, as autoconfigure ran them
        // over its final resource.
        return autoConfigurationCustomizers.customizeResource(
                QuarkusSpiResource.create(componentLoader, otelConfigProperties)
                        .merge(Resource.create(delayed.get()))
                        .merge(resources.stream().reduce(Resource.empty(), Resource::merge))
                        .merge(TracerUtil.mapResourceAttributes(
                                oTelRuntimeConfig.resourceAttributes().orElse(List.of()),
                                serviceName, HostName.getHostName())),
                otelConfigProperties);
    }

    // The lookup SpanExporterCDIProvider makes for the cdi exporter. It creates no bean.
    private static boolean cdiSpanExportersPresent() {
        return !CDI.current().select(SpanExporter.class, Any.Literal.INSTANCE).isUnsatisfied();
    }

    private static void callAutoConfigureListeners(List<AutoConfigureListener> listeners, OpenTelemetrySdk sdk) {
        for (AutoConfigureListener listener : listeners) {
            try {
                listener.afterAutoConfigure(sdk);
            } catch (Throwable t) {
                LOG.warnf(t, "Error invoking listener %s", listener.getClass().getName());
            }
        }
    }

    // sources: the class names or configuration properties responsible, for the warning
    private static Engine legacy(String reason, Collection<String> sources) {
        LOG.warnf("%s: %s. The OpenTelemetry SDK is initialized with the legacy autoconfigure engine instead of the "
                + "declarative configuration model.", reason, String.join(", ", sources));
        return Engine.LEGACY;
    }

    // Replaces the legacy builder's setResultAsGlobal(): instrumentation reading
    // GlobalOpenTelemetry must see this SDK.
    private static OpenTelemetry register(OpenTelemetrySdk sdk) {
        GlobalOpenTelemetry.set(sdk);
        return sdk;
    }

    /**
     * The engine {@link #select} chose. The legacy engine is initialized by the caller, from the same inputs it
     * passed to {@link #select}.
     */
    public static final class Engine {

        private static final Engine LEGACY = new Engine(null, null, null, null, null, null);

        private final SyntheticCreationalContext<OpenTelemetry> context;
        private final OTelBuildConfig buildConfig;
        private final OTelRuntimeConfig runtimeConfig;
        private final QuarkusComponentLoader componentLoader;
        private final AutoConfigurationCustomizers autoConfigurationCustomizers;
        private final ConfigProperties otelConfigProperties;

        private Engine(SyntheticCreationalContext<OpenTelemetry> context, OTelBuildConfig buildConfig,
                OTelRuntimeConfig runtimeConfig, QuarkusComponentLoader componentLoader,
                AutoConfigurationCustomizers autoConfigurationCustomizers, ConfigProperties otelConfigProperties) {
            this.context = context;
            this.buildConfig = buildConfig;
            this.runtimeConfig = runtimeConfig;
            this.componentLoader = componentLoader;
            this.autoConfigurationCustomizers = autoConfigurationCustomizers;
            this.otelConfigProperties = otelConfigProperties;
        }

        /**
         * Returns whether the declarative configuration engine was selected.
         *
         * @return {@code true} for the declarative engine, {@code false} for the legacy engine
         */
        public boolean declarative() {
            return context != null;
        }

        /**
         * Creates the SDK and sets it as the {@link GlobalOpenTelemetry} instance.
         *
         * @return the SDK
         * @throws IllegalStateException if the legacy engine was selected
         */
        public OpenTelemetry create() {
            if (!declarative()) {
                throw new IllegalStateException("The legacy engine was selected");
            }
            OpenTelemetrySdk sdk = QuarkusDeclarativeSdk.create(context, buildConfig, runtimeConfig, componentLoader,
                    autoConfigurationCustomizers, otelConfigProperties);
            // The SPI instances loaded outside the declarative configuration, called as autoconfigure calls them
            // before setting the global instance
            callAutoConfigureListeners(componentLoader.listeners(), sdk);
            callAutoConfigureListeners(autoConfigurationCustomizers.listeners(), sdk);
            return register(sdk);
        }
    }
}
