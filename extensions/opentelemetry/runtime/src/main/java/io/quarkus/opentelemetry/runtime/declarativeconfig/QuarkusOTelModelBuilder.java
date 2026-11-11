package io.quarkus.opentelemetry.runtime.declarativeconfig;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
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
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.LogRecordExporterModel;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.LogRecordExporterPropertyModel;
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
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.PushMetricExporterPropertyModel;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.ResourceModel;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.SamplerModel;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.SamplerPropertyModel;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.SimpleLogRecordProcessorModel;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.SimpleSpanProcessorModel;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.SpanExporterModel;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.SpanExporterPropertyModel;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.SpanLimitsModel;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.SpanProcessorModel;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.TraceIdRatioBasedSamplerModel;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.TracerProviderModel;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigurationException;
import io.quarkus.opentelemetry.runtime.config.build.ExporterType;
import io.quarkus.opentelemetry.runtime.config.build.OTelBuildConfig;
import io.quarkus.opentelemetry.runtime.config.build.SamplerType;
import io.quarkus.opentelemetry.runtime.config.runtime.OTelRuntimeConfig;
import io.quarkus.opentelemetry.runtime.config.runtime.TracesRuntimeConfig;
import io.quarkus.opentelemetry.runtime.config.runtime.exporter.CompressionType;
import io.quarkus.opentelemetry.runtime.config.runtime.exporter.OtlpExporterConfig;
import io.quarkus.opentelemetry.runtime.config.runtime.exporter.OtlpExporterRuntimeConfig;
import io.quarkus.opentelemetry.runtime.exporter.otlp.sender.VertxHttpSender;

/**
 * Builds the {@link OpenTelemetryConfigurationModel} from the resolved configuration (the typed
 * {@code quarkus.otel.*} mappings plus the merged flat {@code otel.*} view the recorder computes), interpreted by
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
     * The declarative configuration schema version implemented by the pinned
     * {@code opentelemetry-sdk-extension-declarative-config} version.
     */
    private static final String FILE_FORMAT = "1.1";

    private static final Set<String> SIMPLE_PROCESSOR_EXPORTERS = Set.of("console", "logging");

    private final OTelBuildConfig buildConfig;
    private final OTelRuntimeConfig runtimeConfig;
    private final OtlpExporterRuntimeConfig exporterRuntimeConfig;
    private final ConfigProperties otelConfigProperties;

    private Attributes resourceAttributes = Attributes.empty();
    private Set<String> nonApplicationUriTargets = Set.of();
    private Set<String> staticResourceTargets = Set.of();
    private boolean cdiSpanProcessorsPresent;
    private boolean cdiLogRecordExporterResolvable;

    private QuarkusOTelModelBuilder(OTelBuildConfig buildConfig, OTelRuntimeConfig runtimeConfig,
            OtlpExporterRuntimeConfig exporterRuntimeConfig, ConfigProperties otelConfigProperties) {
        this.buildConfig = buildConfig;
        this.runtimeConfig = runtimeConfig;
        this.exporterRuntimeConfig = exporterRuntimeConfig;
        this.otelConfigProperties = otelConfigProperties;
    }

    /**
     * @param otelConfigProperties the flat {@code otel.*} view of the resolved configuration, computed by the
     *        recorder: every {@code quarkus.otel.*} value (defaults included) merged with the bare {@code otel.*}
     *        properties, the {@code otel.*} spelling winning under {@code quarkus.otel.mp.compatibility}. The
     *        decisions autoconfigure used to make from these properties are made from this view, so the
     *        MicroProfile Telemetry {@code otel.*} configuration contract keeps working; Quarkus-specific
     *        decisions (enablement, {@code simple}, URI suppression, OTLP exporter settings) stay on the typed
     *        config.
     */
    public static QuarkusOTelModelBuilder forConfig(OTelBuildConfig buildConfig, OTelRuntimeConfig runtimeConfig,
            OtlpExporterRuntimeConfig exporterRuntimeConfig, ConfigProperties otelConfigProperties) {
        return new QuarkusOTelModelBuilder(buildConfig, runtimeConfig, exporterRuntimeConfig, otelConfigProperties);
    }

    /**
     * The resolved resource attributes the model carries; the recorder supplies the fully merged resource.
     */
    public QuarkusOTelModelBuilder resourceAttributes(Attributes resourceAttributes) {
        this.resourceAttributes = resourceAttributes;
        return this;
    }

    /**
     * The build-time-computed set of non-application URIs (e.g. {@code /q/health}) contributed by other extensions,
     * mirroring {@code TracerRecorder.dropNonApplicationUriTargets}.
     */
    public QuarkusOTelModelBuilder nonApplicationUriTargets(Set<String> nonApplicationUriTargets) {
        this.nonApplicationUriTargets = nonApplicationUriTargets;
        return this;
    }

    /**
     * The build-time-computed set of static resource URIs, mirroring
     * {@code TracerRecorder.dropStaticResourceTargets}.
     */
    public QuarkusOTelModelBuilder staticResourceTargets(Set<String> staticResourceTargets) {
        this.staticResourceTargets = staticResourceTargets;
        return this;
    }

    /**
     * Whether the application contributes {@code SpanProcessor} beans: {@code quarkus.otel.simple} then keeps the
     * batch processor, as the legacy {@code SpanProcessorCustomizer} only swapped it for a simple one when no such
     * beans existed.
     */
    public QuarkusOTelModelBuilder cdiSpanProcessorsPresent(boolean cdiSpanProcessorsPresent) {
        this.cdiSpanProcessorsPresent = cdiSpanProcessorsPresent;
        return this;
    }

    /**
     * Whether exactly one default-qualified {@code LogRecordExporter} bean is resolvable:
     * {@code quarkus.otel.simple} only affects the log pipeline when one is, as the legacy
     * {@code SimpleLogRecordProcessorCustomizer} did nothing otherwise.
     */
    public QuarkusOTelModelBuilder cdiLogRecordExporterResolvable(boolean cdiLogRecordExporterResolvable) {
        this.cdiLogRecordExporterResolvable = cdiLogRecordExporterResolvable;
        return this;
    }

    public OpenTelemetryConfigurationModel build() {
        OpenTelemetryConfigurationModel model = new OpenTelemetryConfigurationModel()
                .withFileFormat(FILE_FORMAT)
                .withDisabled(otelConfigProperties.getBoolean("otel.sdk.disabled", false))
                .withAttributeLimits(buildAttributeLimits())
                .withResource(buildResource())
                .withPropagator(buildPropagator(otelConfigProperties.getList("otel.propagators")));

        // A null tracer_provider makes the SDK fall back to a default provider sampling parentbased(always_on), so
        // disabled traces get an explicit always_off sampler instead, matching the legacy SamplerCustomizer.
        model.withTracerProvider(buildConfig.traces().enabled().orElse(true)
                ? buildTracerProvider()
                : new TracerProviderModel().withSampler(new SamplerModel().withAlwaysOff(new AlwaysOffSamplerModel())));
        if (buildConfig.metrics().enabled().orElse(true)) {
            model.withMeterProvider(buildMeterProvider());
        }
        if (buildConfig.logs().enabled().orElse(true)) {
            model.withLoggerProvider(buildLoggerProvider());
        }

        return model;
    }

    private AttributeLimitsModel buildAttributeLimits() {
        AttributeLimitsModel limits = new AttributeLimitsModel()
                .withAttributeCountLimit(otelConfigProperties.getInt("otel.attribute.count.limit"));
        Integer valueLengthLimit = otelConfigProperties.getInt("otel.attribute.value.length.limit");
        if (valueLengthLimit != null) {
            limits.withAttributeValueLengthLimit(valueLengthLimit);
        }
        return limits;
    }

    private ResourceModel buildResource() {
        List<AttributeNameValueModel> attributes = new ArrayList<>();
        resourceAttributes.forEach((key, value) -> attributes.add(toAttributeNameValueModel(key, value)));
        return new ResourceModel().withAttributes(attributes);
    }

    private TracerProviderModel buildTracerProvider() {
        SpanLimitsModel limits = new SpanLimitsModel()
                .withAttributeCountLimit(otelConfigProperties.getInt("otel.span.attribute.count.limit"))
                .withEventCountLimit(otelConfigProperties.getInt("otel.span.event.count.limit"))
                .withLinkCountLimit(otelConfigProperties.getInt("otel.span.link.count.limit"));
        Integer attributeValueLengthLimit = otelConfigProperties.getInt("otel.span.attribute.value.length.limit");
        if (attributeValueLengthLimit != null) {
            limits.withAttributeValueLengthLimit(attributeValueLengthLimit);
        }

        return new TracerProviderModel()
                .withLimits(limits)
                .withSampler(buildSampler(runtimeConfig.traces()))
                .withProcessors(buildSpanProcessors());
    }

    private SamplerModel buildSampler(TracesRuntimeConfig tracesRuntimeConfig) {
        SamplerModel sampler = buildEffectiveSampler();

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
        return new SamplerModel().withAdditionalProperty(QUARKUS_DROP_TARGETS, new SamplerPropertyModel()
                .withAdditionalProperty("delegate", sampler)
                .withAdditionalProperty("drop_targets", Set.copyOf(dropTargets)));
    }

    private SamplerModel buildEffectiveSampler() {
        String sampler = otelConfigProperties.getString("otel.traces.sampler",
                SamplerType.Constants.PARENT_BASED_TRACE_ID_RATIO);
        return switch (sampler) {
            case SamplerType.Constants.ALWAYS_ON -> new SamplerModel().withAlwaysOn(new AlwaysOnSamplerModel());
            case SamplerType.Constants.ALWAYS_OFF -> new SamplerModel().withAlwaysOff(new AlwaysOffSamplerModel());
            case SamplerType.Constants.TRACE_ID_RATIO -> new SamplerModel().withTraceIdRatioBased(
                    new TraceIdRatioBasedSamplerModel().withRatio(ratio()));
            case SamplerType.Constants.PARENT_BASED_ALWAYS_ON -> new SamplerModel().withParentBased(
                    new ParentBasedSamplerModel().withRoot(new SamplerModel().withAlwaysOn(new AlwaysOnSamplerModel())));
            case SamplerType.Constants.PARENT_BASED_ALWAYS_OFF -> new SamplerModel().withParentBased(
                    new ParentBasedSamplerModel().withRoot(new SamplerModel().withAlwaysOff(new AlwaysOffSamplerModel())));
            case SamplerType.Constants.PARENT_BASED_TRACE_ID_RATIO -> new SamplerModel().withParentBased(
                    new ParentBasedSamplerModel().withRoot(new SamplerModel()
                            .withTraceIdRatioBased(
                                    new TraceIdRatioBasedSamplerModel().withRatio(ratio()))));
            // Custom/SPI sampler name, resolved via a Sampler ComponentProvider registered under that name.
            default -> new SamplerModel().withAdditionalProperty(sampler, new SamplerPropertyModel());
        };
    }

    // Only parsed for the ratio-based samplers: a custom sampler may use a non-numeric sampler-arg.
    private double ratio() {
        return Double.parseDouble(otelConfigProperties.getString("otel.traces.sampler.arg", "0.1d"));
    }

    private List<SpanProcessorModel> buildSpanProcessors() {
        List<SpanProcessorModel> processors = new ArrayList<>();
        for (String name : exporterNames("quarkus.otel.traces.exporter",
                otelConfigProperties.getList("otel.traces.exporter"))) {
            SpanExporterModel exporter = buildSpanExporterModel(name, exporterRuntimeConfig.traces());
            // quarkus.otel.simple has no effect when the application contributes SpanProcessor beans: the legacy
            // SpanProcessorCustomizer only swapped batch for simple when no SpanProcessor beans existed. The
            // console-type exporters stay simple regardless, as autoconfigure creates them.
            boolean simple = SIMPLE_PROCESSOR_EXPORTERS.contains(name)
                    || (buildConfig.simple() && !cdiSpanProcessorsPresent);
            processors.add(simple
                    ? new SpanProcessorModel().withSimple(new SimpleSpanProcessorModel().withExporter(exporter))
                    : new SpanProcessorModel().withBatch(new BatchSpanProcessorModel()
                            .withExporter(exporter)
                            .withScheduleDelay(durationMillis("otel.bsp.schedule.delay"))
                            .withMaxQueueSize(otelConfigProperties.getInt("otel.bsp.max.queue.size"))
                            .withMaxExportBatchSize(otelConfigProperties.getInt("otel.bsp.max.export.batch.size"))
                            .withExportTimeout(durationMillis("otel.bsp.export.timeout"))));
        }
        return processors;
    }

    private MeterProviderModel buildMeterProvider() {
        // One periodic reader per exporter, as autoconfigure creates them. Unlike the bsp/blrp durations, the
        // interval reads the typed config with Quarkus duration semantics, so a bare otel.metric.export.interval
        // has no effect - as on the legacy path, where the typed value always replaced it.
        int intervalMillis = Math.toIntExact(runtimeConfig.metric().exportInterval().toMillis());
        List<MetricReaderModel> readers = new ArrayList<>();
        for (String name : exporterNames("quarkus.otel.metrics.exporter",
                otelConfigProperties.getList("otel.metrics.exporter"))) {
            readers.add(new MetricReaderModel().withPeriodic(new PeriodicMetricReaderModel()
                    .withExporter(buildMetricExporterModel(name, exporterRuntimeConfig.metrics()))
                    .withInterval(intervalMillis)));
        }
        return readers.isEmpty() ? new MeterProviderModel() : new MeterProviderModel().withReaders(readers);
    }

    private LoggerProviderModel buildLoggerProvider() {
        // Matches autoconfigure's LoggerProviderConfiguration: log record limits share the otel.attribute.* keys.
        LogRecordLimitsModel limits = new LogRecordLimitsModel()
                .withAttributeCountLimit(otelConfigProperties.getInt("otel.attribute.count.limit"));
        Integer valueLengthLimit = otelConfigProperties.getInt("otel.attribute.value.length.limit");
        if (valueLengthLimit != null) {
            limits.withAttributeValueLengthLimit(valueLengthLimit);
        }

        LoggerProviderModel loggerProvider = new LoggerProviderModel().withLimits(limits);
        List<LogRecordProcessorModel> processors = new ArrayList<>();
        for (String name : exporterNames("quarkus.otel.logs.exporter",
                otelConfigProperties.getList("otel.logs.exporter"))) {
            LogRecordExporterModel exporter = buildLogExporterModel(name, exporterRuntimeConfig.logs());
            // quarkus.otel.simple only affects logs when a default-qualified LogRecordExporter bean is resolvable:
            // the legacy SimpleLogRecordProcessorCustomizer replaced the batch processor with a simple one wrapping
            // that bean and did nothing otherwise. The console-type exporters stay simple regardless, as
            // autoconfigure creates them.
            boolean simple = SIMPLE_PROCESSOR_EXPORTERS.contains(name)
                    || (buildConfig.simple() && cdiLogRecordExporterResolvable);
            processors.add(simple
                    ? new LogRecordProcessorModel().withSimple(new SimpleLogRecordProcessorModel().withExporter(exporter))
                    : new LogRecordProcessorModel().withBatch(new BatchLogRecordProcessorModel()
                            .withExporter(exporter)
                            .withScheduleDelay(durationMillis("otel.blrp.schedule.delay"))
                            .withMaxQueueSize(otelConfigProperties.getInt("otel.blrp.max.queue.size"))
                            .withMaxExportBatchSize(otelConfigProperties.getInt("otel.blrp.max.export.batch.size"))
                            .withExportTimeout(durationMillis("otel.blrp.export.timeout"))));
        }
        return processors.isEmpty() ? loggerProvider : loggerProvider.withProcessors(processors);
    }

    // In these properties a bare number means milliseconds, which getDuration preserves; Quarkus's own Duration
    // converter reads a bare number as seconds, so the typed values must not be used here.
    private int durationMillis(String name) {
        final Duration duration = otelConfigProperties.getDuration(name);
        if (duration != null) {
            return Math.toIntExact(duration.toMillis());
        }
        // This should never happen, but we'll be safe
        throw new ConfigurationException("%s is missing from the resolved configuration".formatted(name));
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
                .withName(key.getKey())
                .withValue(value)
                .withType(type);
    }

    private static PropagatorModel buildPropagator(List<String> propagators) {
        return new PropagatorModel().withCompositeList(String.join(",", propagators));
    }

    private static String addSlashIfNecessary(String uri) {
        return uri.startsWith("/") ? uri : "/" + uri;
    }

    private static SpanExporterModel buildSpanExporterModel(String name, OtlpExporterConfig signalConfig) {
        if (!ExporterType.Constants.OTLP_VALUE.equals(name)) {
            // "cdi" or a custom exporter resolved by name via the ComponentProvider SPI (or a legacy
            // ConfigurableSpanExporterProvider).
            return new SpanExporterModel().withAdditionalProperty(name, new SpanExporterPropertyModel());
        }
        OtlpSettings settings = otlpSettings(signalConfig, VertxHttpSender.TRACES_PATH);
        return settings.grpc()
                ? new SpanExporterModel().withOtlpGrpc(otlpGrpc(settings))
                : new SpanExporterModel().withOtlpHttp(otlpHttp(settings));
    }

    private static PushMetricExporterModel buildMetricExporterModel(String name, OtlpExporterConfig signalConfig) {
        if (!ExporterType.Constants.OTLP_VALUE.equals(name)) {
            return new PushMetricExporterModel().withAdditionalProperty(name, new PushMetricExporterPropertyModel());
        }
        OtlpSettings settings = otlpSettings(signalConfig, VertxHttpSender.METRICS_PATH);
        return settings.grpc()
                ? new PushMetricExporterModel().withOtlpGrpc(new OtlpGrpcMetricExporterModel()
                        .withEndpoint(settings.endpoint())
                        .withTimeout(settings.timeout())
                        .withCompression(settings.compression())
                        .withHeaders(settings.headers()))
                : new PushMetricExporterModel().withOtlpHttp(new OtlpHttpMetricExporterModel()
                        .withEndpoint(settings.endpoint())
                        .withTimeout(settings.timeout())
                        .withCompression(settings.compression())
                        .withHeaders(settings.headers()));
    }

    private static LogRecordExporterModel buildLogExporterModel(String name, OtlpExporterConfig signalConfig) {
        if (!ExporterType.Constants.OTLP_VALUE.equals(name)) {
            return new LogRecordExporterModel().withAdditionalProperty(name, new LogRecordExporterPropertyModel());
        }
        OtlpSettings settings = otlpSettings(signalConfig, VertxHttpSender.LOGS_PATH);
        return settings.grpc()
                ? new LogRecordExporterModel().withOtlpGrpc(otlpGrpc(settings))
                : new LogRecordExporterModel().withOtlpHttp(otlpHttp(settings));
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
                .withEndpoint(settings.endpoint())
                .withTimeout(settings.timeout())
                .withCompression(settings.compression())
                .withHeaders(settings.headers());
    }

    private static OtlpHttpExporterModel otlpHttp(OtlpSettings settings) {
        return new OtlpHttpExporterModel()
                .withEndpoint(settings.endpoint())
                .withTimeout(settings.timeout())
                .withCompression(settings.compression())
                .withHeaders(settings.headers());
    }

    // Mirrors autoconfigure's *ExporterConfiguration: duplicate names collapse, "none" disables the signal and may not
    // be combined with other exporters. The values come from the merged flat otel.* view; the error message names
    // the documented quarkus.otel.* property.
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
    // quarkus.otel.exporter.otlp.<signal>.* already falls back to quarkus.otel.exporter.otlp.* and then to the
    // OTel defaults via OtlpExporterConfigBuilder's FallbackConfigSourceInterceptor; no cascade logic is needed here.
    // TLS and the metrics-specific temporality/histogram-aggregation settings are not mapped here yet.
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
                    .withName(parts[0].trim())
                    .withValue(parts[1].trim()));
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
