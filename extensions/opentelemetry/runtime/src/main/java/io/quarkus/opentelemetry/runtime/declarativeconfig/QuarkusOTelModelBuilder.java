package io.quarkus.opentelemetry.runtime.declarativeconfig;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.AlwaysOffSamplerModel;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.AlwaysOnSamplerModel;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.AttributeLimitsModel;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.AttributeNameValueModel;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.AttributeTypeModel;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.BatchLogRecordProcessorModel;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.BatchSpanProcessorModel;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.CardinalityLimitsModel;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.ExemplarFilterModel;
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
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.ResourceModel;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.SamplerModel;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.SimpleLogRecordProcessorModel;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.SimpleSpanProcessorModel;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.SpanExporterModel;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.SpanLimitsModel;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.SpanProcessorModel;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.TextMapPropagatorModel;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.TraceIdRatioBasedSamplerModel;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.TracerProviderModel;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigurationException;
import io.opentelemetry.sdk.autoconfigure.spi.internal.DefaultConfigProperties;
import io.opentelemetry.sdk.resources.Resource;
import io.quarkus.opentelemetry.runtime.config.build.ExporterType;
import io.quarkus.opentelemetry.runtime.config.build.OTelBuildConfig;
import io.quarkus.opentelemetry.runtime.config.runtime.OTelRuntimeConfig;
import io.quarkus.opentelemetry.runtime.config.runtime.TracesRuntimeConfig;

/**
 * Builds the {@link OpenTelemetryConfigurationModel} from the resolved configuration (the typed
 * {@code quarkus.otel.*} mappings plus the merged flat {@code otel.*} view the recorder computes). The model is the
 * input of upstream's {@code DeclarativeConfiguration.create(model, componentLoader)}, or of the stand-in that
 * replaces it until the swap.
 * <p>
 * {@code quarkus.otel.*.exporter} defaults to {@code cdi}: Quarkus's own OTLP exporter (currently built on Vert.x,
 * an implementation detail this mapping doesn't depend on), and any exporter contributed by a Quarkiverse
 * extension, is registered as a {@code SpanExporter}/{@code MetricExporter}/
 * {@code LogRecordExporter} CDI bean, looked up generically today via
 * {@code io.quarkus.opentelemetry.runtime.tracing.spi.SpanExporterCDIProvider} (and its metric/log equivalents).
 * The model names it {@link ExporterType.Constants#CDI_VALUE}, which resolves to those same
 * {@code Configurable*ExporterProvider} classes, so none of the existing CDI bean wiring (including Quarkiverse
 * exporters) needs to change.
 * </p>
 * <p>
 * {@code exporter=otlp} is the escape hatch for using the real upstream {@code opentelemetry-exporter-otlp} library
 * directly (it requires the user to add an HTTP sender dependency, since Quarkus doesn't ship one). Like every other
 * exporter it is an extension node resolved by name, so upstream's provider reads the flat
 * {@code otel.exporter.otlp.*} properties as it did on the autoconfigure path.
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
     * Exporter extension node for every span and log record exporter: its {@code exporters} entry lists one node per
     * exporter name, and the exporters are customized one by one by the {@code AutoConfigurationCustomizerProvider}
     * exporter customizers, then composed. Autoconfigure composed every exporter other than
     * {@code console}/{@code logging} into one batch processor, and the declarative model has no composite exporter
     * and no exporter customizer.
     */
    public static final String QUARKUS_COMPOSITE = "quarkus_composite";

    /**
     * Pull metric reader extension node for one configured metric exporter: its {@code exporter} entry names the
     * exporter and {@code interval} holds the export interval in milliseconds. The declarative model has no hook for
     * the executor a periodic reader runs on, and Quarkus runs it on the container-managed one.
     */
    public static final String QUARKUS_PERIODIC = "quarkus_periodic";

    /**
     * Pull metric reader extension node for the Dev UI's in-memory reader. It has no configuration.
     */
    public static final String QUARKUS_DEVUI = "quarkus_devui";

    /**
     * Extension node for a component the application supplies as a CDI bean. It has no configuration.
     */
    public static final String QUARKUS_CDI = "quarkus_cdi";

    /**
     * Extension node for a customized sampler or propagator.
     * <p>
     * As a sampler, it applies the {@code AutoConfigurationCustomizerProvider} sampler customizers to the sampler in
     * its {@code delegate} entry. It is the outermost sampler node, since autoconfigure ran those customizers after
     * the Quarkus wiring.
     * </p>
     * <p>
     * As a {@code composite} propagator entry, it builds the propagator its {@code name} entry names and applies the
     * {@code TextMapPropagatorCustomizer} beans, then the {@code AutoConfigurationCustomizerProvider} propagator
     * customizers.
     * </p>
     */
    public static final String QUARKUS_CUSTOMIZED = "quarkus_customized";

    /**
     * Span processor extension node for a processor that drops every span. It has no configuration. Tracing stays
     * on, so spans keep valid ids for log correlation and context propagation, but nothing is exported.
     */
    public static final String QUARKUS_DROP_ALL = "quarkus_drop_all";

    /**
     * Span and log record processor extension node for the simple processor {@code quarkus.otel.simple} selects. The
     * span node's {@code exporter} entry holds the exporter node; the log record node has no configuration and uses
     * the default {@code LogRecordExporter} bean. Unlike the model's {@code simple} processor, it records no processor
     * self-metrics, as on the autoconfigure path.
     */
    public static final String QUARKUS_SIMPLE = "quarkus_simple";

    /**
     * The declarative configuration schema version. Only upstream's {@code DeclarativeConfiguration} checks it,
     * warning when it differs from the version the SDK expects.
     */
    private static final String FILE_FORMAT = "1.2";

    private static final Set<String> SIMPLE_PROCESSOR_EXPORTERS = Set.of("console", "logging");

    private final OTelBuildConfig buildConfig;
    private final OTelRuntimeConfig runtimeConfig;
    private final ConfigProperties otelConfigProperties;

    private Resource resource = Resource.empty();
    private Set<String> nonApplicationUriTargets = Set.of();
    private Set<String> staticResourceTargets = Set.of();
    private boolean cdiSpanProcessorsPresent;
    private boolean devUiMetricReaderPresent;
    private boolean cdiSamplerPresent;
    private boolean samplerCustomized;
    private boolean cdiIdGeneratorPresent;
    private boolean cdiSpanExportersPresent = true;
    private boolean defaultLogRecordExporterPresent;
    private boolean propagatorsCustomized;

    private QuarkusOTelModelBuilder(OTelBuildConfig buildConfig, OTelRuntimeConfig runtimeConfig,
            ConfigProperties otelConfigProperties) {
        this.buildConfig = buildConfig;
        this.runtimeConfig = runtimeConfig;
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
            ConfigProperties otelConfigProperties) {
        return new QuarkusOTelModelBuilder(buildConfig, runtimeConfig, otelConfigProperties);
    }

    /**
     * Returns the propagator names of {@code otel.propagators}, as upstream reads a {@code composite_list}: trimmed,
     * without duplicates, empty names and {@code none}. Autoconfigure removed duplicate propagator instances instead,
     * after customizing each name.
     *
     * @param propagators the {@code otel.propagators} list, not {@code null}
     * @return the names in their first order, or an empty list
     * @throws ConfigurationException if the list contains {@code none} along with other propagators
     */
    public static List<String> propagatorNames(List<String> propagators) {
        Set<String> names = new LinkedHashSet<>();
        boolean none = false;
        int count = 0;
        for (String propagator : propagators) {
            String name = propagator.trim();
            if (name.isEmpty()) {
                continue;
            }
            count++;
            if (ExporterType.Constants.NONE_VALUE.equals(name)) {
                none = true;
            } else {
                names.add(name);
            }
        }
        // As autoconfigure's PropagatorConfiguration, which counts every name, a repeated none included
        if (none && count > 1) {
            throw new ConfigurationException("otel.propagators contains 'none' along with other propagators");
        }
        return List.copyOf(names);
    }

    /**
     * Sets the resource the model carries, with its attributes and schema URL. The recorder supplies the fully
     * merged resource. Attributes of type {@link io.opentelemetry.api.common.AttributeType#VALUE VALUE} have no
     * model representation and are left out of the model.
     */
    public QuarkusOTelModelBuilder resource(Resource resource) {
        this.resource = resource;
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
     * Whether the application contributes {@code SpanProcessor} beans. When tracing is enabled, a {@link #QUARKUS_CDI}
     * processor then follows the configured processors, and {@code quarkus.otel.simple} keeps the batch processor, as
     * the legacy {@code SpanProcessorCustomizer} only swapped it for a simple one when no such beans existed.
     */
    public QuarkusOTelModelBuilder cdiSpanProcessorsPresent(boolean cdiSpanProcessorsPresent) {
        this.cdiSpanProcessorsPresent = cdiSpanProcessorsPresent;
        return this;
    }

    /**
     * Whether the Dev UI's in-memory metric reader is available: a {@link #QUARKUS_DEVUI} reader is then added after
     * the exporter readers, without a cardinality limit.
     */
    public QuarkusOTelModelBuilder devUiMetricReaderPresent(boolean devUiMetricReaderPresent) {
        this.devUiMetricReaderPresent = devUiMetricReaderPresent;
        return this;
    }

    /**
     * Whether the application contributes a {@code Sampler} bean: when tracing is enabled, a {@link #QUARKUS_CDI}
     * sampler then replaces the configured one, inside the drop targets.
     */
    public QuarkusOTelModelBuilder cdiSamplerPresent(boolean cdiSamplerPresent) {
        this.cdiSamplerPresent = cdiSamplerPresent;
        return this;
    }

    /**
     * Whether an {@code AutoConfigurationCustomizerProvider} registers a sampler customizer: the sampler is then
     * wrapped in a {@link #QUARKUS_CUSTOMIZED} node, also when tracing is disabled.
     */
    public QuarkusOTelModelBuilder samplerCustomized(boolean samplerCustomized) {
        this.samplerCustomized = samplerCustomized;
        return this;
    }

    /**
     * Whether the application contributes an {@code IdGenerator} bean: when tracing is enabled, the tracer provider's
     * {@code id_generator} is then a {@link #QUARKUS_CDI} node.
     */
    public QuarkusOTelModelBuilder cdiIdGeneratorPresent(boolean cdiIdGeneratorPresent) {
        this.cdiIdGeneratorPresent = cdiIdGeneratorPresent;
        return this;
    }

    /**
     * Whether the application contributes {@code SpanExporter} beans, {@code true} unless set. Without them the
     * {@code cdi} exporter is the no-op exporter, and when it is the only batched exporter and there are no
     * {@code SpanProcessor} beans, a {@link #QUARKUS_DROP_ALL} processor replaces the batch processor.
     */
    public QuarkusOTelModelBuilder cdiSpanExportersPresent(boolean cdiSpanExportersPresent) {
        this.cdiSpanExportersPresent = cdiSpanExportersPresent;
        return this;
    }

    /**
     * Whether a default-qualified {@code LogRecordExporter} bean is resolvable. With {@code quarkus.otel.simple}, a
     * {@link #QUARKUS_SIMPLE} log record processor then replaces the batch processor, as the legacy
     * {@code SimpleLogRecordProcessorCustomizer} did; without the bean, the batch processor is kept.
     */
    public QuarkusOTelModelBuilder defaultLogRecordExporterPresent(boolean defaultLogRecordExporterPresent) {
        this.defaultLogRecordExporterPresent = defaultLogRecordExporterPresent;
        return this;
    }

    /**
     * Whether {@code TextMapPropagatorCustomizer} beans exist or an {@code AutoConfigurationCustomizerProvider}
     * registers a propagator customizer: each propagator is then a {@link #QUARKUS_CUSTOMIZED} {@code composite}
     * entry instead of a {@code composite_list} name.
     */
    public QuarkusOTelModelBuilder propagatorsCustomized(boolean propagatorsCustomized) {
        this.propagatorsCustomized = propagatorsCustomized;
        return this;
    }

    /**
     * Returns whether {@link #build()} replaces the batch processor of the {@code cdi} traces exporter with a
     * {@link #QUARKUS_DROP_ALL} processor. That is the case when tracing is enabled, {@code cdi} is the only batched
     * traces exporter, {@code quarkus.otel.simple} is off, and there are neither {@code SpanExporter} nor
     * {@code SpanProcessor} beans.
     *
     * @return {@code true} if the {@code cdi} exporter's spans are dropped
     * @throws IllegalArgumentException if the traces exporters combine {@code none} with other exporters
     */
    public boolean dropsCdiSpans() {
        if (!buildConfig.traces().enabled().orElse(true) || cdiSpanProcessorsPresent || cdiSpanExportersPresent
                || buildConfig.simple()) {
            return false;
        }
        List<String> batched = new ArrayList<>();
        for (String name : exporterNames("otel.traces.exporter")) {
            if (!SIMPLE_PROCESSOR_EXPORTERS.contains(name)) {
                batched.add(name);
            }
        }
        return batched.equals(List.of(ExporterType.Constants.CDI_VALUE));
    }

    public OpenTelemetryConfigurationModel build() {
        OpenTelemetryConfigurationModel model = new OpenTelemetryConfigurationModel()
                .setFileFormat(FILE_FORMAT)
                .setDisabled(otelConfigProperties.getBoolean("otel.sdk.disabled", false))
                .setAttributeLimits(buildAttributeLimits())
                .setResource(buildResource())
                .setPropagator(buildPropagator());

        // A null tracer_provider makes the SDK fall back to a default provider sampling parentbased(always_on), so
        // disabled traces get an explicit always_off sampler instead, matching the legacy SamplerCustomizer.
        model.setTracerProvider(buildConfig.traces().enabled().orElse(true)
                ? buildTracerProvider()
                : new TracerProviderModel().setSampler(
                        customized(new SamplerModel().setAlwaysOff(new AlwaysOffSamplerModel()))));
        if (buildConfig.metrics().enabled().orElse(true)) {
            model.setMeterProvider(buildMeterProvider());
        }
        if (buildConfig.logs().enabled().orElse(true)) {
            model.setLoggerProvider(buildLoggerProvider());
        }

        return model;
    }

    private AttributeLimitsModel buildAttributeLimits() {
        AttributeLimitsModel limits = new AttributeLimitsModel()
                .setAttributeCountLimit(otelConfigProperties.getInt("otel.attribute.count.limit"));
        Integer valueLengthLimit = otelConfigProperties.getInt("otel.attribute.value.length.limit");
        if (valueLengthLimit != null) {
            limits.setAttributeValueLengthLimit(valueLengthLimit);
        }
        return limits;
    }

    private ResourceModel buildResource() {
        List<AttributeNameValueModel> attributes = new ArrayList<>();
        resource.getAttributes().forEach((key, value) -> {
            AttributeNameValueModel attribute = toAttributeNameValueModel(key, value);
            if (attribute != null) {
                attributes.add(attribute);
            }
        });
        return new ResourceModel()
                .setAttributes(attributes)
                .setSchemaUrl(resource.getSchemaUrl());
    }

    private TracerProviderModel buildTracerProvider() {
        // As autoconfigure: the general otel.attribute.count.limit also limits each event's and link's attributes,
        // the span-specific otel.span.attribute.count.limit does not
        Integer attributeCountLimit = otelConfigProperties.getInt("otel.attribute.count.limit");
        SpanLimitsModel limits = new SpanLimitsModel()
                .setAttributeCountLimit(otelConfigProperties.getInt("otel.span.attribute.count.limit"))
                .setEventCountLimit(otelConfigProperties.getInt("otel.span.event.count.limit"))
                .setLinkCountLimit(otelConfigProperties.getInt("otel.span.link.count.limit"))
                .setEventAttributeCountLimit(attributeCountLimit)
                .setLinkAttributeCountLimit(attributeCountLimit);
        Integer attributeValueLengthLimit = otelConfigProperties.getInt("otel.span.attribute.value.length.limit");
        if (attributeValueLengthLimit != null) {
            limits.setAttributeValueLengthLimit(attributeValueLengthLimit);
        }

        return new TracerProviderModel()
                .setLimits(limits)
                .setSampler(buildSampler(runtimeConfig.traces()))
                .setIdGenerator(cdiIdGeneratorPresent
                        ? new IdGeneratorModel().setExtensionProperty(QUARKUS_CDI, Map.of())
                        : null)
                .setProcessors(buildSpanProcessors());
    }

    // As the legacy SamplerCustomizer: a CDI Sampler bean replaces the configured sampler, the drop targets wrap
    // it, and the SPI sampler customizers run last
    private SamplerModel buildSampler(TracesRuntimeConfig tracesRuntimeConfig) {
        // Built even when the bean replaces it, so an invalid sampler argument fails as autoconfigure failed
        SamplerModel configured = buildEffectiveSampler();
        return customized(buildDropTargetsSampler(tracesRuntimeConfig, cdiSamplerPresent
                ? new SamplerModel().setExtensionProperty(QUARKUS_CDI, Map.of())
                : configured));
    }

    private SamplerModel buildDropTargetsSampler(TracesRuntimeConfig tracesRuntimeConfig, SamplerModel sampler) {

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

    private SamplerModel buildEffectiveSampler() {
        // The autoconfigure sampler names, as SamplerType lists them
        String sampler = otelConfigProperties.getString("otel.traces.sampler", "parentbased_traceidratio");
        return switch (sampler) {
            case "always_on" -> new SamplerModel().setAlwaysOn(new AlwaysOnSamplerModel());
            case "always_off" -> new SamplerModel().setAlwaysOff(new AlwaysOffSamplerModel());
            case "traceidratio" -> new SamplerModel().setTraceIdRatioBased(
                    new TraceIdRatioBasedSamplerModel().setRatio(ratio()));
            case "parentbased_always_on" -> new SamplerModel().setParentBased(
                    new ParentBasedSamplerModel().setRoot(new SamplerModel().setAlwaysOn(new AlwaysOnSamplerModel())));
            case "parentbased_always_off" -> new SamplerModel().setParentBased(
                    new ParentBasedSamplerModel().setRoot(new SamplerModel().setAlwaysOff(new AlwaysOffSamplerModel())));
            case "parentbased_traceidratio" -> new SamplerModel().setParentBased(
                    new ParentBasedSamplerModel().setRoot(new SamplerModel()
                            .setTraceIdRatioBased(
                                    new TraceIdRatioBasedSamplerModel().setRatio(ratio()))));
            // As autoconfigure: parentBased around the sampler the jaeger_remote provider supplies
            case "parentbased_jaeger_remote" -> new SamplerModel().setParentBased(new ParentBasedSamplerModel()
                    .setRoot(new SamplerModel().setExtensionProperty("jaeger_remote", Map.of())));
            // Custom/SPI sampler name, resolved by name like every other extension node
            default -> new SamplerModel().setExtensionProperty(sampler, Map.of());
        };
    }

    private SamplerModel customized(SamplerModel sampler) {
        return samplerCustomized
                ? new SamplerModel().setExtensionProperty(QUARKUS_CUSTOMIZED, Map.of("delegate", sampler))
                : sampler;
    }

    // Only parsed for the ratio-based samplers: a custom sampler may use a non-numeric sampler-arg.
    private double ratio() {
        return otelConfigProperties.getDouble("otel.traces.sampler.arg", 0.1d);
    }

    private List<SpanProcessorModel> buildSpanProcessors() {
        List<SpanProcessorModel> processors = new ArrayList<>();
        List<String> configuredNames = otelConfigProperties.getList("otel.traces.exporter");
        List<String> composed = new ArrayList<>();
        for (String name : exporterNames("otel.traces.exporter")) {
            if (SIMPLE_PROCESSOR_EXPORTERS.contains(name)) {
                processors.add(new SpanProcessorModel().setSimple(new SimpleSpanProcessorModel()
                        .setExporter(new SpanExporterModel().setExtensionProperty(QUARKUS_COMPOSITE,
                                compositeNode(List.of(name))))));
            } else {
                composed.add(name);
            }
        }
        if (!composed.isEmpty()) {
            SpanExporterModel exporter = new SpanExporterModel().setExtensionProperty(QUARKUS_COMPOSITE,
                    compositeNode(composed));
            // quarkus.otel.simple has no effect when the application contributes SpanProcessor beans: the legacy
            // SpanProcessorCustomizer only swapped batch for simple when no SpanProcessor beans existed.
            if (buildConfig.simple() && !cdiSpanProcessorsPresent) {
                processors.add(new SpanProcessorModel().setExtensionProperty(QUARKUS_SIMPLE,
                        Map.of("exporter", exporter.getExtensionProperties())));
            } else if (dropsCdiSpans()) {
                // As the legacy SpanProcessorCustomizer, which replaced the batch processor around the no-op
                // exporter that cdi is without SpanExporter beans
                processors.add(new SpanProcessorModel().setExtensionProperty(QUARKUS_DROP_ALL, Map.of()));
            } else {
                processors.add(new SpanProcessorModel().setBatch(new BatchSpanProcessorModel()
                        .setExporter(exporter)
                        .setScheduleDelay(durationMillis("otel.bsp.schedule.delay"))
                        .setMaxQueueSize(otelConfigProperties.getInt("otel.bsp.max.queue.size"))
                        .setMaxExportBatchSize(otelConfigProperties.getInt("otel.bsp.max.export.batch.size"))
                        .setExportTimeout(durationMillis("otel.bsp.export.timeout"))));
            }
        } else if (!cdiSpanProcessorsPresent && configuredNames.contains(ExporterType.Constants.NONE_VALUE)) {
            // As the legacy TracerProviderCustomizer: with no exporter there is no processor, and tracing is on,
            // so one is needed for the SDK to hand out real tracers
            processors.add(new SpanProcessorModel().setExtensionProperty(QUARKUS_DROP_ALL, Map.of()));
        }
        // As the legacy TracerProviderCustomizer, which added the beans after the configured processors
        if (cdiSpanProcessorsPresent) {
            processors.add(new SpanProcessorModel().setExtensionProperty(QUARKUS_CDI, Map.of()));
        }
        return processors;
    }

    private MeterProviderModel buildMeterProvider() {
        // One periodic reader per exporter, as autoconfigure creates them, each a quarkus_periodic pull node.
        // Unlike the bsp/blrp durations, the interval reads the typed config with Quarkus duration semantics, so a
        // bare otel.metric.export.interval has no effect - as on the legacy path, where the typed value always
        // replaced it.
        int intervalMillis = Math.toIntExact(runtimeConfig.metric().exportInterval().toMillis());
        // Matches autoconfigure's MeterProviderConfiguration: one cardinality limit for every configured reader
        Integer cardinalityLimit = otelConfigProperties.getInt("otel.java.metrics.cardinality.limit");
        if (cardinalityLimit != null && cardinalityLimit < 1) {
            throw new ConfigurationException("otel.java.metrics.cardinality.limit must be >= 1");
        }
        List<MetricReaderModel> readers = new ArrayList<>();
        for (String name : exporterNames("otel.metrics.exporter")) {
            Map<String, Object> periodic = new LinkedHashMap<>();
            periodic.put("exporter", Map.of(name, Map.of()));
            periodic.put("interval", intervalMillis);
            readers.add(new MetricReaderModel().setPull(new PullMetricReaderModel()
                    .setExporter(new PullMetricExporterModel().setExtensionProperty(QUARKUS_PERIODIC, periodic))
                    .setCardinalityLimits(cardinalityLimit == null ? null
                            : new CardinalityLimitsModel().setDefault(cardinalityLimit))));
        }
        if (devUiMetricReaderPresent) {
            readers.add(new MetricReaderModel().setPull(new PullMetricReaderModel()
                    .setExporter(new PullMetricExporterModel().setExtensionProperty(QUARKUS_DEVUI, Map.of()))));
        }
        MeterProviderModel meterProvider = new MeterProviderModel().setExemplarFilter(buildExemplarFilter());
        return readers.isEmpty() ? meterProvider : meterProvider.setReaders(readers);
    }

    // Matches autoconfigure's MeterProviderConfiguration, which falls back to trace_based for unknown values.
    private ExemplarFilterModel buildExemplarFilter() {
        return switch (otelConfigProperties.getString("otel.metrics.exemplar.filter", "trace_based")
                .toLowerCase(Locale.ROOT)) {
            case "always_off" -> ExemplarFilterModel.ALWAYS_OFF;
            case "always_on" -> ExemplarFilterModel.ALWAYS_ON;
            default -> ExemplarFilterModel.TRACE_BASED;
        };
    }

    private LoggerProviderModel buildLoggerProvider() {
        // Matches autoconfigure's LoggerProviderConfiguration: log record limits share the otel.attribute.* keys.
        LogRecordLimitsModel limits = new LogRecordLimitsModel()
                .setAttributeCountLimit(otelConfigProperties.getInt("otel.attribute.count.limit"));
        Integer valueLengthLimit = otelConfigProperties.getInt("otel.attribute.value.length.limit");
        if (valueLengthLimit != null) {
            limits.setAttributeValueLengthLimit(valueLengthLimit);
        }

        LoggerProviderModel loggerProvider = new LoggerProviderModel().setLimits(limits);
        List<LogRecordProcessorModel> processors = new ArrayList<>();
        List<String> composed = new ArrayList<>();
        for (String name : exporterNames("otel.logs.exporter")) {
            if (SIMPLE_PROCESSOR_EXPORTERS.contains(name)) {
                processors.add(new LogRecordProcessorModel().setSimple(new SimpleLogRecordProcessorModel()
                        .setExporter(new LogRecordExporterModel().setExtensionProperty(QUARKUS_COMPOSITE,
                                compositeNode(List.of(name))))));
            } else {
                composed.add(name);
            }
        }
        if (!composed.isEmpty()) {
            LogRecordExporterModel exporter = new LogRecordExporterModel().setExtensionProperty(QUARKUS_COMPOSITE,
                    compositeNode(composed));
            // As the legacy SimpleLogRecordProcessorCustomizer: the default LogRecordExporter bean alone replaces the
            // batch processor and its exporters
            processors.add(buildConfig.simple() && defaultLogRecordExporterPresent
                    ? new LogRecordProcessorModel().setExtensionProperty(QUARKUS_SIMPLE, Map.of())
                    : new LogRecordProcessorModel().setBatch(new BatchLogRecordProcessorModel()
                            .setExporter(exporter)
                            .setScheduleDelay(durationMillis("otel.blrp.schedule.delay"))
                            .setMaxQueueSize(otelConfigProperties.getInt("otel.blrp.max.queue.size"))
                            .setMaxExportBatchSize(otelConfigProperties.getInt("otel.blrp.max.export.batch.size"))
                            .setExportTimeout(durationMillis("otel.blrp.export.timeout"))));
        }
        return processors.isEmpty() ? loggerProvider : loggerProvider.setProcessors(processors);
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
            // The type has no mapping to the AttributeTypeModel, e.g. VALUE. Nothing is lost: the SDK uses the
            // merged Resource the recorder supplies, not one rebuilt from the model.
            default -> null;
        };
        if (type == null) {
            return null;
        }
        return new AttributeNameValueModel()
                .setName(key.getKey())
                .setValue(value)
                .setType(type);
    }

    private PropagatorModel buildPropagator() {
        List<String> propagators = otelConfigProperties.getList("otel.propagators");
        List<String> names = propagatorNames(propagators);
        if (!propagatorsCustomized || names.isEmpty()) {
            return new PropagatorModel().setCompositeList(String.join(",", propagators));
        }
        // Upstream de-duplicates composite_list names but not composite entries
        List<TextMapPropagatorModel> composite = new ArrayList<>();
        for (String name : names) {
            composite.add(new TextMapPropagatorModel().setExtensionProperty(QUARKUS_CUSTOMIZED, Map.of("name", name)));
        }
        return new PropagatorModel().setComposite(composite);
    }

    private static String addSlashIfNecessary(String uri) {
        return uri.startsWith("/") ? uri : "/" + uri;
    }

    // The QUARKUS_COMPOSITE node of the exporters names lists. Every exporter, otlp included, is resolved by name, as
    // autoconfigure resolved the otel.*.exporter names: "otlp" reaches upstream's Configurable*ExporterProvider,
    // which reads the flat otel.exporter.otlp.* properties as on the autoconfigure path, and "cdi" the Quarkus CDI
    // exporters.
    private static Map<String, Object> compositeNode(List<String> names) {
        List<Map<String, Object>> exporters = new ArrayList<>();
        for (String name : names) {
            exporters.add(Map.of(name, Map.of()));
        }
        return Map.of("exporters", List.copyOf(exporters));
    }

    // Mirrors autoconfigure's *ExporterConfiguration: upstream's getSet rejects duplicate names, "none" disables the
    // signal and may not be combined with other exporters. The values come from the merged flat otel.* view, in
    // their configured order.
    private Set<String> exporterNames(String flatName) {
        DefaultConfigProperties.getSet(otelConfigProperties, flatName);
        Set<String> names = new LinkedHashSet<>(otelConfigProperties.getList(flatName));
        if (names.contains(ExporterType.Constants.NONE_VALUE)) {
            if (names.size() > 1) {
                throw new ConfigurationException(
                        flatName + " contains " + ExporterType.Constants.NONE_VALUE + " along with other exporters");
            }
            return Set.of();
        }
        return names;
    }

}
