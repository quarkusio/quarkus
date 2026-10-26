package io.quarkus.opentelemetry.runtime.declarativeconfig;

import java.io.Closeable;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jboss.logging.Logger;

import io.opentelemetry.api.baggage.propagation.W3CBaggagePropagator;
import io.opentelemetry.api.incubator.config.ConfigProvider;
import io.opentelemetry.api.incubator.config.DeclarativeConfigException;
import io.opentelemetry.api.incubator.config.DeclarativeConfigProperties;
import io.opentelemetry.api.metrics.MeterProvider;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.common.ComponentLoader;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.OpenTelemetrySdkBuilder;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.YamlDeclarativeConfigProperties;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.AttributeLimitsModel;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.BatchLogRecordProcessorModel;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.BatchSpanProcessorModel;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.CardinalityLimitsModel;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.IdGeneratorModel;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.LogRecordExporterModel;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.LogRecordLimitsModel;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.LogRecordProcessorModel;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.LoggerProviderModel;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.MeterProviderModel;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.MetricReaderModel;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.OpenTelemetryConfigurationModel;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.ParentBasedSamplerModel;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.PropagatorModel;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.PullMetricExporterModel;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.PullMetricReaderModel;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.SamplerModel;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.SpanExporterModel;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.SpanLimitsModel;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.SpanProcessorModel;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.TextMapPropagatorModel;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.TracerProviderModel;
import io.opentelemetry.sdk.autoconfigure.spi.internal.AutoConfigureListener;
import io.opentelemetry.sdk.autoconfigure.spi.internal.ComponentProvider;
import io.opentelemetry.sdk.common.InternalTelemetryVersion;
import io.opentelemetry.sdk.logs.LogLimits;
import io.opentelemetry.sdk.logs.LogRecordProcessor;
import io.opentelemetry.sdk.logs.SdkLoggerProvider;
import io.opentelemetry.sdk.logs.SdkLoggerProviderBuilder;
import io.opentelemetry.sdk.logs.export.BatchLogRecordProcessor;
import io.opentelemetry.sdk.logs.export.BatchLogRecordProcessorBuilder;
import io.opentelemetry.sdk.logs.export.LogRecordExporter;
import io.opentelemetry.sdk.logs.export.SimpleLogRecordProcessor;
import io.opentelemetry.sdk.logs.export.SimpleLogRecordProcessorBuilder;
import io.opentelemetry.sdk.metrics.ExemplarFilter;
import io.opentelemetry.sdk.metrics.InstrumentType;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.metrics.SdkMeterProviderBuilder;
import io.opentelemetry.sdk.metrics.export.CardinalityLimitSelector;
import io.opentelemetry.sdk.metrics.export.MetricReader;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.IdGenerator;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.SdkTracerProviderBuilder;
import io.opentelemetry.sdk.trace.SpanLimits;
import io.opentelemetry.sdk.trace.SpanLimitsBuilder;
import io.opentelemetry.sdk.trace.SpanProcessor;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessorBuilder;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessorBuilder;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import io.opentelemetry.sdk.trace.samplers.ParentBasedSamplerBuilder;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import io.quarkus.opentelemetry.runtime.declarativeconfig.spi.QuarkusExtendedConfigProperties;

/**
 * Builds an {@link OpenTelemetrySdk} from an {@link OpenTelemetryConfigurationModel}. It stands in for upstream's
 * {@code DeclarativeConfiguration.create(model, componentLoader)} and {@code DeclarativeConfiguration.createSampler},
 * which 1.67 cannot run without Jackson 2 and SnakeYAML (see the dependency comment in this module's {@code pom.xml}),
 * until an OpenTelemetry release contains opentelemetry-java#8983, which makes the mapper and YAML parser pluggable.
 * <p>
 * It reproduces upstream's behavior for the model shapes {@link QuarkusOTelModelBuilder} emits, and nothing more.
 * Extension nodes are resolved as upstream resolves them: by name, through the {@link ComponentProvider}s of the
 * component loader. Quarkus behavior lives in those providers and in the model builder, never here.
 * </p>
 * <p>
 * The swap to upstream takes these steps:
 * </p>
 * <ol>
 * <li>Add a Quarkus {@code DeclarativeConfigurationMapper} and {@code DeclarativeConfigurationYamlParser}, registered
 * for native.</li>
 * <li>Replace the call to {@link #create} with {@code DeclarativeConfiguration.create(model, componentLoader)}, and the
 * calls to {@link #createSampler(DeclarativeConfigProperties)} with {@code DeclarativeConfiguration.createSampler}.</li>
 * <li>Delete this class and its tests.</li>
 * <li>Remove the Jackson 2 and SnakeYAML exclusions from this module's {@code pom.xml}, as far as #8983 allows.</li>
 * <li>Emit the internal telemetry version in the model, under {@code instrumentation/development}.</li>
 * <li>Emit the merged resource in the model, through a {@code quarkus} resource detector under
 * {@code resource.detection/development}.</li>
 * </ol>
 * <p>
 * The last two replace the {@code internalTelemetryVersion} and {@code resource} parameters of {@link #create}: in
 * 1.67 the model cannot hold those experimental keys without Jackson 2.
 * </p>
 */
public final class QuarkusDeclarativeConfiguration {

    private static final Logger LOG = Logger.getLogger(QuarkusDeclarativeConfiguration.class);

    private QuarkusDeclarativeConfiguration() {
    }

    /**
     * Builds the SDK from the model, as upstream's {@code DeclarativeConfiguration.create(model, componentLoader)}
     * does. A model with {@code disabled: true} produces an SDK without components. When construction fails midway,
     * every component already built is closed before the exception propagates, so no worker threads outlive the
     * failure.
     * <p>
     * Once the SDK is built, each {@link ComponentProvider} of the component loader that implements
     * {@link AutoConfigureListener} is called with it, as upstream does. A listener that throws is logged and does
     * not stop the others.
     * </p>
     *
     * @param model the model, not {@code null}
     * @param componentLoader resolves the extension nodes, not {@code null}
     * @param internalTelemetryVersion the version of the SDK's own telemetry, which upstream reads from the model's
     *        {@code instrumentation/development} node, not {@code null}
     * @param resource the resource of every provider, used as-is instead of one built from the model's
     *        {@code resource} node, not {@code null}
     * @return the SDK
     * @throws DeclarativeConfigException if the model names a component no provider creates, or is malformed
     */
    public static OpenTelemetrySdk create(OpenTelemetryConfigurationModel model, ComponentLoader componentLoader,
            InternalTelemetryVersion internalTelemetryVersion, Resource resource) {
        OpenTelemetrySdk sdk = createSdk(model, componentLoader, internalTelemetryVersion, resource);
        callAutoConfigureListeners(componentLoader, sdk);
        return sdk;
    }

    private static OpenTelemetrySdk createSdk(OpenTelemetryConfigurationModel model, ComponentLoader componentLoader,
            InternalTelemetryVersion internalTelemetryVersion, Resource resource) {
        if (Boolean.TRUE.equals(model.getDisabled())) {
            // As upstream: a disabled SDK has no components, propagators included
            return OpenTelemetrySdk.builder().build();
        }
        List<Closeable> closeables = new ArrayList<>();
        try {
            OpenTelemetrySdkBuilder builder = OpenTelemetrySdk.builder();
            if (model.getPropagator() != null) {
                builder.setPropagators(createPropagators(model.getPropagator(), componentLoader));
            }
            // The meter provider is built before the tracer and logger providers so they can report the SDK's
            // self-telemetry through it, matching autoconfigure's ordering.
            MeterProvider meterProvider = MeterProvider.noop();
            if (model.getMeterProvider() != null) {
                SdkMeterProvider sdkMeterProvider = createMeterProvider(model.getMeterProvider(), resource,
                        componentLoader, closeables).build();
                closeables.add(sdkMeterProvider);
                builder.setMeterProvider(sdkMeterProvider);
                meterProvider = sdkMeterProvider;
            }
            SelfTelemetry selfTelemetry = new SelfTelemetry(internalTelemetryVersion, meterProvider);
            if (model.getTracerProvider() != null) {
                SdkTracerProvider tracerProvider = createTracerProvider(model.getTracerProvider(),
                        model.getAttributeLimits(), resource, componentLoader, selfTelemetry, closeables).build();
                closeables.add(tracerProvider);
                builder.setTracerProvider(tracerProvider);
            }
            if (model.getLoggerProvider() != null) {
                SdkLoggerProvider loggerProvider = createLoggerProvider(model.getLoggerProvider(),
                        model.getAttributeLimits(), resource, componentLoader, selfTelemetry, closeables).build();
                closeables.add(loggerProvider);
                builder.setLoggerProvider(loggerProvider);
            }
            return builder.build();
        } catch (RuntimeException e) {
            for (int i = closeables.size() - 1; i >= 0; i--) {
                try {
                    closeables.get(i).close();
                } catch (IOException | RuntimeException closeFailure) {
                    e.addSuppressed(closeFailure);
                }
            }
            throw e;
        }
    }

    /**
     * Builds a {@link Sampler} from a declarative-config properties node, for use by a {@link ComponentProvider}
     * (which receives {@link DeclarativeConfigProperties} rather than the typed model). Only the sampler shapes
     * {@link QuarkusOTelModelBuilder} emits are recognized; any other single-key node is resolved as an extension
     * sampler by name.
     */
    public static Sampler createSampler(DeclarativeConfigProperties config) {
        ComponentLoader componentLoader = config.getComponentLoader();
        if (config.getStructured("always_on") != null) {
            return Sampler.alwaysOn();
        }
        if (config.getStructured("always_off") != null) {
            return Sampler.alwaysOff();
        }
        DeclarativeConfigProperties ratioBased = config.getStructured("trace_id_ratio_based");
        if (ratioBased != null) {
            return Sampler.traceIdRatioBased(ratioBased.getDouble("ratio", 1.0d));
        }
        DeclarativeConfigProperties parentBased = config.getStructured("parent_based");
        if (parentBased != null) {
            DeclarativeConfigProperties root = parentBased.getStructured("root");
            ParentBasedSamplerBuilder builder = Sampler
                    .parentBasedBuilder(root == null ? Sampler.alwaysOn() : createSampler(root));
            DeclarativeConfigProperties remoteParentSampled = parentBased.getStructured("remote_parent_sampled");
            if (remoteParentSampled != null) {
                builder.setRemoteParentSampled(createSampler(remoteParentSampled));
            }
            DeclarativeConfigProperties remoteParentNotSampled = parentBased.getStructured("remote_parent_not_sampled");
            if (remoteParentNotSampled != null) {
                builder.setRemoteParentNotSampled(createSampler(remoteParentNotSampled));
            }
            DeclarativeConfigProperties localParentSampled = parentBased.getStructured("local_parent_sampled");
            if (localParentSampled != null) {
                builder.setLocalParentSampled(createSampler(localParentSampled));
            }
            DeclarativeConfigProperties localParentNotSampled = parentBased.getStructured("local_parent_not_sampled");
            if (localParentNotSampled != null) {
                builder.setLocalParentNotSampled(createSampler(localParentNotSampled));
            }
            return builder.build();
        }
        Set<String> keys = config.getPropertyKeys();
        if (keys.size() > 1) {
            throw new DeclarativeConfigException("The sampler node must define exactly one sampler, found: " + keys);
        }
        if (!keys.isEmpty()) {
            String name = keys.iterator().next();
            DeclarativeConfigProperties node = config.getStructured(name,
                    YamlDeclarativeConfigProperties.create(Map.of(), componentLoader));
            Sampler sampler = loadComponent(Sampler.class, name, node, componentLoader);
            if (sampler != null) {
                return sampler;
            }
            throw noSuchComponent(Sampler.class, name);
        }
        throw new DeclarativeConfigException("The sampler node defines no sampler");
    }

    // Builds a sampler from a typed sampler model through the properties factory; package-private for the tests
    static Sampler createSampler(SamplerModel model, ComponentLoader componentLoader) {
        return createSampler(YamlDeclarativeConfigProperties.create(toConfigMap(model), componentLoader));
    }

    // Mirrors upstream's PropagatorFactory: the composite entries, then the composite_list names not seen yet,
    // without empty names and "none"
    private static ContextPropagators createPropagators(PropagatorModel model, ComponentLoader componentLoader) {
        List<TextMapPropagator> propagators = new ArrayList<>();
        Set<String> names = new HashSet<>();
        if (model.getComposite() != null) {
            for (TextMapPropagatorModel propagator : model.getComposite()) {
                if (propagator.getTracecontext() != null) {
                    names.add("tracecontext");
                    propagators.add(W3CTraceContextPropagator.getInstance());
                } else if (propagator.getBaggage() != null) {
                    names.add("baggage");
                    propagators.add(W3CBaggagePropagator.getInstance());
                } else {
                    Map.Entry<String, Object> extension = singleExtension(propagator.getExtensionProperties(),
                            TextMapPropagator.class);
                    names.add(extension.getKey());
                    propagators.add(createPropagator(extension.getKey(), extensionProperties(extension.getValue()),
                            componentLoader));
                }
            }
        }
        if (model.getCompositeList() != null) {
            for (String name : model.getCompositeList().split(",")) {
                name = name.trim();
                if (!name.isEmpty() && !"none".equals(name) && names.add(name)) {
                    propagators.add(createPropagator(name, Map.of(), componentLoader));
                }
            }
        }
        return ContextPropagators.create(TextMapPropagator.composite(propagators));
    }

    // Mirrors upstream's TextMapPropagatorFactory
    private static TextMapPropagator createPropagator(String name, Map<String, Object> properties,
            ComponentLoader componentLoader) {
        switch (name) {
            case "tracecontext":
                return W3CTraceContextPropagator.getInstance();
            case "baggage":
                return W3CBaggagePropagator.getInstance();
            default:
                TextMapPropagator propagator = loadComponent(TextMapPropagator.class, name, properties,
                        componentLoader);
                if (propagator != null) {
                    return propagator;
                }
                throw noSuchComponent(TextMapPropagator.class, name);
        }
    }

    private static SdkTracerProviderBuilder createTracerProvider(TracerProviderModel model,
            AttributeLimitsModel attributeLimits, Resource resource, ComponentLoader componentLoader,
            SelfTelemetry selfTelemetry, List<Closeable> closeables) {
        SdkTracerProviderBuilder builder = SdkTracerProvider.builder().setResource(resource);
        builder.setMeterProvider(selfTelemetry::meterProvider);
        if (model.getLimits() != null || attributeLimits != null) {
            builder.setSpanLimits(createSpanLimits(model.getLimits(), attributeLimits));
        }
        if (model.getSampler() != null) {
            builder.setSampler(createSampler(model.getSampler(), componentLoader));
        }
        if (model.getIdGenerator() != null) {
            builder.setIdGenerator(createIdGenerator(model.getIdGenerator(), componentLoader));
        }
        if (model.getProcessors() != null) {
            for (SpanProcessorModel processor : model.getProcessors()) {
                SpanProcessor spanProcessor = createSpanProcessor(processor, componentLoader, selfTelemetry, closeables);
                closeables.add(spanProcessor);
                builder.addSpanProcessor(spanProcessor);
            }
        }
        return builder;
    }

    // Mirrors upstream's IdGeneratorFactory
    private static IdGenerator createIdGenerator(IdGeneratorModel model, ComponentLoader componentLoader) {
        if (model.getRandom() != null) {
            return IdGenerator.random();
        }
        Map<String, Object> additional = model.getExtensionProperties();
        if (additional != null && !additional.isEmpty()) {
            Map.Entry<String, Object> extension = singleExtension(additional, IdGenerator.class);
            String name = extension.getKey();
            IdGenerator idGenerator = loadComponent(IdGenerator.class, name, extensionProperties(extension.getValue()),
                    componentLoader);
            if (idGenerator != null) {
                return idGenerator;
            }
            throw noSuchComponent(IdGenerator.class, name);
        }
        throw new DeclarativeConfigException("The id generator model defines no id generator");
    }

    // The general attribute_limits apply wherever the span-specific limit is unset, as upstream's
    // SpanLimitsFactory does.
    private static SpanLimits createSpanLimits(SpanLimitsModel spanLimits, AttributeLimitsModel attributeLimits) {
        SpanLimitsModel model = spanLimits == null ? new SpanLimitsModel() : spanLimits;
        AttributeLimitsModel general = attributeLimits == null ? new AttributeLimitsModel() : attributeLimits;
        SpanLimitsBuilder builder = SpanLimits.builder();
        Integer attributeCountLimit = orElse(model.getAttributeCountLimit(), general.getAttributeCountLimit());
        if (attributeCountLimit != null) {
            builder.setMaxNumberOfAttributes(attributeCountLimit);
        }
        Integer attributeValueLengthLimit = orElse(model.getAttributeValueLengthLimit(),
                general.getAttributeValueLengthLimit());
        if (attributeValueLengthLimit != null) {
            builder.setMaxAttributeValueLength(attributeValueLengthLimit);
        }
        if (model.getEventCountLimit() != null) {
            builder.setMaxNumberOfEvents(model.getEventCountLimit());
        }
        if (model.getLinkCountLimit() != null) {
            builder.setMaxNumberOfLinks(model.getLinkCountLimit());
        }
        if (model.getEventAttributeCountLimit() != null) {
            builder.setMaxNumberOfAttributesPerEvent(model.getEventAttributeCountLimit());
        }
        if (model.getLinkAttributeCountLimit() != null) {
            builder.setMaxNumberOfAttributesPerLink(model.getLinkAttributeCountLimit());
        }
        return builder.build();
    }

    private static SpanProcessor createSpanProcessor(SpanProcessorModel model, ComponentLoader componentLoader,
            SelfTelemetry selfTelemetry, List<Closeable> closeables) {
        if (model.getBatch() != null) {
            BatchSpanProcessorModel batch = model.getBatch();
            SpanExporter exporter = createSpanExporter(batch.getExporter(), componentLoader, closeables);
            BatchSpanProcessorBuilder builder = BatchSpanProcessor.builder(exporter)
                    .setMeterProvider(selfTelemetry::meterProvider)
                    .setInternalTelemetryVersion(selfTelemetry.version());
            if (batch.getScheduleDelay() != null) {
                builder.setScheduleDelay(Duration.ofMillis(batch.getScheduleDelay()));
            }
            if (batch.getMaxQueueSize() != null) {
                builder.setMaxQueueSize(batch.getMaxQueueSize());
            }
            if (batch.getMaxExportBatchSize() != null) {
                builder.setMaxExportBatchSize(batch.getMaxExportBatchSize());
            }
            if (batch.getExportTimeout() != null) {
                builder.setExporterTimeout(Duration.ofMillis(batch.getExportTimeout()));
            }
            return builder.build();
        }
        if (model.getSimple() != null) {
            SpanExporter exporter = createSpanExporter(model.getSimple().getExporter(), componentLoader, closeables);
            SimpleSpanProcessorBuilder builder = SimpleSpanProcessor.builder(exporter);
            builder.setMeterProvider(selfTelemetry::meterProvider);
            return builder.build();
        }
        Map<String, Object> additional = model.getExtensionProperties();
        if (additional != null && !additional.isEmpty()) {
            Map.Entry<String, Object> extension = singleExtension(additional, SpanProcessor.class);
            String name = extension.getKey();
            SpanProcessor processor = loadComponent(SpanProcessor.class, name, extensionProperties(extension.getValue()),
                    componentLoader);
            if (processor != null) {
                return processor;
            }
            throw noSuchComponent(SpanProcessor.class, name);
        }
        throw new DeclarativeConfigException("The span processor model defines no processor");
    }

    // Registered for closing as soon as it exists, as upstream registers every component it creates, so a later
    // failure still closes it
    private static SpanExporter createSpanExporter(SpanExporterModel model, ComponentLoader componentLoader,
            List<Closeable> closeables) {
        if (model == null) {
            throw new DeclarativeConfigException("The span processor model defines no exporter");
        }
        if (!model.getExtensionProperties().isEmpty()) {
            var extension = singleExtension(model.getExtensionProperties(), SpanExporter.class);
            String name = extension.getKey();
            Map<String, Object> properties = extensionProperties(extension.getValue());
            SpanExporter exporter = loadComponent(SpanExporter.class, name, properties, componentLoader);
            if (exporter != null) {
                closeables.add(exporter);
                return exporter;
            }
            throw noSuchComponent(SpanExporter.class, name);
        }
        throw new DeclarativeConfigException("The span exporter model defines no exporter");
    }

    private static SdkMeterProviderBuilder createMeterProvider(MeterProviderModel model, Resource resource,
            ComponentLoader componentLoader, List<Closeable> closeables) {
        SdkMeterProviderBuilder builder = SdkMeterProvider.builder().setResource(resource);
        if (model.getExemplarFilter() != null) {
            builder.setExemplarFilter(switch (model.getExemplarFilter()) {
                case ALWAYS_ON -> ExemplarFilter.alwaysOn();
                case ALWAYS_OFF -> ExemplarFilter.alwaysOff();
                case TRACE_BASED -> ExemplarFilter.traceBased();
            });
        }
        if (model.getReaders() != null) {
            for (MetricReaderModel reader : model.getReaders()) {
                MetricReader metricReader = createMetricReader(reader, componentLoader);
                closeables.add(metricReader);
                // Quarkus only emits the default limit, applied to every instrument type as autoconfigure did
                CardinalityLimitsModel limits = reader.getPull().getCardinalityLimits();
                if (limits != null && limits.getDefault() != null) {
                    final int limit = limits.getDefault();
                    builder.registerMetricReader(metricReader, new CardinalityLimitSelector() {
                        @Override
                        public int getCardinalityLimit(InstrumentType instrumentType) {
                            return limit;
                        }
                    });
                } else {
                    builder.registerMetricReader(metricReader);
                }
            }
        }
        return builder;
    }

    // As upstream's MetricReaderFactory. QuarkusOTelModelBuilder only emits pull readers: the periodic readers are
    // quarkus_periodic pull nodes.
    private static MetricReader createMetricReader(MetricReaderModel model, ComponentLoader componentLoader) {
        if (model.getPull() == null) {
            throw new DeclarativeConfigException("Only pull metric readers are supported");
        }
        return createPullMetricReader(model.getPull(), componentLoader);
    }

    // As upstream's MetricReaderFactory.PullMetricReaderFactory: the exporter node names a MetricReader component
    private static MetricReader createPullMetricReader(PullMetricReaderModel model, ComponentLoader componentLoader) {
        PullMetricExporterModel exporter = model.getExporter();
        if (exporter == null || exporter.getExtensionProperties().isEmpty()) {
            throw new DeclarativeConfigException("The pull metric reader model defines no exporter");
        }
        var extension = singleExtension(exporter.getExtensionProperties(), MetricReader.class);
        String name = extension.getKey();
        MetricReader reader = loadComponent(MetricReader.class, name, extensionProperties(extension.getValue()),
                componentLoader);
        if (reader != null) {
            return reader;
        }
        throw noSuchComponent(MetricReader.class, name);
    }

    private static SdkLoggerProviderBuilder createLoggerProvider(LoggerProviderModel model,
            AttributeLimitsModel attributeLimits, Resource resource, ComponentLoader componentLoader,
            SelfTelemetry selfTelemetry, List<Closeable> closeables) {
        SdkLoggerProviderBuilder builder = SdkLoggerProvider.builder().setResource(resource);
        builder.setMeterProvider(selfTelemetry::meterProvider);
        if (model.getLimits() != null || attributeLimits != null) {
            LogLimits limits = createLogLimits(model.getLimits(), attributeLimits);
            builder.setLogLimits(() -> limits);
        }
        if (model.getProcessors() != null) {
            for (LogRecordProcessorModel processor : model.getProcessors()) {
                LogRecordProcessor logRecordProcessor = createLogRecordProcessor(processor, componentLoader,
                        selfTelemetry, closeables);
                closeables.add(logRecordProcessor);
                builder.addLogRecordProcessor(logRecordProcessor);
            }
        }
        return builder;
    }

    // The general attribute_limits apply wherever the log-record-specific limit is unset, as upstream's
    // LogLimitsFactory does.
    private static LogLimits createLogLimits(LogRecordLimitsModel logLimits, AttributeLimitsModel attributeLimits) {
        LogRecordLimitsModel model = logLimits == null ? new LogRecordLimitsModel() : logLimits;
        AttributeLimitsModel general = attributeLimits == null ? new AttributeLimitsModel() : attributeLimits;
        var builder = LogLimits.builder();
        Integer attributeCountLimit = orElse(model.getAttributeCountLimit(), general.getAttributeCountLimit());
        if (attributeCountLimit != null) {
            builder.setMaxNumberOfAttributes(attributeCountLimit);
        }
        Integer attributeValueLengthLimit = orElse(model.getAttributeValueLengthLimit(),
                general.getAttributeValueLengthLimit());
        if (attributeValueLengthLimit != null) {
            builder.setMaxAttributeValueLength(attributeValueLengthLimit);
        }
        return builder.build();
    }

    private static Integer orElse(Integer value, Integer fallback) {
        return value != null ? value : fallback;
    }

    private static LogRecordProcessor createLogRecordProcessor(LogRecordProcessorModel model,
            ComponentLoader componentLoader, SelfTelemetry selfTelemetry, List<Closeable> closeables) {
        if (model.getBatch() != null) {
            BatchLogRecordProcessorModel batch = model.getBatch();
            LogRecordExporter exporter = createLogRecordExporter(batch.getExporter(), componentLoader, closeables);
            BatchLogRecordProcessorBuilder builder = BatchLogRecordProcessor.builder(exporter)
                    .setMeterProvider(selfTelemetry.meterProvider())
                    .setInternalTelemetryVersion(selfTelemetry.version());
            if (batch.getScheduleDelay() != null) {
                builder.setScheduleDelay(Duration.ofMillis(batch.getScheduleDelay()));
            }
            if (batch.getMaxQueueSize() != null) {
                builder.setMaxQueueSize(batch.getMaxQueueSize());
            }
            if (batch.getMaxExportBatchSize() != null) {
                builder.setMaxExportBatchSize(batch.getMaxExportBatchSize());
            }
            if (batch.getExportTimeout() != null) {
                builder.setExporterTimeout(Duration.ofMillis(batch.getExportTimeout()));
            }
            return builder.build();
        }
        if (model.getSimple() != null) {
            LogRecordExporter exporter = createLogRecordExporter(model.getSimple().getExporter(), componentLoader,
                    closeables);
            SimpleLogRecordProcessorBuilder builder = SimpleLogRecordProcessor.builder(exporter);
            builder.setMeterProvider(selfTelemetry::meterProvider);
            return builder.build();
        }
        Map<String, Object> additional = model.getExtensionProperties();
        if (additional != null && !additional.isEmpty()) {
            Map.Entry<String, Object> extension = singleExtension(additional, LogRecordProcessor.class);
            String name = extension.getKey();
            LogRecordProcessor processor = loadComponent(LogRecordProcessor.class, name,
                    extensionProperties(extension.getValue()), componentLoader);
            if (processor != null) {
                return processor;
            }
            throw noSuchComponent(LogRecordProcessor.class, name);
        }
        throw new DeclarativeConfigException("The log record processor model defines no processor");
    }

    // See createSpanExporter
    private static LogRecordExporter createLogRecordExporter(LogRecordExporterModel model, ComponentLoader componentLoader,
            List<Closeable> closeables) {
        if (model == null) {
            throw new DeclarativeConfigException("The log record processor model defines no exporter");
        }
        if (!model.getExtensionProperties().isEmpty()) {
            var extension = singleExtension(model.getExtensionProperties(), LogRecordExporter.class);
            String name = extension.getKey();
            Map<String, Object> properties = extensionProperties(extension.getValue());
            LogRecordExporter exporter = loadComponent(LogRecordExporter.class, name, properties,
                    componentLoader);
            if (exporter != null) {
                closeables.add(exporter);
                return exporter;
            }
            throw noSuchComponent(LogRecordExporter.class, name);
        }
        throw new DeclarativeConfigException("The log record exporter model defines no exporter");
    }

    // Mirrors upstream's DeclarativeConfiguration.callAutoConfigureListeners
    private static void callAutoConfigureListeners(ComponentLoader componentLoader, OpenTelemetrySdk sdk) {
        for (ComponentProvider provider : componentLoader.load(ComponentProvider.class)) {
            if (provider instanceof AutoConfigureListener listener) {
                try {
                    listener.afterAutoConfigure(sdk);
                } catch (Throwable t) {
                    LOG.warnf(t, "Error invoking listener %s", listener.getClass().getName());
                }
            }
        }
    }

    private static <T> T loadComponent(Class<T> type, String name, Map<String, Object> properties,
            ComponentLoader componentLoader) {
        return loadComponent(type, name, YamlDeclarativeConfigProperties.create(toConfigMap(properties), componentLoader),
                componentLoader);
    }

    // Takes the node as is: upstream's DeclarativeConfigProperties.toMap probes every value as each scalar type, and
    // YamlDeclarativeConfigProperties logs a WARNING for each mismatch
    private static <T> T loadComponent(Class<T> type, String name, DeclarativeConfigProperties properties,
            ComponentLoader componentLoader) {
        // As upstream's DeclarativeConfigContext without a ConfigProvider
        DeclarativeConfigProperties config = new QuarkusExtendedConfigProperties(properties, ConfigProvider.noop());
        for (ComponentProvider provider : componentLoader.load(ComponentProvider.class)) {
            if (type.equals(provider.getType()) && name.equals(provider.getName())) {
                return type.cast(provider.create(config));
            }
        }
        return null;
    }

    private static DeclarativeConfigException noSuchComponent(Class<?> type, String name) {
        return new DeclarativeConfigException(
                "No %s named '%s' was found. Ensure the artifact providing it is a dependency of the application."
                        .formatted(type.getSimpleName(), name));
    }

    // An extension node names exactly one component; with more than one key the pick would depend on map
    // iteration order, so fail instead of resolving an arbitrary entry.
    private static <T> Map.Entry<String, T> singleExtension(Map<String, T> additionalProperties, Class<?> type) {
        if (additionalProperties.size() > 1) {
            throw new DeclarativeConfigException("The %s model must define exactly one component, found: %s"
                    .formatted(type.getSimpleName(), additionalProperties.keySet()));
        }
        return additionalProperties.entrySet().iterator().next();
    }

    // Mirrors upstream's DeclarativeConfiguration.toConfigProperties, which converts an extension node with Jackson:
    // a nested model becomes the map its YAML form parses to, and a collection becomes a list. Only the sampler
    // models QuarkusOTelModelBuilder nests are converted.
    private static Map<String, Object> toConfigMap(Map<?, ?> map) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            result.put((String) entry.getKey(), toConfigValue(entry.getValue()));
        }
        return result;
    }

    private static Object toConfigValue(Object value) {
        if (value instanceof SamplerModel sampler) {
            return toConfigMap(sampler);
        }
        if (value instanceof Map<?, ?> map) {
            return toConfigMap(map);
        }
        if (value instanceof Collection<?> collection) {
            List<Object> result = new ArrayList<>();
            for (Object element : collection) {
                result.add(toConfigValue(element));
            }
            return result;
        }
        return value;
    }

    private static Map<String, Object> toConfigMap(SamplerModel model) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (model.getAlwaysOn() != null) {
            result.put("always_on", Map.of());
        }
        if (model.getAlwaysOff() != null) {
            result.put("always_off", Map.of());
        }
        if (model.getTraceIdRatioBased() != null) {
            Map<String, Object> ratioBased = new LinkedHashMap<>();
            if (model.getTraceIdRatioBased().getRatio() != null) {
                ratioBased.put("ratio", model.getTraceIdRatioBased().getRatio());
            }
            result.put("trace_id_ratio_based", ratioBased);
        }
        ParentBasedSamplerModel parentBased = model.getParentBased();
        if (parentBased != null) {
            Map<String, Object> parent = new LinkedHashMap<>();
            putSampler(parent, "root", parentBased.getRoot());
            putSampler(parent, "remote_parent_sampled", parentBased.getRemoteParentSampled());
            putSampler(parent, "remote_parent_not_sampled", parentBased.getRemoteParentNotSampled());
            putSampler(parent, "local_parent_sampled", parentBased.getLocalParentSampled());
            putSampler(parent, "local_parent_not_sampled", parentBased.getLocalParentNotSampled());
            result.put("parent_based", parent);
        }
        result.putAll(toConfigMap(model.getExtensionProperties()));
        return result;
    }

    private static void putSampler(Map<String, Object> target, String key, SamplerModel sampler) {
        if (sampler != null) {
            target.put(key, toConfigMap(sampler));
        }
    }

    // Extension property values are untyped in the model; Quarkus only ever stores a map there.
    @SuppressWarnings("unchecked")
    private static Map<String, Object> extensionProperties(Object value) {
        return value == null ? Map.of() : (Map<String, Object>) value;
    }

    // The SDK's internal (self-diagnostics) telemetry wiring, as upstream's DeclarativeConfigContext does it for
    // version latest: every provider and processor receives the meter provider, and the batch processors also
    // receive the version. QuarkusDeclarativeSdk.select sends version legacy to the legacy engine.
    private static final class SelfTelemetry {

        private final InternalTelemetryVersion version;
        private final MeterProvider meterProvider;

        private SelfTelemetry(InternalTelemetryVersion version, MeterProvider meterProvider) {
            this.version = version;
            this.meterProvider = meterProvider;
        }

        InternalTelemetryVersion version() {
            return version;
        }

        MeterProvider meterProvider() {
            return meterProvider;
        }
    }

}
