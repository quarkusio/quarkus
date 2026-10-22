package io.quarkus.opentelemetry.runtime.declarativeconfig;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.UnaryOperator;

import io.opentelemetry.api.baggage.propagation.W3CBaggagePropagator;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.api.incubator.config.ConfigProvider;
import io.opentelemetry.api.incubator.config.DeclarativeConfigException;
import io.opentelemetry.api.incubator.config.DeclarativeConfigProperties;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.common.ComponentLoader;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.OpenTelemetrySdkBuilder;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.YamlDeclarativeConfigProperties;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.AttributeLimitsModel;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.AttributeNameValueModel;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.AttributeTypeModel;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.BatchLogRecordProcessorModel;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.BatchSpanProcessorModel;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.LogRecordExporterModel;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.LogRecordLimitsModel;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.LogRecordProcessorModel;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.LoggerProviderModel;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.MeterProviderModel;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.MetricReaderModel;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.NameStringValuePairModel;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.OpenTelemetryConfigurationModel;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.ParentBasedSamplerModel;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.PeriodicMetricReaderModel;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.PropagatorModel;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.PushMetricExporterModel;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.ResourceModel;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.SamplerModel;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.SamplerPropertyModel;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.SpanExporterModel;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.SpanLimitsModel;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.SpanProcessorModel;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.TracerProviderModel;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigurablePropagatorProvider;
import io.opentelemetry.sdk.autoconfigure.spi.internal.ComponentProvider;
import io.opentelemetry.sdk.autoconfigure.spi.internal.DefaultConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.internal.ExtendedDeclarativeConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.logs.ConfigurableLogRecordExporterProvider;
import io.opentelemetry.sdk.autoconfigure.spi.metrics.ConfigurableMetricExporterProvider;
import io.opentelemetry.sdk.autoconfigure.spi.traces.ConfigurableSamplerProvider;
import io.opentelemetry.sdk.autoconfigure.spi.traces.ConfigurableSpanExporterProvider;
import io.opentelemetry.sdk.common.Clock;
import io.opentelemetry.sdk.logs.LogLimits;
import io.opentelemetry.sdk.logs.LogRecordProcessor;
import io.opentelemetry.sdk.logs.SdkLoggerProvider;
import io.opentelemetry.sdk.logs.SdkLoggerProviderBuilder;
import io.opentelemetry.sdk.logs.export.BatchLogRecordProcessor;
import io.opentelemetry.sdk.logs.export.BatchLogRecordProcessorBuilder;
import io.opentelemetry.sdk.logs.export.LogRecordExporter;
import io.opentelemetry.sdk.logs.export.SimpleLogRecordProcessor;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.metrics.SdkMeterProviderBuilder;
import io.opentelemetry.sdk.metrics.export.MetricExporter;
import io.opentelemetry.sdk.metrics.export.MetricReader;
import io.opentelemetry.sdk.metrics.export.PeriodicMetricReader;
import io.opentelemetry.sdk.metrics.export.PeriodicMetricReaderBuilder;
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
import io.opentelemetry.sdk.trace.export.SpanExporter;
import io.opentelemetry.sdk.trace.samplers.ParentBasedSamplerBuilder;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import io.quarkus.opentelemetry.runtime.tracing.DropTargetsSampler;

/**
 * Quarkus-internal interpreter of the OpenTelemetry declarative configuration model: builds an
 * {@link OpenTelemetrySdk} from an {@link OpenTelemetryConfigurationModel}, the same contract as upstream's
 * {@code DeclarativeConfiguration.create(model)}.
 * <p>
 * Upstream's implementation cannot be used because it unconditionally initializes a Jackson 2 {@code ObjectMapper}
 * (and SnakeYAML), see
 * <a href="https://github.com/open-telemetry/opentelemetry-java/issues/8820">opentelemetry-java#8820</a> and the
 * dependency comment in this module's {@code pom.xml}. Once that issue is resolved upstream, this class is intended
 * to be deleted and replaced by a call to {@code DeclarativeConfiguration.create(model)}.
 * </p>
 * <p>
 * Consequently this is not a general-purpose model interpreter: it only covers the model shapes that
 * {@link QuarkusOTelModelBuilder} emits, plus extension nodes (a model's {@code additionalProperties}), which are
 * resolved exactly as upstream resolves them - by name, through the
 * {@link ComponentProvider} SPI. A name no {@code ComponentProvider} claims is also looked up through the legacy
 * autoconfigure component SPIs ({@link ConfigurableSamplerProvider}, {@link ConfigurableSpanExporterProvider},
 * etc.), so third-party components written for the replaced {@code AutoConfiguredOpenTelemetrySdk} path keep
 * resolving. Such a component receives {@link Customizations#legacyConfigProperties()}: the legacy path passed
 * every {@code quarkus.otel.*} property (rewritten to {@code otel.*}) plus any raw {@code otel.*} property to these
 * SPIs, and components such as custom samplers read their arguments from it (e.g. {@code otel.traces.sampler.arg}).
 * </p>
 * <p>
 * Quarkus-specific runtime pieces that cannot come from the model (the CDI-provided sampler override, id generator,
 * additional span processors, the SDK clock, and the container-managed executor for the periodic metric reader) are
 * supplied through {@link Customizations}.
 * </p>
 * <p>
 * Nothing in Quarkus calls {@link #create(OpenTelemetryConfigurationModel, Customizations)} yet: the SDK is still
 * initialized through {@code AutoConfiguredOpenTelemetrySdk}. {@code OpenTelemetryRecorder} is expected to switch to
 * this class, at which point it has to supply every {@link Customizations} hook with the CDI-sourced values the
 * legacy {@code AutoConfiguredOpenTelemetrySdkBuilderCustomizer} chain provides today.
 * </p>
 */
public final class QuarkusDeclarativeConfiguration {

    private QuarkusDeclarativeConfiguration() {
    }

    /**
     * Builds the SDK from the model. A model with {@code disabled: true} produces an empty (no-op) SDK.
     */
    public static OpenTelemetrySdk create(OpenTelemetryConfigurationModel model, Customizations customizations) {
        if (Boolean.TRUE.equals(model.getDisabled())) {
            return OpenTelemetrySdk.builder().build();
        }
        Resource resource = createResource(model.getResource());
        OpenTelemetrySdkBuilder builder = OpenTelemetrySdk.builder();
        if (model.getPropagator() != null) {
            builder.setPropagators(createPropagators(model.getPropagator(), customizations));
        }
        if (model.getTracerProvider() != null) {
            builder.setTracerProvider(createTracerProvider(model.getTracerProvider(), model.getAttributeLimits(), resource,
                    customizations).build());
        }
        if (model.getMeterProvider() != null) {
            builder.setMeterProvider(createMeterProvider(model.getMeterProvider(), resource, customizations).build());
        }
        if (model.getLoggerProvider() != null) {
            builder.setLoggerProvider(createLoggerProvider(model.getLoggerProvider(), model.getAttributeLimits(), resource,
                    customizations).build());
        }
        return builder.build();
    }

    /**
     * Builds a {@link Sampler} from a typed sampler model, resolving unknown (extension) sampler names through the
     * {@link ComponentProvider} SPI and falling back to the legacy {@link ConfigurableSamplerProvider} SPI.
     */
    public static Sampler createSampler(SamplerModel model, ComponentLoader componentLoader) {
        return createSampler(model, Customizations.builder(componentLoader).build());
    }

    /**
     * Builds a {@link Sampler} from a declarative-config properties node, for use by a {@link ComponentProvider}
     * (which receives {@link DeclarativeConfigProperties} rather than the typed model). Only the sampler shapes
     * {@link QuarkusOTelModelBuilder} emits are recognized; any other single-key node is resolved as an extension
     * sampler by name.
     * <p>
     * Unlike {@link #createSampler(SamplerModel, ComponentLoader)}, there is intentionally no fallback to the legacy
     * {@link ConfigurableSamplerProvider} SPI. This entry point is only reached through
     * {@code QuarkusDropTargetsSamplerProvider} under a standard declarative-config implementation (upstream's
     * {@code DeclarativeConfiguration.create()}), which resolves every other sampler without that fallback too, so
     * resolution stays consistent within each interpreter.
     * </p>
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
            DeclarativeConfigProperties node = config.getStructured(name, DeclarativeConfigProperties.empty());
            Sampler sampler = loadComponent(Sampler.class, name, DeclarativeConfigProperties.toMap(node), componentLoader);
            if (sampler != null) {
                return sampler;
            }
            throw noSuchComponent(Sampler.class, name);
        }
        throw new DeclarativeConfigException("The sampler node defines no sampler");
    }

    private static Sampler createSampler(SamplerModel model, Customizations customizations) {
        if (model.getAlwaysOn() != null) {
            return Sampler.alwaysOn();
        }
        if (model.getAlwaysOff() != null) {
            return Sampler.alwaysOff();
        }
        if (model.getTraceIdRatioBased() != null) {
            Double ratio = model.getTraceIdRatioBased().getRatio();
            return Sampler.traceIdRatioBased(ratio == null ? 1.0d : ratio);
        }
        if (model.getParentBased() != null) {
            return createParentBasedSampler(model.getParentBased(), customizations);
        }
        Map<String, SamplerPropertyModel> additional = model.getAdditionalProperties();
        if (additional != null && !additional.isEmpty()) {
            Map.Entry<String, SamplerPropertyModel> extension = singleExtension(additional, Sampler.class);
            String name = extension.getKey();
            Map<String, Object> properties = extension.getValue() == null ? Map.of()
                    : extension.getValue().getAdditionalProperties();
            if (QuarkusOTelModelBuilder.QUARKUS_DROP_TARGETS.equals(name)) {
                return createDropTargetsSampler(properties, customizations);
            }
            Sampler sampler = loadComponent(Sampler.class, name, properties, customizations.componentLoader());
            if (sampler != null) {
                return sampler;
            }
            for (ConfigurableSamplerProvider provider : customizations.componentLoader()
                    .load(ConfigurableSamplerProvider.class)) {
                if (name.equals(provider.getName())) {
                    return provider.createSampler(customizations.legacyConfigProperties());
                }
            }
            throw noSuchComponent(Sampler.class, name);
        }
        throw new DeclarativeConfigException("The sampler model defines no sampler");
    }

    private static Resource createResource(ResourceModel model) {
        if (model == null || model.getAttributes() == null || model.getAttributes().isEmpty()) {
            return Resource.getDefault();
        }
        AttributesBuilder attributes = Attributes.builder();
        for (AttributeNameValueModel attribute : model.getAttributes()) {
            putAttribute(attributes, attribute);
        }
        return Resource.getDefault().merge(Resource.create(attributes.build()));
    }

    @SuppressWarnings("unchecked")
    private static void putAttribute(AttributesBuilder builder, AttributeNameValueModel attribute) {
        String name = attribute.getName();
        Object value = attribute.getValue();
        if (name == null || value == null) {
            return;
        }
        AttributeTypeModel type = attribute.getType() == null ? AttributeTypeModel.STRING : attribute.getType();
        switch (type) {
            case STRING -> builder.put(AttributeKey.stringKey(name), String.valueOf(value));
            case BOOL -> builder.put(AttributeKey.booleanKey(name), (Boolean) value);
            case INT -> builder.put(AttributeKey.longKey(name), ((Number) value).longValue());
            case DOUBLE -> builder.put(AttributeKey.doubleKey(name), ((Number) value).doubleValue());
            case STRING_ARRAY -> builder.put(AttributeKey.stringArrayKey(name), (List<String>) value);
            case BOOL_ARRAY -> builder.put(AttributeKey.booleanArrayKey(name), (List<Boolean>) value);
            case INT_ARRAY -> builder.put(AttributeKey.longArrayKey(name), (List<Long>) value);
            case DOUBLE_ARRAY -> builder.put(AttributeKey.doubleArrayKey(name), (List<Double>) value);
            default -> throw new DeclarativeConfigException(
                    "Resource attribute '%s' has an unsupported type: %s".formatted(name, type));
        }
    }

    private static ContextPropagators createPropagators(PropagatorModel model, Customizations customizations) {
        String compositeList = model.getCompositeList();
        if (compositeList == null || compositeList.isBlank()) {
            return ContextPropagators.noop();
        }
        List<TextMapPropagator> propagators = new ArrayList<>();
        for (String name : compositeList.split(",")) {
            name = name.trim();
            if (name.isEmpty() || "none".equals(name)) {
                continue;
            }
            propagators.add(customizations.propagatorCustomizer()
                    .apply(createPropagator(name, customizations)));
        }
        return ContextPropagators.create(TextMapPropagator.composite(propagators));
    }

    private static TextMapPropagator createPropagator(String name, Customizations customizations) {
        switch (name) {
            case "tracecontext":
                return W3CTraceContextPropagator.getInstance();
            case "baggage":
                return W3CBaggagePropagator.getInstance();
            default:
                TextMapPropagator propagator = loadComponent(TextMapPropagator.class, name, Map.of(),
                        customizations.componentLoader());
                if (propagator != null) {
                    return propagator;
                }
                for (ConfigurablePropagatorProvider provider : customizations.componentLoader()
                        .load(ConfigurablePropagatorProvider.class)) {
                    if (name.equals(provider.getName())) {
                        return provider.getPropagator(customizations.legacyConfigProperties());
                    }
                }
                throw noSuchComponent(TextMapPropagator.class, name);
        }
    }

    private static SdkTracerProviderBuilder createTracerProvider(TracerProviderModel model,
            AttributeLimitsModel attributeLimits, Resource resource, Customizations customizations) {
        SdkTracerProviderBuilder builder = SdkTracerProvider.builder().setResource(resource);
        if (model.getLimits() != null || attributeLimits != null) {
            builder.setSpanLimits(createSpanLimits(model.getLimits(), attributeLimits));
        }
        builder.setSampler(createEffectiveSampler(model.getSampler(), customizations));
        customizations.idGenerator().ifPresent(builder::setIdGenerator);
        if (model.getProcessors() != null) {
            for (SpanProcessorModel processor : model.getProcessors()) {
                builder.addSpanProcessor(createSpanProcessor(processor, customizations));
            }
        }
        for (SpanProcessor processor : customizations.spanProcessors()) {
            builder.addSpanProcessor(processor);
        }
        return builder;
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
        return builder.build();
    }

    /**
     * Resolves the sampler to install on the tracer provider. A CDI-provided {@link Sampler} bean
     * ({@link Customizations#sampler()}) replaces the configured sampler but keeps the drop-targets wrapping,
     * mirroring the legacy {@code SamplerCustomizer} behavior.
     */
    private static Sampler createEffectiveSampler(SamplerModel model, Customizations customizations) {
        // An explicit always_off sampler is never replaced by the CDI bean: the model builder installs it when
        // tracing is disabled (quarkus.otel.traces.enabled=false), and the legacy SamplerCustomizer returned
        // alwaysOff() unconditionally in that case, ignoring any CDI Sampler bean.
        if (model != null && model.getAlwaysOff() != null) {
            return Sampler.alwaysOff();
        }
        Sampler override = customizations.sampler().orElse(null);
        if (override != null) {
            Set<String> dropTargets = dropTargets(model);
            return dropTargets.isEmpty() ? override : new DropTargetsSampler(override, dropTargets);
        }
        if (model == null) {
            return Sampler.parentBased(Sampler.alwaysOn());
        }
        return createSampler(model, customizations);
    }

    @SuppressWarnings("unchecked")
    private static Set<String> dropTargets(SamplerModel model) {
        if (model == null || model.getAdditionalProperties() == null) {
            return Set.of();
        }
        SamplerPropertyModel wrapper = model.getAdditionalProperties().get(QuarkusOTelModelBuilder.QUARKUS_DROP_TARGETS);
        if (wrapper == null) {
            return Set.of();
        }
        Collection<String> targets = (Collection<String>) wrapper.getAdditionalProperties().get("drop_targets");
        return targets == null ? Set.of() : Set.copyOf(targets);
    }

    private static Sampler createDropTargetsSampler(Map<String, Object> properties, Customizations customizations) {
        SamplerModel delegateModel = (SamplerModel) properties.get("delegate");
        Sampler delegate = delegateModel == null ? Sampler.parentBased(Sampler.alwaysOn())
                : createSampler(delegateModel, customizations);
        @SuppressWarnings("unchecked")
        Collection<String> targets = (Collection<String>) properties.get("drop_targets");
        return new DropTargetsSampler(delegate, targets == null ? Set.of() : Set.copyOf(targets));
    }

    private static Sampler createParentBasedSampler(ParentBasedSamplerModel model, Customizations customizations) {
        ParentBasedSamplerBuilder builder = Sampler.parentBasedBuilder(
                model.getRoot() == null ? Sampler.alwaysOn() : createSampler(model.getRoot(), customizations));
        if (model.getRemoteParentSampled() != null) {
            builder.setRemoteParentSampled(createSampler(model.getRemoteParentSampled(), customizations));
        }
        if (model.getRemoteParentNotSampled() != null) {
            builder.setRemoteParentNotSampled(createSampler(model.getRemoteParentNotSampled(), customizations));
        }
        if (model.getLocalParentSampled() != null) {
            builder.setLocalParentSampled(createSampler(model.getLocalParentSampled(), customizations));
        }
        if (model.getLocalParentNotSampled() != null) {
            builder.setLocalParentNotSampled(createSampler(model.getLocalParentNotSampled(), customizations));
        }
        return builder.build();
    }

    private static SpanProcessor createSpanProcessor(SpanProcessorModel model, Customizations customizations) {
        if (model.getBatch() != null) {
            BatchSpanProcessorModel batch = model.getBatch();
            SpanExporter exporter = createSpanExporter(batch.getExporter(), customizations);
            // Mirrors the legacy SpanProcessorCustomizer: without CDI SpanProcessor beans, a batch processor around
            // the no-op exporter (e.g. "cdi" with no SpanExporter beans) is replaced by a no-op processor, so no
            // batch worker thread is started. The no-op exporter is a singleton (an empty composite always returns
            // it), so an identity check is enough and survives an upstream rename of the package-private class.
            if (customizations.spanProcessors().isEmpty() && exporter == SpanExporter.composite()) {
                return SpanProcessor.composite();
            }
            BatchSpanProcessorBuilder builder = BatchSpanProcessor.builder(exporter);
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
            return SimpleSpanProcessor.create(createSpanExporter(model.getSimple().getExporter(), customizations));
        }
        throw new DeclarativeConfigException("The span processor model defines no processor");
    }

    private static SpanExporter createSpanExporter(SpanExporterModel model, Customizations customizations) {
        if (model == null) {
            throw new DeclarativeConfigException("The span processor model defines no exporter");
        }
        if (model.getOtlpHttp() != null) {
            return loadRequired(SpanExporter.class, "otlp_http", otlpProperties(model.getOtlpHttp().getEndpoint(),
                    model.getOtlpHttp().getTimeout(), model.getOtlpHttp().getCompression(),
                    model.getOtlpHttp().getHeaders()), customizations.componentLoader());
        }
        if (model.getOtlpGrpc() != null) {
            return loadRequired(SpanExporter.class, "otlp_grpc", otlpProperties(model.getOtlpGrpc().getEndpoint(),
                    model.getOtlpGrpc().getTimeout(), model.getOtlpGrpc().getCompression(),
                    model.getOtlpGrpc().getHeaders()), customizations.componentLoader());
        }
        if (model.getAdditionalProperties() != null && !model.getAdditionalProperties().isEmpty()) {
            var extension = singleExtension(model.getAdditionalProperties(), SpanExporter.class);
            String name = extension.getKey();
            Map<String, Object> properties = extension.getValue() == null ? Map.of()
                    : extension.getValue().getAdditionalProperties();
            SpanExporter exporter = loadComponent(SpanExporter.class, name, properties, customizations.componentLoader());
            if (exporter != null) {
                return exporter;
            }
            for (ConfigurableSpanExporterProvider provider : customizations.componentLoader()
                    .load(ConfigurableSpanExporterProvider.class)) {
                if (name.equals(provider.getName())) {
                    return provider.createExporter(customizations.legacyConfigProperties());
                }
            }
            throw noSuchComponent(SpanExporter.class, name);
        }
        throw new DeclarativeConfigException("The span exporter model defines no exporter");
    }

    private static SdkMeterProviderBuilder createMeterProvider(MeterProviderModel model, Resource resource,
            Customizations customizations) {
        SdkMeterProviderBuilder builder = SdkMeterProvider.builder().setResource(resource);
        customizations.clock().ifPresent(builder::setClock);
        if (model.getReaders() != null) {
            for (MetricReaderModel reader : model.getReaders()) {
                builder.registerMetricReader(createMetricReader(reader, customizations));
            }
        }
        return builder;
    }

    private static MetricReader createMetricReader(MetricReaderModel model, Customizations customizations) {
        if (model.getPeriodic() == null) {
            throw new DeclarativeConfigException("Only periodic metric readers are supported");
        }
        PeriodicMetricReaderModel periodic = model.getPeriodic();
        PeriodicMetricReaderBuilder builder = PeriodicMetricReader
                .builder(createMetricExporter(periodic.getExporter(), customizations));
        if (periodic.getInterval() != null) {
            builder.setInterval(Duration.ofMillis(periodic.getInterval()));
        }
        // When the caller supplies an executor, it replaces the reader's own unmanaged single-thread one (which has
        // no context classloader and lives outside the container's pools). Nothing supplies one yet: unit tests
        // leave it empty, and OpenTelemetryRecorder is expected to pass the container-managed scheduled executor
        // once it builds the SDK through this class, wrapped so it tolerates the reader's shutdown calls (the
        // reader assumes it owns its executor and shuts it down on close), as the legacy MetricProviderCustomizer
        // did with NonShutdownableScheduledExecutorService.
        customizations.metricReaderExecutor().ifPresent(builder::setExecutor);
        return builder.build();
    }

    private static MetricExporter createMetricExporter(PushMetricExporterModel model, Customizations customizations) {
        if (model == null) {
            throw new DeclarativeConfigException("The metric reader model defines no exporter");
        }
        if (model.getOtlpHttp() != null) {
            return loadRequired(MetricExporter.class, "otlp_http", otlpProperties(model.getOtlpHttp().getEndpoint(),
                    model.getOtlpHttp().getTimeout(), model.getOtlpHttp().getCompression(),
                    model.getOtlpHttp().getHeaders()), customizations.componentLoader());
        }
        if (model.getOtlpGrpc() != null) {
            return loadRequired(MetricExporter.class, "otlp_grpc", otlpProperties(model.getOtlpGrpc().getEndpoint(),
                    model.getOtlpGrpc().getTimeout(), model.getOtlpGrpc().getCompression(),
                    model.getOtlpGrpc().getHeaders()), customizations.componentLoader());
        }
        if (model.getAdditionalProperties() != null && !model.getAdditionalProperties().isEmpty()) {
            var extension = singleExtension(model.getAdditionalProperties(), MetricExporter.class);
            String name = extension.getKey();
            Map<String, Object> properties = extension.getValue() == null ? Map.of()
                    : extension.getValue().getAdditionalProperties();
            MetricExporter exporter = loadComponent(MetricExporter.class, name, properties, customizations.componentLoader());
            if (exporter != null) {
                return exporter;
            }
            for (ConfigurableMetricExporterProvider provider : customizations.componentLoader()
                    .load(ConfigurableMetricExporterProvider.class)) {
                if (name.equals(provider.getName())) {
                    return provider.createExporter(customizations.legacyConfigProperties());
                }
            }
            throw noSuchComponent(MetricExporter.class, name);
        }
        throw new DeclarativeConfigException("The metric exporter model defines no exporter");
    }

    private static SdkLoggerProviderBuilder createLoggerProvider(LoggerProviderModel model,
            AttributeLimitsModel attributeLimits, Resource resource, Customizations customizations) {
        SdkLoggerProviderBuilder builder = SdkLoggerProvider.builder().setResource(resource);
        if (model.getLimits() != null || attributeLimits != null) {
            LogLimits limits = createLogLimits(model.getLimits(), attributeLimits);
            builder.setLogLimits(() -> limits);
        }
        if (model.getProcessors() != null) {
            for (LogRecordProcessorModel processor : model.getProcessors()) {
                builder.addLogRecordProcessor(createLogRecordProcessor(processor, customizations));
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
            Customizations customizations) {
        if (model.getBatch() != null) {
            BatchLogRecordProcessorModel batch = model.getBatch();
            BatchLogRecordProcessorBuilder builder = BatchLogRecordProcessor
                    .builder(createLogRecordExporter(batch.getExporter(), customizations));
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
            return SimpleLogRecordProcessor.create(createLogRecordExporter(model.getSimple().getExporter(), customizations));
        }
        throw new DeclarativeConfigException("The log record processor model defines no processor");
    }

    private static LogRecordExporter createLogRecordExporter(LogRecordExporterModel model, Customizations customizations) {
        if (model == null) {
            throw new DeclarativeConfigException("The log record processor model defines no exporter");
        }
        if (model.getOtlpHttp() != null) {
            return loadRequired(LogRecordExporter.class, "otlp_http", otlpProperties(model.getOtlpHttp().getEndpoint(),
                    model.getOtlpHttp().getTimeout(), model.getOtlpHttp().getCompression(),
                    model.getOtlpHttp().getHeaders()), customizations.componentLoader());
        }
        if (model.getOtlpGrpc() != null) {
            return loadRequired(LogRecordExporter.class, "otlp_grpc", otlpProperties(model.getOtlpGrpc().getEndpoint(),
                    model.getOtlpGrpc().getTimeout(), model.getOtlpGrpc().getCompression(),
                    model.getOtlpGrpc().getHeaders()), customizations.componentLoader());
        }
        if (model.getAdditionalProperties() != null && !model.getAdditionalProperties().isEmpty()) {
            var extension = singleExtension(model.getAdditionalProperties(), LogRecordExporter.class);
            String name = extension.getKey();
            Map<String, Object> properties = extension.getValue() == null ? Map.of()
                    : extension.getValue().getAdditionalProperties();
            LogRecordExporter exporter = loadComponent(LogRecordExporter.class, name, properties,
                    customizations.componentLoader());
            if (exporter != null) {
                return exporter;
            }
            for (ConfigurableLogRecordExporterProvider provider : customizations.componentLoader()
                    .load(ConfigurableLogRecordExporterProvider.class)) {
                if (name.equals(provider.getName())) {
                    return provider.createExporter(customizations.legacyConfigProperties());
                }
            }
            throw noSuchComponent(LogRecordExporter.class, name);
        }
        throw new DeclarativeConfigException("The log record exporter model defines no exporter");
    }

    /**
     * Builds the properties node for the stock {@code otlp_http}/{@code otlp_grpc} {@link ComponentProvider}s
     * shipped by {@code opentelemetry-exporter-otlp}, using the declarative-config schema key names.
     */
    private static Map<String, Object> otlpProperties(String endpoint, Integer timeout, String compression,
            List<NameStringValuePairModel> headers) {
        Map<String, Object> properties = new LinkedHashMap<>();
        if (endpoint != null) {
            properties.put("endpoint", endpoint);
        }
        if (timeout != null) {
            properties.put("timeout", timeout);
        }
        if (compression != null) {
            properties.put("compression", compression);
        }
        if (headers != null && !headers.isEmpty()) {
            List<Map<String, Object>> headerList = new ArrayList<>(headers.size());
            for (NameStringValuePairModel header : headers) {
                final String name = header.getName();
                final Object value = header.getValue();
                if (name != null && value != null) {
                    headerList.add(Map.of("name", name, "value", value));
                }
            }
            properties.put("headers", headerList);
        }
        return properties;
    }

    private static <T> T loadRequired(Class<T> type, String name, Map<String, Object> properties,
            ComponentLoader componentLoader) {
        T component = loadComponent(type, name, properties, componentLoader);
        if (component == null) {
            throw noSuchComponent(type, name);
        }
        return component;
    }

    private static <T> T loadComponent(Class<T> type, String name, Map<String, Object> properties,
            ComponentLoader componentLoader) {
        DeclarativeConfigProperties config = new ExtendedConfigProperties(
                YamlDeclarativeConfigProperties.create(properties, componentLoader));
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

    /**
     * Quarkus runtime pieces that cannot be expressed in the declarative configuration model and are instead
     * injected into SDK construction directly. Each hook documents the value the legacy
     * {@code AutoConfiguredOpenTelemetrySdkBuilderCustomizer} chain supplies today; see the class Javadoc for who
     * is expected to populate them.
     */
    public static final class Customizations {

        private final ComponentLoader componentLoader;
        private final Sampler sampler;
        private final IdGenerator idGenerator;
        private final List<SpanProcessor> spanProcessors;
        private final Clock clock;
        private final ScheduledExecutorService metricReaderExecutor;
        private final UnaryOperator<TextMapPropagator> propagatorCustomizer;
        private final ConfigProperties legacyConfigProperties;

        private Customizations(Builder builder) {
            this.componentLoader = builder.componentLoader;
            this.sampler = builder.sampler;
            this.idGenerator = builder.idGenerator;
            this.spanProcessors = List.copyOf(builder.spanProcessors);
            this.clock = builder.clock;
            this.metricReaderExecutor = builder.metricReaderExecutor;
            this.propagatorCustomizer = builder.propagatorCustomizer;
            this.legacyConfigProperties = builder.legacyConfigProperties;
        }

        public static Builder builder(ComponentLoader componentLoader) {
            return new Builder(componentLoader);
        }

        public ComponentLoader componentLoader() {
            return componentLoader;
        }

        /**
         * A CDI-provided {@link Sampler} bean which replaces the sampler described in the model, keeping the
         * drop-targets wrapping if the model has one. A bare {@code always_off} model sampler (what the model
         * builder emits for disabled traces) is never replaced.
         */
        public Optional<Sampler> sampler() {
            return Optional.ofNullable(sampler);
        }

        public Optional<IdGenerator> idGenerator() {
            return Optional.ofNullable(idGenerator);
        }

        /**
         * CDI-provided {@link SpanProcessor} beans, added after the processors described in the model.
         */
        public List<SpanProcessor> spanProcessors() {
            return spanProcessors;
        }

        public Optional<Clock> clock() {
            return Optional.ofNullable(clock);
        }

        public Optional<ScheduledExecutorService> metricReaderExecutor() {
            return Optional.ofNullable(metricReaderExecutor);
        }

        public UnaryOperator<TextMapPropagator> propagatorCustomizer() {
            return propagatorCustomizer;
        }

        /**
         * The flat {@code otel.*} properties passed to components resolved through the legacy autoconfigure SPIs
         * ({@link ConfigurableSamplerProvider} and friends). Empty unless set.
         */
        public ConfigProperties legacyConfigProperties() {
            return legacyConfigProperties;
        }

        public static final class Builder {

            private final ComponentLoader componentLoader;
            private final List<SpanProcessor> spanProcessors = new ArrayList<>();
            private Sampler sampler;
            private IdGenerator idGenerator;
            private Clock clock;
            private ScheduledExecutorService metricReaderExecutor;
            private UnaryOperator<TextMapPropagator> propagatorCustomizer = UnaryOperator.identity();
            private ConfigProperties legacyConfigProperties = DefaultConfigProperties.createFromMap(Map.of());

            private Builder(ComponentLoader componentLoader) {
                this.componentLoader = componentLoader;
            }

            public Builder sampler(Sampler sampler) {
                this.sampler = sampler;
                return this;
            }

            public Builder idGenerator(IdGenerator idGenerator) {
                this.idGenerator = idGenerator;
                return this;
            }

            public Builder addSpanProcessor(SpanProcessor spanProcessor) {
                this.spanProcessors.add(spanProcessor);
                return this;
            }

            public Builder clock(Clock clock) {
                this.clock = clock;
                return this;
            }

            public Builder metricReaderExecutor(ScheduledExecutorService metricReaderExecutor) {
                this.metricReaderExecutor = metricReaderExecutor;
                return this;
            }

            public Builder propagatorCustomizer(UnaryOperator<TextMapPropagator> propagatorCustomizer) {
                this.propagatorCustomizer = propagatorCustomizer;
                return this;
            }

            /**
             * Sets the properties passed to legacy autoconfigure SPI components. The caller is expected to build
             * them the way the legacy {@code OpenTelemetryRecorder.getOtelConfigs()} did, i.e. every
             * {@code quarkus.otel.*} property rewritten to {@code otel.*} merged with raw {@code otel.*} properties.
             */
            public Builder legacyConfigProperties(ConfigProperties legacyConfigProperties) {
                this.legacyConfigProperties = legacyConfigProperties;
                return this;
            }

            public Customizations build() {
                return new Customizations(this);
            }
        }
    }

    /**
     * Upstream {@link ComponentProvider}s, such as the OTLP exporters, require {@link ExtendedDeclarativeConfigProperties}.
     * Upstream wraps the properties in {@code DeclarativeConfigContext}, which is part of the
     * {@code DeclarativeConfiguration} path we cannot use, so the wrapping is done here instead.
     */
    private static final class ExtendedConfigProperties implements ExtendedDeclarativeConfigProperties {

        private final DeclarativeConfigProperties delegate;

        private ExtendedConfigProperties(final DeclarativeConfigProperties delegate) {
            this.delegate = delegate;
        }

        @Override
        public ConfigProvider getConfigProvider() {
            return ConfigProvider.noop();
        }

        @Override
        public String getString(String name) {
            return delegate.getString(name);
        }

        @Override
        public Boolean getBoolean(String name) {
            return delegate.getBoolean(name);
        }

        @Override
        public Integer getInt(String name) {
            return delegate.getInt(name);
        }

        @Override
        public Long getLong(String name) {
            return delegate.getLong(name);
        }

        @Override
        public Double getDouble(String name) {
            return delegate.getDouble(name);
        }

        @Override
        public <T> List<T> getScalarList(String name, Class<T> scalarType) {
            return delegate.getScalarList(name, scalarType);
        }

        @Override
        public DeclarativeConfigProperties getStructured(String name) {
            DeclarativeConfigProperties structured = delegate.getStructured(name);
            return structured == null ? null : new ExtendedConfigProperties(structured);
        }

        @Override
        public List<DeclarativeConfigProperties> getStructuredList(String name) {
            List<DeclarativeConfigProperties> structuredList = delegate.getStructuredList(name);
            if (structuredList == null) {
                return null;
            }
            List<DeclarativeConfigProperties> wrapped = new ArrayList<>(structuredList.size());
            for (DeclarativeConfigProperties structured : structuredList) {
                wrapped.add(new ExtendedConfigProperties(structured));
            }
            return wrapped;
        }

        @Override
        public Set<String> getPropertyKeys() {
            return delegate.getPropertyKeys();
        }

        @Override
        public ComponentLoader getComponentLoader() {
            return delegate.getComponentLoader();
        }
    }
}
