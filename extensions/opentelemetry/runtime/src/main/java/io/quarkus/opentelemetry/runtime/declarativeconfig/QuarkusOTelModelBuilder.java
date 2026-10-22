package io.quarkus.opentelemetry.runtime.declarativeconfig;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.AlwaysOffSamplerModel;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.AlwaysOnSamplerModel;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.AttributeLimitsModel;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.AttributeNameValueModel;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.AttributeTypeModel;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.BatchLogRecordProcessorModel;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.BatchSpanProcessorModel;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.ExporterDefaultHistogramAggregationModel;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.ExporterTemporalityPreferenceModel;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.LogRecordExporterModel;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.LogRecordLimitsModel;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.LogRecordProcessorModel;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.LoggerProviderModel;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.MeterProviderModel;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.MetricReaderModel;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.NameStringValuePairModel;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.OpenTelemetryConfigurationModel;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.OtlpGrpcExporterModel;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.OtlpGrpcMetricExporterModel;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.OtlpHttpExporterModel;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.OtlpHttpMetricExporterModel;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.ParentBasedSamplerModel;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.PeriodicMetricReaderModel;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.PropagatorModel;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.PushMetricExporterModel;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.ResourceModel;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.SamplerModel;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.SimpleLogRecordProcessorModel;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.SimpleSpanProcessorModel;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.SpanExporterModel;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.SpanLimitsModel;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.SpanProcessorModel;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.TraceIdRatioBasedSamplerModel;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.TracerProviderModel;
import io.quarkus.opentelemetry.runtime.config.build.ExporterType;
import io.quarkus.opentelemetry.runtime.config.build.OTelBuildConfig;
import io.quarkus.opentelemetry.runtime.config.build.SamplerType;
import io.quarkus.opentelemetry.runtime.config.build.TracesBuildConfig;
import io.quarkus.opentelemetry.runtime.config.runtime.AttributeConfig;
import io.quarkus.opentelemetry.runtime.config.runtime.BatchLogRecordProcessorConfig;
import io.quarkus.opentelemetry.runtime.config.runtime.BatchSpanProcessorConfig;
import io.quarkus.opentelemetry.runtime.config.runtime.OTelRuntimeConfig;
import io.quarkus.opentelemetry.runtime.config.runtime.SpanConfig;
import io.quarkus.opentelemetry.runtime.config.runtime.TracesRuntimeConfig;
import io.quarkus.opentelemetry.runtime.config.runtime.exporter.CompressionType;
import io.quarkus.opentelemetry.runtime.config.runtime.exporter.OtlpExporterConfig;
import io.quarkus.opentelemetry.runtime.config.runtime.exporter.OtlpExporterMetricsConfig;
import io.quarkus.opentelemetry.runtime.config.runtime.exporter.OtlpExporterRuntimeConfig;
import io.quarkus.opentelemetry.runtime.exporter.otlp.sender.VertxHttpSender;

/**
 * Pure-function translation of the resolved {@code quarkus.otel.*} configuration into an
 * {@link OpenTelemetryConfigurationModel}, interpreted by
 * {@link QuarkusDeclarativeConfiguration#create(OpenTelemetryConfigurationModel, QuarkusDeclarativeConfiguration.Customizations)}.
 * Upstream's {@code DeclarativeConfiguration.create(OpenTelemetryConfigurationModel)} must not be used: it requires
 * Jackson 2 and SnakeYAML, which are excluded from the declarative-config dependency (see the comment on that
 * dependency in this module's pom).
 * <p>
 * {@code quarkus.otel.*.exporter} defaults to {@code cdi}: Quarkus's own OTLP exporter (currently built on Vert.x,
 * an implementation detail this mapping doesn't depend on), and any exporter contributed by a Quarkiverse
 * extension, is registered as a {@code SpanExporter}/{@code MetricExporter}/
 * {@code LogRecordExporter} CDI bean, looked up generically today via
 * {@code io.quarkus.opentelemetry.runtime.tracing.spi.SpanExporterCDIProvider} (and its metric/log equivalents).
 * Those same classes also implement the declarative-config {@code ComponentProvider} SPI under the identical name
 * ({@link ExporterType.Constants#CDI_VALUE}), so none of the existing CDI bean wiring (including Quarkiverse
 * exporters) needs to change.
 * </p>
 * <p>
 * {@code exporter=otlp} is the escape hatch for using the real upstream {@code opentelemetry-exporter-otlp} library
 * directly (it requires the user to add an HTTP sender dependency, since Quarkus doesn't ship one). That case is
 * mapped onto the model's typed {@code otlp_http}/{@code otlp_grpc} fields, which resolve, by name, to upstream's own
 * {@code ComponentProvider}.
 * </p>
 */
public final class QuarkusOTelModelBuilder {

    /**
     * Component name for the Sampler {@code ComponentProvider} that wraps a delegate sampler to exclude a
     * configured set of URI prefixes, mirroring {@code DropTargetsSampler} usage in
     * {@code AutoConfiguredOpenTelemetrySdkBuilderCustomizer.SamplerCustomizer} for the legacy SDK initialisation
     * path.
     */
    public static final String QUARKUS_DROP_TARGETS = "quarkus_drop_targets";

    /**
     * The declarative configuration schema version. Only upstream's {@code DeclarativeConfiguration} checks it,
     * warning when it differs from the version the SDK expects.
     */
    private static final String FILE_FORMAT = "1.2";

    private static final Set<String> SIMPLE_PROCESSOR_EXPORTERS = Set.of("console", "logging");

    private QuarkusOTelModelBuilder() {
    }

    /**
     * @param nonApplicationUriTargets the build-time-computed set of non-application URIs (e.g. {@code /q/health})
     *        contributed by other extensions, mirroring {@code TracerRecorder.dropNonApplicationUriTargets}
     * @param staticResourceTargets the build-time-computed set of static resource URIs, mirroring
     *        {@code TracerRecorder.dropStaticResourceTargets}
     */
    public static OpenTelemetryConfigurationModel build(OTelBuildConfig buildConfig,
            OTelRuntimeConfig runtimeConfig,
            OtlpExporterRuntimeConfig exporterRuntimeConfig,
            Attributes resourceAttributes,
            Set<String> nonApplicationUriTargets,
            Set<String> staticResourceTargets) {
        OpenTelemetryConfigurationModel model = new OpenTelemetryConfigurationModel()
                .setFileFormat(FILE_FORMAT)
                .setDisabled(runtimeConfig.sdkDisabled())
                .setAttributeLimits(buildAttributeLimits(runtimeConfig.attribute()))
                .setResource(buildResource(resourceAttributes))
                .setPropagator(buildPropagator(buildConfig.propagators()));

        // A null tracer_provider makes the SDK fall back to a default provider sampling parentbased(always_on), so
        // disabled traces get an explicit always_off sampler instead, matching the legacy SamplerCustomizer.
        model.setTracerProvider(buildConfig.traces().enabled().orElse(true)
                ? buildTracerProvider(buildConfig, runtimeConfig, exporterRuntimeConfig, nonApplicationUriTargets,
                        staticResourceTargets)
                : new TracerProviderModel().setSampler(new SamplerModel().setAlwaysOff(new AlwaysOffSamplerModel())));
        if (buildConfig.metrics().enabled().orElse(true)) {
            model.setMeterProvider(buildMeterProvider(buildConfig, runtimeConfig, exporterRuntimeConfig));
        }
        if (buildConfig.logs().enabled().orElse(true)) {
            model.setLoggerProvider(buildLoggerProvider(buildConfig, runtimeConfig, exporterRuntimeConfig));
        }

        return model;
    }

    private static AttributeLimitsModel buildAttributeLimits(AttributeConfig attributeConfig) {
        AttributeLimitsModel limits = new AttributeLimitsModel()
                .setAttributeCountLimit(attributeConfig.countLimit());
        attributeConfig.valueLengthLimit()
                .ifPresent(value -> limits.setAttributeValueLengthLimit(Integer.valueOf(value)));
        return limits;
    }

    private static ResourceModel buildResource(Attributes resourceAttributes) {
        List<AttributeNameValueModel> attributes = new ArrayList<>();
        resourceAttributes.forEach((key, value) -> attributes.add(toAttributeNameValueModel(key, value)));
        return new ResourceModel().setAttributes(attributes);
    }

    private static AttributeNameValueModel toAttributeNameValueModel(AttributeKey<?> key, Object value) {
        AttributeTypeModel type = switch (key.getType()) {
            case STRING -> AttributeTypeModel.STRING;
            case BOOLEAN -> AttributeTypeModel.BOOL;
            case LONG -> AttributeTypeModel.INT;
            case DOUBLE -> AttributeTypeModel.DOUBLE;
            case STRING_ARRAY -> AttributeTypeModel.STRING_ARRAY;
            case BOOLEAN_ARRAY -> AttributeTypeModel.BOOL_ARRAY;
            case LONG_ARRAY -> AttributeTypeModel.INT_ARRAY;
            case DOUBLE_ARRAY -> AttributeTypeModel.DOUBLE_ARRAY;
            // The type has no mapping to the AttributeTypeModel
            default -> throw new IllegalArgumentException(
                    "Resource attribute '%s' has no mapping for the declarative-config.".formatted(key.getKey()));
        };
        return new AttributeNameValueModel()
                .setName(key.getKey())
                .setValue(value)
                .setType(type);
    }

    private static PropagatorModel buildPropagator(List<String> propagators) {
        return new PropagatorModel().setCompositeList(String.join(",", propagators));
    }

    private static TracerProviderModel buildTracerProvider(OTelBuildConfig buildConfig, OTelRuntimeConfig runtimeConfig,
            OtlpExporterRuntimeConfig exporterRuntimeConfig, Set<String> nonApplicationUriTargets,
            Set<String> staticResourceTargets) {
        SpanConfig spanConfig = runtimeConfig.span();
        SpanLimitsModel limits = new SpanLimitsModel()
                .setAttributeCountLimit(spanConfig.attributeCountLimit())
                .setEventCountLimit(spanConfig.eventCountLimit())
                .setLinkCountLimit(spanConfig.linkCountLimit());
        spanConfig.attributeValueLengthLimit().ifPresent(limits::withAttributeValueLengthLimit);

        return new TracerProviderModel()
                .setLimits(limits)
                .setSampler(buildSampler(buildConfig.traces(), runtimeConfig.traces(), nonApplicationUriTargets,
                        staticResourceTargets))
                .setProcessors(buildSpanProcessors(buildConfig, runtimeConfig, exporterRuntimeConfig));
    }

    private static SamplerModel buildSampler(TracesBuildConfig tracesBuildConfig, TracesRuntimeConfig tracesRuntimeConfig,
            Set<String> nonApplicationUriTargets, Set<String> staticResourceTargets) {
        SamplerModel sampler = buildEffectiveSampler(tracesBuildConfig, tracesRuntimeConfig);

        Set<String> dropTargets = new HashSet<>();
        if (tracesRuntimeConfig.suppressNonApplicationUris()) {
            dropTargets.addAll(nonApplicationUriTargets);
        }
        if (!tracesRuntimeConfig.includeStaticResources()) {
            dropTargets.addAll(staticResourceTargets);
        }
        tracesRuntimeConfig.suppressApplicationUris().ifPresent(uris -> uris.stream()
                .filter(Predicate.not(String::isEmpty))
                .map(QuarkusOTelModelBuilder::addSlashIfNecessary)
                .forEach(dropTargets::add));

        if (dropTargets.isEmpty()) {
            return sampler;
        }
        return new SamplerModel().setExtensionProperty(QUARKUS_DROP_TARGETS, Map.of(
                "delegate", sampler,
                "drop_targets", Set.copyOf(dropTargets)));
    }

    private static String addSlashIfNecessary(String uri) {
        return uri.startsWith("/") ? uri : "/" + uri;
    }

    private static SamplerModel buildEffectiveSampler(TracesBuildConfig tracesBuildConfig,
            TracesRuntimeConfig tracesRuntimeConfig) {
        String sampler = tracesBuildConfig.sampler();
        return switch (sampler) {
            case SamplerType.Constants.ALWAYS_ON -> new SamplerModel().setAlwaysOn(new AlwaysOnSamplerModel());
            case SamplerType.Constants.ALWAYS_OFF -> new SamplerModel().setAlwaysOff(new AlwaysOffSamplerModel());
            case SamplerType.Constants.TRACE_ID_RATIO -> new SamplerModel().setTraceIdRatioBased(
                    new TraceIdRatioBasedSamplerModel().setRatio(ratio(tracesRuntimeConfig)));
            case SamplerType.Constants.PARENT_BASED_ALWAYS_ON -> new SamplerModel().setParentBased(
                    new ParentBasedSamplerModel().setRoot(new SamplerModel().setAlwaysOn(new AlwaysOnSamplerModel())));
            case SamplerType.Constants.PARENT_BASED_ALWAYS_OFF -> new SamplerModel().setParentBased(
                    new ParentBasedSamplerModel().setRoot(new SamplerModel().setAlwaysOff(new AlwaysOffSamplerModel())));
            case SamplerType.Constants.PARENT_BASED_TRACE_ID_RATIO -> new SamplerModel().setParentBased(
                    new ParentBasedSamplerModel().setRoot(new SamplerModel()
                            .setTraceIdRatioBased(new TraceIdRatioBasedSamplerModel().setRatio(ratio(tracesRuntimeConfig)))));
            // Custom/SPI sampler name, resolved via a Sampler ComponentProvider registered under that name.
            default -> new SamplerModel().setExtensionProperty(sampler, Map.of());
        };
    }

    // Only parsed for the ratio-based samplers: a custom sampler may use a non-numeric sampler-arg.
    private static double ratio(TracesRuntimeConfig tracesRuntimeConfig) {
        return Double.parseDouble(tracesRuntimeConfig.samplerArg().orElse("0.1d"));
    }

    private static List<SpanProcessorModel> buildSpanProcessors(OTelBuildConfig buildConfig, OTelRuntimeConfig runtimeConfig,
            OtlpExporterRuntimeConfig exporterRuntimeConfig) {
        BatchSpanProcessorConfig bsp = runtimeConfig.bsp();
        List<SpanProcessorModel> processors = new ArrayList<>();
        for (String name : exporterNames("quarkus.otel.traces.exporter", buildConfig.traces().exporter())) {
            SpanExporterModel exporter = buildSpanExporterModel(name, exporterRuntimeConfig.traces());
            processors.add(useSimpleProcessor(buildConfig, name)
                    ? new SpanProcessorModel().setSimple(new SimpleSpanProcessorModel().setExporter(exporter))
                    : new SpanProcessorModel().setBatch(new BatchSpanProcessorModel()
                            .setExporter(exporter)
                            .setScheduleDelay(Math.toIntExact(bsp.scheduleDelay().toMillis()))
                            .setMaxQueueSize(bsp.maxQueueSize())
                            .setMaxExportBatchSize(bsp.maxExportBatchSize())
                            .setExportTimeout(Math.toIntExact(bsp.exportTimeout().toMillis()))));
        }
        return processors;
    }

    private static MeterProviderModel buildMeterProvider(OTelBuildConfig buildConfig, OTelRuntimeConfig runtimeConfig,
            OtlpExporterRuntimeConfig exporterRuntimeConfig) {
        // Matches autoconfigure's MeterProviderConfiguration: one periodic reader per exporter.
        List<MetricReaderModel> readers = new ArrayList<>();
        for (String name : exporterNames("quarkus.otel.metrics.exporter", buildConfig.metrics().exporter())) {
            readers.add(new MetricReaderModel().setPeriodic(new PeriodicMetricReaderModel()
                    .setExporter(buildMetricExporterModel(name, exporterRuntimeConfig.metrics()))
                    .setInterval(Math.toIntExact(runtimeConfig.metric().exportInterval().toMillis()))));
        }
        return readers.isEmpty() ? new MeterProviderModel() : new MeterProviderModel().setReaders(readers);
    }

    private static LoggerProviderModel buildLoggerProvider(OTelBuildConfig buildConfig, OTelRuntimeConfig runtimeConfig,
            OtlpExporterRuntimeConfig exporterRuntimeConfig) {
        LogRecordLimitsModel limits = new LogRecordLimitsModel()
                .setAttributeCountLimit(runtimeConfig.attribute().countLimit());
        runtimeConfig.attribute().valueLengthLimit()
                .ifPresent(value -> limits.setAttributeValueLengthLimit(Integer.valueOf(value)));

        LoggerProviderModel loggerProvider = new LoggerProviderModel().setLimits(limits);
        BatchLogRecordProcessorConfig blrp = runtimeConfig.blrp();
        List<LogRecordProcessorModel> processors = new ArrayList<>();
        for (String name : exporterNames("quarkus.otel.logs.exporter", buildConfig.logs().exporter())) {
            LogRecordExporterModel exporter = buildLogExporterModel(name, exporterRuntimeConfig.logs());
            processors.add(useSimpleProcessor(buildConfig, name)
                    ? new LogRecordProcessorModel().setSimple(new SimpleLogRecordProcessorModel().setExporter(exporter))
                    : new LogRecordProcessorModel().setBatch(new BatchLogRecordProcessorModel()
                            .setExporter(exporter)
                            .setScheduleDelay(Math.toIntExact(blrp.scheduleDelay().toMillis()))
                            .setMaxQueueSize(blrp.maxQueueSize())
                            .setMaxExportBatchSize(blrp.maxExportBatchSize())
                            .setExportTimeout(Math.toIntExact(blrp.exportTimeout().toMillis()))));
        }
        return processors.isEmpty() ? loggerProvider : loggerProvider.setProcessors(processors);
    }

    private static SpanExporterModel buildSpanExporterModel(String name, OtlpExporterConfig signalConfig) {
        if (!ExporterType.Constants.OTLP_VALUE.equals(name)) {
            // "cdi" or a custom exporter resolved by name via the ComponentProvider SPI (or a legacy
            // ConfigurableSpanExporterProvider).
            return new SpanExporterModel().setExtensionProperty(name, Map.of());
        }
        OtlpSettings settings = otlpSettings(signalConfig, VertxHttpSender.TRACES_PATH);
        return settings.grpc()
                ? new SpanExporterModel().setOtlpGrpc(otlpGrpc(settings))
                : new SpanExporterModel().setOtlpHttp(otlpHttp(settings));
    }

    private static PushMetricExporterModel buildMetricExporterModel(String name, OtlpExporterMetricsConfig signalConfig) {
        if (!ExporterType.Constants.OTLP_VALUE.equals(name)) {
            return new PushMetricExporterModel().setExtensionProperty(name, Map.of());
        }
        OtlpSettings settings = otlpSettings(signalConfig, VertxHttpSender.METRICS_PATH);
        ExporterTemporalityPreferenceModel temporalityPreference = resolveTemporalityPreference(signalConfig);
        ExporterDefaultHistogramAggregationModel histogramAggregation = resolveDefaultHistogramAggregation(signalConfig);
        return settings.grpc()
                ? new PushMetricExporterModel().setOtlpGrpc(new OtlpGrpcMetricExporterModel()
                        .setEndpoint(settings.endpoint())
                        .setTimeout(settings.timeout())
                        .setCompression(settings.compression())
                        .setHeaders(settings.headers())
                        .setTemporalityPreference(temporalityPreference)
                        .setDefaultHistogramAggregation(histogramAggregation))
                : new PushMetricExporterModel().setOtlpHttp(new OtlpHttpMetricExporterModel()
                        .setEndpoint(settings.endpoint())
                        .setTimeout(settings.timeout())
                        .setCompression(settings.compression())
                        .setHeaders(settings.headers())
                        .setTemporalityPreference(temporalityPreference)
                        .setDefaultHistogramAggregation(histogramAggregation));
    }

    private static LogRecordExporterModel buildLogExporterModel(String name, OtlpExporterConfig signalConfig) {
        if (!ExporterType.Constants.OTLP_VALUE.equals(name)) {
            return new LogRecordExporterModel().setExtensionProperty(name, Map.of());
        }
        OtlpSettings settings = otlpSettings(signalConfig, VertxHttpSender.LOGS_PATH);
        return settings.grpc()
                ? new LogRecordExporterModel().setOtlpGrpc(otlpGrpc(settings))
                : new LogRecordExporterModel().setOtlpHttp(otlpHttp(settings));
    }

    private static OtlpSettings otlpSettings(OtlpExporterConfig signalConfig, String httpSignalPath) {
        boolean grpc = isGrpc(signalConfig);
        return new OtlpSettings(grpc, resolveEndpoint(signalConfig, grpc, httpSignalPath),
                Math.toIntExact(signalConfig.timeout().toMillis()),
                resolveCompression(signalConfig), resolveHeaders(signalConfig));
    }

    // Spans and logs share the same OTLP exporter model types; metrics has its own (otherwise identical) ones.
    private static OtlpGrpcExporterModel otlpGrpc(OtlpSettings settings) {
        return new OtlpGrpcExporterModel()
                .setEndpoint(settings.endpoint())
                .setTimeout(settings.timeout())
                .setCompression(settings.compression())
                .setHeaders(settings.headers());
    }

    private static OtlpHttpExporterModel otlpHttp(OtlpSettings settings) {
        return new OtlpHttpExporterModel()
                .setEndpoint(settings.endpoint())
                .setTimeout(settings.timeout())
                .setCompression(settings.compression())
                .setHeaders(settings.headers());
    }

    // Mirrors autoconfigure's *ExporterConfiguration: duplicate names collapse, "none" disables the signal and may not
    // be combined with other exporters.
    private static Set<String> exporterNames(String propertyName, List<String> exporters) {
        Set<String> names = new LinkedHashSet<>(exporters);
        if (names.contains(ExporterType.Constants.NONE_VALUE)) {
            if (names.size() > 1) {
                throw new IllegalArgumentException(
                        propertyName + " contains " + ExporterType.Constants.NONE_VALUE + " along with other exporters");
            }
            return Set.of();
        }
        return names;
    }

    // Mirrors autoconfigure's TracerProviderConfiguration/LoggerProviderConfiguration, which always use a simple
    // processor for the console and logging exporters. Every other exporter gets its own batch processor, where
    // autoconfigure wrapped them all in a single batch processor: the declarative model has no composite exporter.
    private static boolean useSimpleProcessor(OTelBuildConfig buildConfig, String exporterName) {
        return buildConfig.simple() || SIMPLE_PROCESSOR_EXPORTERS.contains(exporterName);
    }

    // quarkus.otel.exporter.otlp.<signal>.* already falls back to quarkus.otel.exporter.otlp.* and then to the
    // OTel defaults via OtlpExporterConfigBuilder's FallbackConfigSourceInterceptor; no cascade logic is needed here.
    // TLS is not mapped: the model takes single PEM files where Quarkus configures lists, and Quarkus TLS
    // (including tls-configuration-name) is applied by the cdi exporters.
    private static boolean isGrpc(OtlpExporterConfig signalConfig) {
        return OtlpExporterConfig.Protocol.GRPC.equals(resolveProtocol(signalConfig));
    }

    private static String resolveProtocol(OtlpExporterConfig signalConfig) {
        return signalConfig.protocol().orElse(OtlpExporterConfig.Protocol.HTTP_PROTOBUF);
    }

    private static String resolveEndpoint(OtlpExporterConfig signalConfig, boolean grpc, String httpSignalPath) {
        String endpoint = signalConfig.endpoint().orElse(OtlpExporterConfig.DEFAULT_HTTP_BASE_URI);
        // The config layer's fallback default is always the HTTP endpoint (see OtlpExporterConfigBuilder);
        // swap in the gRPC default when the user picked gRPC but left the endpoint unset, mirroring
        // OTelExporterRecorder.resolveTraceEndpoint/excludeDefaultEndpoint.
        if (grpc) {
            return OtlpExporterConfig.DEFAULT_HTTP_BASE_URI.equals(endpoint)
                    ? OtlpExporterConfig.DEFAULT_GRPC_BASE_URI
                    : endpoint;
        }
        // quarkus.otel.exporter.otlp.*.endpoint is a base URI: the legacy VertxHttpSender always appends the
        // signal path to it. The declarative otlp_http endpoint is used as-is by upstream's ComponentProvider,
        // so the signal path has to be appended here.
        endpoint = endpoint.trim();
        if (endpoint.endsWith("/")) {
            endpoint = endpoint.substring(0, endpoint.length() - 1);
        }
        return endpoint + httpSignalPath;
    }

    private static String resolveCompression(OtlpExporterConfig signalConfig) {
        return signalConfig.compression().map(CompressionType::getValue).orElse(null);
    }

    // Quarkus spells the low-memory preference "lowmemory", like the flat otel.* property; the model spells it
    // "low_memory". Both are accepted.
    private static ExporterTemporalityPreferenceModel resolveTemporalityPreference(OtlpExporterMetricsConfig config) {
        if (config.temporalityPreference().isEmpty()) {
            return null;
        }
        String value = config.temporalityPreference().get().trim().toLowerCase(Locale.ROOT);
        try {
            return ExporterTemporalityPreferenceModel.fromValue("lowmemory".equals(value) ? "low_memory" : value);
        } catch (IllegalArgumentException e) {
            throw new ConfigurationException(
                    "Unrecognized quarkus.otel.exporter.otlp.metrics.temporality-preference: " + value, e);
        }
    }

    private static ExporterDefaultHistogramAggregationModel resolveDefaultHistogramAggregation(
            OtlpExporterMetricsConfig config) {
        if (config.defaultHistogramAggregation().isEmpty()) {
            return null;
        }
        String value = config.defaultHistogramAggregation().get().trim().toLowerCase(Locale.ROOT);
        try {
            return ExporterDefaultHistogramAggregationModel.fromValue(value);
        } catch (IllegalArgumentException e) {
            throw new ConfigurationException(
                    "Unrecognized quarkus.otel.exporter.otlp.metrics.default-histogram-aggregation: " + value, e);
        }
    }

    private static List<NameStringValuePairModel> resolveHeaders(OtlpExporterConfig signalConfig) {
        if (signalConfig.headers().isEmpty()) {
            return List.of();
        }
        List<NameStringValuePairModel> headers = new ArrayList<>();
        for (String header : signalConfig.headers().get()) {
            if (header.isEmpty()) {
                continue;
            }
            String[] parts = header.split("=", 2);
            if (parts.length < 2) {
                continue;
            }
            headers.add(new NameStringValuePairModel()
                    .setName(parts[0].trim())
                    .setValue(parts[1].trim()));
        }
        return headers;
    }

    /**
     * The resolved settings shared by the {@code otlp_http}/{@code otlp_grpc} exporter models of every signal.
     */
    private record OtlpSettings(boolean grpc, String endpoint, int timeout, String compression,
            List<NameStringValuePairModel> headers) {
    }
}
