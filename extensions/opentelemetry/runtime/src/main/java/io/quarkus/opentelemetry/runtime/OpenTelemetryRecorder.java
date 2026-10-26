package io.quarkus.opentelemetry.runtime;

import static io.opentelemetry.semconv.ServiceAttributes.SERVICE_NAME;
import static io.opentelemetry.semconv.ServiceAttributes.SERVICE_VERSION;
import static io.opentelemetry.semconv.incubating.WebengineIncubatingAttributes.WEBENGINE_NAME;
import static io.opentelemetry.semconv.incubating.WebengineIncubatingAttributes.WEBENGINE_VERSION;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.regex.Pattern;

import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.util.TypeLiteral;

import org.eclipse.microprofile.config.spi.Converter;
import org.jboss.logging.Logger;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.common.ComponentLoader;
import io.opentelemetry.context.ContextStorage;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.OpenTelemetryConfigurationModel;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.common.Clock;
import io.opentelemetry.sdk.logs.export.LogRecordExporter;
import io.opentelemetry.sdk.metrics.export.MetricReader;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.IdGenerator;
import io.opentelemetry.sdk.trace.SpanProcessor;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import io.quarkus.arc.SyntheticCreationalContext;
import io.quarkus.opentelemetry.runtime.config.build.OTelBuildConfig;
import io.quarkus.opentelemetry.runtime.config.runtime.OTelRuntimeConfig;
import io.quarkus.opentelemetry.runtime.config.runtime.exporter.OtlpExporterRuntimeConfig;
import io.quarkus.opentelemetry.runtime.declarativeconfig.AutoConfigurationCustomizers;
import io.quarkus.opentelemetry.runtime.declarativeconfig.QuarkusDeclarativeConfiguration;
import io.quarkus.opentelemetry.runtime.declarativeconfig.QuarkusDeclarativeConfiguration.Customizations;
import io.quarkus.opentelemetry.runtime.declarativeconfig.QuarkusOTelModelBuilder;
import io.quarkus.opentelemetry.runtime.propagation.TextMapPropagatorCustomizer;
import io.quarkus.opentelemetry.runtime.resource.KubernetesResourceProvider;
import io.quarkus.opentelemetry.runtime.tracing.TracerRecorder;
import io.quarkus.opentelemetry.runtime.tracing.TracerUtil;
import io.quarkus.runtime.ApplicationConfig;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;
import io.quarkus.runtime.annotations.RuntimeInit;
import io.quarkus.runtime.annotations.StaticInit;
import io.quarkus.runtime.configuration.DurationConverter;
import io.smallrye.common.net.HostName;
import io.smallrye.config.Config;
import io.smallrye.config.ConfigValue;
import io.vertx.core.Vertx;

@Recorder
public class OpenTelemetryRecorder {
    public static final String OPEN_TELEMETRY_DRIVER = "io.opentelemetry.instrumentation.jdbc.OpenTelemetryDriver";
    private static final Logger LOG = Logger.getLogger(OpenTelemetryRecorder.class);
    private final OTelBuildConfig buildConfig;
    private final RuntimeValue<OTelRuntimeConfig> runtimeConfig;
    private final RuntimeValue<OtlpExporterRuntimeConfig> exporterRuntimeConfig;

    public OpenTelemetryRecorder(final OTelBuildConfig buildConfig,
            final RuntimeValue<OTelRuntimeConfig> runtimeConfig,
            final RuntimeValue<OtlpExporterRuntimeConfig> exporterRuntimeConfig) {
        this.buildConfig = buildConfig;
        this.runtimeConfig = runtimeConfig;
        this.exporterRuntimeConfig = exporterRuntimeConfig;
    }

    @StaticInit
    public Supplier<DelayedAttributes> delayedAttributes(String quarkusVersion,
            String serviceName,
            String serviceVersion) {
        return new Supplier<>() {
            @Override
            public DelayedAttributes get() {
                var result = new DelayedAttributes();
                result.setAttributesDelegate(Resource.getDefault()
                        .merge(Resource.create(
                                Attributes.of(
                                        SERVICE_NAME, serviceName,
                                        SERVICE_VERSION, serviceVersion,
                                        WEBENGINE_NAME, "Quarkus",
                                        WEBENGINE_VERSION, quarkusVersion)))
                        .getAttributes());
                return result;
            }
        };
    }

    @RuntimeInit
    public Supplier<Resource> kubernetesResource() {
        return new Supplier<>() {
            @Override
            public Resource get() {
                return KubernetesResourceProvider.createResource();
            }
        };
    }

    @RuntimeInit
    public void resetGlobalOpenTelemetryForDevMode() {
        GlobalOpenTelemetry.resetForTest();
    }

    @RuntimeInit
    public RuntimeValue<Boolean> isOtelSdkEnabled() {
        return new RuntimeValue<>(!runtimeConfig.getValue().sdkDisabled());
    }

    @RuntimeInit
    public void eagerlyCreateContextStorage() {
        ContextStorage.get();
    }

    @RuntimeInit
    public void storeVertxOnContextStorage(Supplier<Vertx> vertx) {
        QuarkusContextStorage.vertx = vertx.get();
    }

    @RuntimeInit
    public Function<SyntheticCreationalContext<OpenTelemetry>, OpenTelemetry> opentelemetryBean() {
        return new Function<>() {
            @SuppressWarnings("removal")
            @Override
            public OpenTelemetry apply(SyntheticCreationalContext<OpenTelemetry> context) {
                // Any resolvable bean of this type is user-provided (the built-in customizers are @Typed to
                // InternalSdkBuilderCustomizer) and engages the legacy engine. isUnsatisfied() creates no bean.
                Instance<AutoConfiguredOpenTelemetrySdkBuilderCustomizer> userCustomizers = context
                        .getInjectedReference(new TypeLiteral<>() {
                        });
                if (!userCustomizers.isUnsatisfied()) {
                    return legacyAutoConfiguredSdk(context, userCustomizers);
                }
                final OTelRuntimeConfig oTelRuntimeConfig = runtimeConfig.getValue();
                final ComponentLoader componentLoader = ComponentLoader
                        .forClassLoader(Thread.currentThread().getContextClassLoader());

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
                        .resolveConfigProperties(getOtelConfigs(), componentLoader);

                if (otelConfigProperties.getBoolean("otel.sdk.disabled", false)) {
                    // The model is not built: autoconfigure skipped all component configuration when the SDK was
                    // disabled, tolerating configuration values the enabled path rejects, and the model builder
                    // validates them. No injected bean is resolved either.
                    return register(OpenTelemetrySdk.builder().build());
                }

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
                Resource merged = autoConfigurationCustomizers.customizeResource(
                        QuarkusDeclarativeConfiguration.createSpiResource(componentLoader, otelConfigProperties)
                                .merge(Resource.create(delayed.get()))
                                .merge(resources.stream().reduce(Resource.empty(), Resource::merge))
                                .merge(TracerUtil.mapResourceAttributes(
                                        oTelRuntimeConfig.resourceAttributes().orElse(List.of()),
                                        serviceName, HostName.getHostName())),
                        otelConfigProperties);

                // Resolved before the model is built, for presence only (neither call creates the beans):
                // quarkus.otel.simple keeps the batch span processor when SpanProcessor beans exist, and only
                // affects the log pipeline when a default-qualified LogRecordExporter bean is resolvable.
                Instance<SpanProcessor> spanProcessors = context.getInjectedReference(new TypeLiteral<>() {
                }, Any.Literal.INSTANCE);
                Instance<LogRecordExporter> logRecordExporters = context.getInjectedReference(new TypeLiteral<>() {
                });

                OpenTelemetryConfigurationModel model = QuarkusOTelModelBuilder
                        .forConfig(buildConfig, oTelRuntimeConfig, exporterRuntimeConfig.getValue(), otelConfigProperties)
                        .resourceAttributes(merged.getAttributes())
                        .nonApplicationUriTargets(TracerRecorder.dropNonApplicationUriTargets)
                        .staticResourceTargets(TracerRecorder.dropStaticResourceTargets)
                        .cdiSpanProcessorsPresent(!spanProcessors.isUnsatisfied())
                        .cdiLogRecordExporterResolvable(logRecordExporters.isResolvable())
                        .build();

                Customizations.Builder customizations = Customizations.builder(componentLoader)
                        .otelConfigProperties(otelConfigProperties)
                        .autoConfigurationCustomizers(autoConfigurationCustomizers);

                // Only consulted when tracing is enabled: a CDI Sampler bean must not replace the always_off
                // sampler the model carries for disabled traces, matching the legacy SamplerCustomizer.
                if (buildConfig.traces().enabled().orElse(Boolean.TRUE)) {
                    Instance<Sampler> samplers = context.getInjectedReference(new TypeLiteral<>() {
                    }, Any.Literal.INSTANCE);
                    if (!samplers.isUnsatisfied()) {
                        // the iteration order matches @All lists: beans sorted by priority
                        customizations.sampler(samplers.iterator().next());
                    }
                    Instance<IdGenerator> idGenerators = context.getInjectedReference(new TypeLiteral<>() {
                    }, Any.Literal.INSTANCE);
                    if (!idGenerators.isUnsatisfied()) {
                        customizations.idGenerator(idGenerators.iterator().next());
                    }
                    for (SpanProcessor spanProcessor : spanProcessors) {
                        customizations.addSpanProcessor(spanProcessor);
                    }
                }

                if (buildConfig.metrics().enabled().orElse(Boolean.TRUE)) {
                    Instance<Clock> clock = context.getInjectedReference(new TypeLiteral<>() {
                    });
                    if (clock.isUnsatisfied()) {
                        throw new IllegalStateException("No Clock bean found");
                    }
                    customizations.clock(clock.get());
                    Instance<ScheduledExecutorService> managedExecutor = context
                            .getInjectedReference(new TypeLiteral<>() {
                            });
                    if (managedExecutor.isResolvable()) {
                        // Each periodic metric reader gets its own view of this container-managed pool; the
                        // interpreter builds the views.
                        customizations.metricReaderExecutor(managedExecutor.get());
                    }
                    // In dev mode the Dev UI contributes an in-memory reader; applications may contribute
                    // their own as well.
                    Instance<MetricReader> metricReaders = context.getInjectedReference(new TypeLiteral<>() {
                    }, Any.Literal.INSTANCE);
                    for (MetricReader metricReader : metricReaders) {
                        customizations.addMetricReader(metricReader);
                    }
                }

                Instance<TextMapPropagatorCustomizer> propagatorCustomizerInstances = context
                        .getInjectedReference(new TypeLiteral<>() {
                        }, Any.Literal.INSTANCE);
                if (!propagatorCustomizerInstances.isUnsatisfied()) {
                    // materialized once: the operator runs for every propagator
                    List<TextMapPropagatorCustomizer> propagatorCustomizers = propagatorCustomizerInstances.stream()
                            .toList();
                    customizations.propagatorCustomizer(new UnaryOperator<TextMapPropagator>() {
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
                            return result;
                        }
                    });
                }

                return register(QuarkusDeclarativeConfiguration.create(model, customizations.build()));
            }

            // Replaces the legacy builder's setResultAsGlobal(): instrumentation reading
            // GlobalOpenTelemetry must see this SDK.
            private OpenTelemetry register(OpenTelemetrySdk sdk) {
                GlobalOpenTelemetry.set(sdk);
                return sdk;
            }

            // The legacy engine, kept for applications that still have AutoConfiguredOpenTelemetrySdkBuilderCustomizer
            // implementations: their customizers run against the real AutoConfiguredOpenTelemetrySdk builder with the
            // same built-in customizer beans as before the declarative-configuration path.
            // Deleted together with the deprecated SPI in Quarkus 4.3.
            @SuppressWarnings("removal")
            private OpenTelemetry legacyAutoConfiguredSdk(SyntheticCreationalContext<OpenTelemetry> context,
                    Instance<AutoConfiguredOpenTelemetrySdkBuilderCustomizer> userCustomizers) {
                LOG.info("Deprecated AutoConfiguredOpenTelemetrySdkBuilderCustomizer implementations are present; " +
                        "initializing the OpenTelemetry SDK with the legacy autoconfigure engine instead of the " +
                        "declarative configuration model");

                final Map<String, String> oTelConfigs = getOtelConfigs();
                // Instructs OTel that the AutoConfiguredOpenTelemetrySdk is in use; only this engine wants it.
                oTelConfigs.put("otel.java.global-autoconfigure.enabled", "true");
                OtelConfigsSupplier propertiesSupplier = new OtelConfigsSupplier(oTelConfigs);
                if (runtimeConfig.getValue().sdkDisabled()) {
                    return AutoConfiguredOpenTelemetrySdk.builder()
                            .setResultAsGlobal()
                            .disableShutdownHook()
                            .addPropertiesSupplier(propertiesSupplier)
                            .build()
                            .getOpenTelemetrySdk();
                }

                var builder = AutoConfiguredOpenTelemetrySdk.builder()
                        .setResultAsGlobal()
                        .disableShutdownHook()
                        .addPropertiesSupplier(propertiesSupplier)
                        .setServiceClassLoader(Thread.currentThread().getContextClassLoader());
                // The built-in Quarkus wiring registers first so a user customizer's functions compose over it; on
                // the single mixed resolution used before the @Typed split, the relative order among the equal
                // default bean priorities was unspecified.
                Instance<InternalSdkBuilderCustomizer> builtInCustomizers = context
                        .getInjectedReference(new TypeLiteral<>() {
                        });
                for (var customizer : builtInCustomizers) {
                    customizer.customize(builder);
                }
                for (var customizer : userCustomizers) {
                    customizer.customize(builder);
                }

                return builder.build().getOpenTelemetrySdk();
            }

            private Map<String, String> getOtelConfigs() {
                Map<String, String> oTelConfigs = new HashMap<>();
                Config config = Config.get();

                Map<String, String> otel = new HashMap<>();
                Map<String, String> quarkus = new HashMap<>();
                // load new properties
                for (String propertyName : config.getPropertyNames()) {
                    if (propertyName.startsWith("quarkus.otel.")) {
                        ConfigValue value = config.getConfigValue(propertyName);
                        if (value.getValue() != null) {
                            if (propertyName.endsWith("timeout") || propertyName.endsWith("delay")) {
                                quarkus.put(propertyName.substring(8),
                                        OTelDurationConverter.INSTANCE.convert(value.getValue()));
                            } else {
                                quarkus.put(propertyName.substring(8), value.getValue());
                            }
                        } else if (value.getValue() == null && value.getRawValue() != null) {
                            config.getValue(propertyName, String.class);
                        }
                    } else if (propertyName.startsWith("otel.")) {
                        ConfigValue value = config.getConfigValue(propertyName);
                        if (value.getValue() != null) {
                            otel.put(propertyName, value.getValue());
                        }
                    }
                }

                if (runtimeConfig.getValue().mpCompatibility()) {
                    oTelConfigs.putAll(quarkus);
                    oTelConfigs.putAll(otel);
                } else {
                    oTelConfigs.putAll(otel);
                    oTelConfigs.putAll(quarkus);
                }

                return oTelConfigs;
            }
        };
    }

    /**
     * Transforms the value to what OTel expects
     * TODO: this is super simplistic, and should be more modular if needed
     */
    private static class OTelDurationConverter implements Converter<String> {
        static OTelDurationConverter INSTANCE = new OTelDurationConverter();

        private static final Pattern DIGITS = Pattern.compile("^[-+]?\\d+$");

        @Override
        public String convert(final String value) throws IllegalArgumentException, NullPointerException {
            if (value == null) {
                throw new NullPointerException();
            }

            if (DIGITS.asPredicate().test(value)) {
                // OTel regards values without a unit to be milliseconds instead of seconds
                // that java.time.Duration assumes, so let's just not do any conversion and let OTel handle with it
                return value;
            }
            Duration duration;
            try {
                duration = DurationConverter.parseDuration(value);
            } catch (Exception ignored) {
                // it's not a Duration, so we can't do much
                return value;
            }

            if (duration == null) {
                return value;
            }

            try {
                return String.valueOf(duration.toMillis()).concat("ms");
            } catch (Exception ignored) {
                return String.valueOf(duration.toSeconds()).concat("s");
            }
        }
    }

    private static class OtelConfigsSupplier implements Supplier<Map<String, String>> {
        private final Map<String, String> oTelConfigs;

        public OtelConfigsSupplier(Map<String, String> oTelConfigs) {
            this.oTelConfigs = oTelConfigs;
        }

        @Override
        public Map<String, String> get() {
            return oTelConfigs;
        }
    }
}
