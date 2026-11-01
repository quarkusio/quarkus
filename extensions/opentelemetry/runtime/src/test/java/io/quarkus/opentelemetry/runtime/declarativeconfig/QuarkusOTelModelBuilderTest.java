package io.quarkus.opentelemetry.runtime.declarativeconfig;

import static io.quarkus.opentelemetry.runtime.config.build.ExporterType.Constants.CDI_VALUE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.assertj.core.groups.Tuple;
import org.junit.jupiter.api.Test;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.AlwaysOffSamplerModel;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.AlwaysOnSamplerModel;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.BatchSpanProcessorModel;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.OpenTelemetryConfigurationModel;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.OtlpGrpcMetricExporterModel;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.OtlpHttpExporterModel;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.ParentBasedSamplerModel;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.SamplerModel;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.SamplerPropertyModel;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.SimpleSpanProcessorModel;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.SpanExporterModel;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.SpanProcessorModel;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.TraceIdRatioBasedSamplerModel;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.internal.DefaultConfigProperties;
import io.quarkus.opentelemetry.runtime.config.build.OTelBuildConfig;
import io.quarkus.opentelemetry.runtime.config.runtime.OTelRuntimeConfig;
import io.quarkus.opentelemetry.runtime.config.runtime.exporter.OtlpExporterConfigBuilder;
import io.quarkus.opentelemetry.runtime.config.runtime.exporter.OtlpExporterRuntimeConfig;
import io.smallrye.config.ConfigValue;
import io.smallrye.config.SmallRyeConfig;
import io.smallrye.config.SmallRyeConfigBuilder;

class QuarkusOTelModelBuilderTest {

    @Test
    void defaultConfigUsesCdiExporterBridge() {
        OpenTelemetryConfigurationModel model = buildModel(Map.of());

        assertThat(model.getFileFormat()).isEqualTo("1.1");
        assertThat(model.getDisabled()).isFalse();
        assertThat(model.getTracerProvider().getSampler()).isEqualTo(
                new SamplerModel().withParentBased(new ParentBasedSamplerModel()
                        .withRoot(new SamplerModel().withTraceIdRatioBased(
                                new TraceIdRatioBasedSamplerModel().withRatio(0.1d)))));

        // default exporter is "cdi": delegate to whatever SpanExporter CDI beans exist, same as today's
        // SpanExporterCDIProvider, rather than building anything OTLP-specific in the model.
        SpanExporterModel exporter = model.getTracerProvider().getProcessors().getFirst().getBatch().getExporter();
        assertThat(exporter.getOtlpHttp()).isNull();
        assertThat(exporter.getOtlpGrpc()).isNull();
        assertThat(exporter.getAdditionalProperties()).containsKey(CDI_VALUE);
    }

    @Test
    void sdkDisabledIsPropagated() {
        OpenTelemetryConfigurationModel model = buildModel(Map.of("quarkus.otel.sdk.disabled", "true"));

        assertThat(model.getDisabled()).isTrue();
    }

    @Test
    void resourceAttributesAreMapped() {
        OpenTelemetryConfigurationModel model = buildModel(Map.of(), Attributes.of(
                AttributeKey.stringKey("service.name"), "my-service"));

        assertThat(model.getResource().getAttributes())
                .extracting("name", "value")
                .containsExactly(Tuple.tuple("service.name", "my-service"));
    }

    @Test
    void alwaysOnSampler() {
        OpenTelemetryConfigurationModel model = buildModel(Map.of("quarkus.otel.traces.sampler", "always_on"));

        assertThat(model.getTracerProvider().getSampler())
                .isEqualTo(new SamplerModel().withAlwaysOn(new AlwaysOnSamplerModel()));
    }

    @Test
    void parentBasedAlwaysOffSampler() {
        OpenTelemetryConfigurationModel model = buildModel(
                Map.of("quarkus.otel.traces.sampler", "parentbased_always_off"));

        assertThat(model.getTracerProvider().getSampler()).isEqualTo(
                new SamplerModel().withParentBased(new ParentBasedSamplerModel()
                        .withRoot(new SamplerModel().withAlwaysOff(new AlwaysOffSamplerModel()))));
    }

    @Test
    void customSamplerNameIsKeptAsExtensionProperty() {
        OpenTelemetryConfigurationModel model = buildModel(Map.of("quarkus.otel.traces.sampler", "my_custom_sampler"));

        assertThat(model.getTracerProvider().getSampler())
                .isEqualTo(new SamplerModel().withAdditionalProperty("my_custom_sampler", new SamplerPropertyModel()));
    }

    @Test
    void customSamplerToleratesNonNumericSamplerArg() {
        OpenTelemetryConfigurationModel model = buildModel(Map.of(
                "quarkus.otel.traces.sampler", "my_custom_sampler",
                "quarkus.otel.traces.sampler.arg", "rate:5"));

        assertThat(model.getTracerProvider().getSampler())
                .isEqualTo(new SamplerModel().withAdditionalProperty("my_custom_sampler", new SamplerPropertyModel()));
    }

    @Test
    void customExporterNameIsKeptAsExtensionProperty() {
        OpenTelemetryConfigurationModel model = buildModel(Map.of("quarkus.otel.traces.exporter", "my-exporter"));

        assertThat(model.getTracerProvider().getProcessors()).hasSize(1);
        assertThat(model.getTracerProvider().getProcessors().getFirst().getBatch().getExporter().getAdditionalProperties())
                .containsOnlyKeys("my-exporter");
    }

    @Test
    void dropTargetsWrapSamplerWhenNonApplicationUrisSuppressed() {
        // suppress-non-application-uris defaults to true, so any candidate targets supplied by other
        // extensions (normally TracerRecorder.dropNonApplicationUriTargets) get dropped.
        OpenTelemetryConfigurationModel model = buildModel(Map.of("quarkus.otel.traces.sampler", "always_on"),
                Set.of("/q/health"), Set.of());

        SamplerModel wrapped = model.getTracerProvider().getSampler();
        SamplerPropertyModel dropTargets = wrapped.getAdditionalProperties().get(QuarkusOTelModelBuilder.QUARKUS_DROP_TARGETS);
        assertThat(dropTargets).isNotNull();
        assertThat(dropTargets.getAdditionalProperties())
                .containsEntry("delegate", new SamplerModel().withAlwaysOn(new AlwaysOnSamplerModel()))
                .containsEntry("drop_targets", Set.of("/q/health"));
    }

    @Test
    void dropTargetsIncludeStaticResourcesByDefault() {
        // include-static-resources defaults to false, meaning static resources ARE dropped by default.
        OpenTelemetryConfigurationModel model = buildModel(Map.of(
                "quarkus.otel.traces.sampler", "always_on",
                "quarkus.otel.traces.suppress-non-application-uris", "false"),
                Set.of(), Set.of("/static"));

        SamplerPropertyModel dropTargets = model.getTracerProvider().getSampler()
                .getAdditionalProperties().get(QuarkusOTelModelBuilder.QUARKUS_DROP_TARGETS);
        assertThat(dropTargets.getAdditionalProperties().get("drop_targets"))
                .asInstanceOf(InstanceOfAssertFactories.collection(String.class))
                .containsExactly("/static");
    }

    @Test
    void suppressApplicationUrisAreAddedToDropTargetsWithLeadingSlash() {
        OpenTelemetryConfigurationModel model = buildModel(Map.of(
                "quarkus.otel.traces.sampler", "always_on",
                "quarkus.otel.traces.suppress-non-application-uris", "false",
                "quarkus.otel.traces.include-static-resources", "true",
                "quarkus.otel.traces.suppress-application-uris", "foo,/bar"),
                Set.of(), Set.of());

        SamplerPropertyModel dropTargets = model.getTracerProvider().getSampler()
                .getAdditionalProperties().get(QuarkusOTelModelBuilder.QUARKUS_DROP_TARGETS);
        assertThat(dropTargets.getAdditionalProperties().get("drop_targets"))
                .asInstanceOf(InstanceOfAssertFactories.collection(String.class))
                .containsExactlyInAnyOrder("/foo", "/bar");
    }

    @Test
    void samplerIsNotWrappedWhenNoUrisAreSuppressed() {
        OpenTelemetryConfigurationModel model = buildModel(Map.of(
                "quarkus.otel.traces.sampler", "always_on",
                "quarkus.otel.traces.suppress-non-application-uris", "false",
                "quarkus.otel.traces.include-static-resources", "true"),
                Set.of("/q/health"), Set.of("/static"));

        assertThat(model.getTracerProvider().getSampler())
                .isEqualTo(new SamplerModel().withAlwaysOn(new AlwaysOnSamplerModel()));
    }

    @Test
    void otlpHttpTracesExporterUsesBatchProcessorByDefault() {
        OpenTelemetryConfigurationModel model = buildModel(Map.of(
                "quarkus.otel.traces.exporter", "otlp",
                "quarkus.otel.exporter.otlp.traces.endpoint", "http://collector:4318/",
                "quarkus.otel.exporter.otlp.traces.headers", "x-api-key=secret",
                "quarkus.otel.exporter.otlp.traces.compression", "gzip"));

        SpanProcessorModel processor = model.getTracerProvider().getProcessors().getFirst();
        assertThat(processor.getSimple()).isNull();
        BatchSpanProcessorModel batch = processor.getBatch();
        assertThat(batch.getScheduleDelay()).isEqualTo(5_000);
        assertThat(batch.getMaxQueueSize()).isEqualTo(2048);
        assertThat(batch.getMaxExportBatchSize()).isEqualTo(512);
        assertThat(batch.getExportTimeout()).isEqualTo(30_000);

        OtlpHttpExporterModel otlpHttp = batch.getExporter().getOtlpHttp();
        assertThat(otlpHttp).isNotNull();
        // the configured endpoint is a base URI; the declarative otlp_http endpoint carries the signal path
        assertThat(otlpHttp.getEndpoint()).isEqualTo("http://collector:4318/v1/traces");
        assertThat(otlpHttp.getTimeout()).isEqualTo(10_000);
        assertThat(otlpHttp.getCompression()).isEqualTo("gzip");
        assertThat(otlpHttp.getHeaders()).extracting("name", "value").containsExactly(Tuple.tuple("x-api-key", "secret"));
    }

    @Test
    void otlpGrpcExporterWithoutExplicitEndpointUsesGrpcDefault() {
        OpenTelemetryConfigurationModel model = buildModel(Map.of(
                "quarkus.otel.metrics.exporter", "otlp",
                "quarkus.otel.exporter.otlp.metrics.protocol", "grpc"));

        OtlpGrpcMetricExporterModel otlpGrpc = model.getMeterProvider().getReaders().getFirst().getPeriodic()
                .getExporter().getOtlpGrpc();
        assertThat(otlpGrpc).isNotNull();
        assertThat(otlpGrpc.getEndpoint()).isEqualTo("http://localhost:4317/");
    }

    @Test
    void simpleProcessorIsUsedWhenConfigured() {
        OpenTelemetryConfigurationModel model = buildModel(Map.of(
                "quarkus.otel.simple", "true",
                "quarkus.otel.traces.exporter", "otlp"));

        SpanProcessorModel processor = model.getTracerProvider().getProcessors().getFirst();
        assertThat(processor.getBatch()).isNull();
        assertThat(processor.getSimple()).isInstanceOf(SimpleSpanProcessorModel.class);
    }

    @Test
    void metricExportIntervalUsesQuarkusDurationSemantics() {
        // The interval reads the typed config, so Quarkus duration parsing applies: ISO-8601 works and a bare
        // number means seconds.
        OpenTelemetryConfigurationModel model = buildModel(Map.of(
                "quarkus.otel.metrics.exporter", "cdi",
                "quarkus.otel.metric.export.interval", "PT30S"));

        assertThat(model.getMeterProvider().getReaders().getFirst().getPeriodic().getInterval()).isEqualTo(30000);

        model = buildModel(Map.of(
                "quarkus.otel.metrics.exporter", "cdi",
                "quarkus.otel.metric.export.interval", "10"));

        assertThat(model.getMeterProvider().getReaders().getFirst().getPeriodic().getInterval()).isEqualTo(10000);
    }

    @Test
    void disabledSignalsAreOmitted() {
        OpenTelemetryConfigurationModel model = buildModel(Map.of(
                "quarkus.otel.metrics.enabled", "false",
                "quarkus.otel.logs.enabled", "false"));

        assertThat(model.getMeterProvider()).isNull();
        assertThat(model.getLoggerProvider()).isNull();
    }

    @Test
    void disabledTracesUseAlwaysOffSampler() {
        OpenTelemetryConfigurationModel model = buildModel(Map.of("quarkus.otel.traces.enabled", "false"));

        assertThat(model.getTracerProvider().getSampler())
                .isEqualTo(new SamplerModel().withAlwaysOff(new AlwaysOffSamplerModel()));
        assertThat(model.getTracerProvider().getProcessors()).isNullOrEmpty();
    }

    @Test
    void noneExporterProducesNoProcessors() {
        OpenTelemetryConfigurationModel model = buildModel(Map.of("quarkus.otel.traces.exporter", "none"));

        assertThat(model.getTracerProvider().getProcessors()).isEmpty();
    }

    @Test
    void eachExporterGetsItsOwnProcessor() {
        OpenTelemetryConfigurationModel model = buildModel(Map.of("quarkus.otel.traces.exporter", "cdi,console"));

        assertThat(model.getTracerProvider().getProcessors()).hasSize(2);
        assertThat(model.getTracerProvider().getProcessors().get(0).getBatch().getExporter().getAdditionalProperties())
                .containsOnlyKeys(CDI_VALUE);
        // console and logging always use a simple processor, as autoconfigure did
        assertThat(model.getTracerProvider().getProcessors().get(1).getSimple().getExporter().getAdditionalProperties())
                .containsOnlyKeys("console");
    }

    @Test
    void duplicateExportersCollapse() {
        OpenTelemetryConfigurationModel model = buildModel(Map.of("quarkus.otel.traces.exporter", "cdi,cdi"));

        assertThat(model.getTracerProvider().getProcessors()).hasSize(1);
    }

    @Test
    void noneCombinedWithOtherExportersFails() {
        assertThatThrownBy(() -> buildModel(Map.of("quarkus.otel.traces.exporter", "none,cdi")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("quarkus.otel.traces.exporter contains none along with other exporters");
    }

    @Test
    void eachMetricExporterGetsItsOwnReader() {
        OpenTelemetryConfigurationModel model = buildModel(Map.of("quarkus.otel.metrics.exporter", "cdi,my-exporter"));

        assertThat(model.getMeterProvider().getReaders()).hasSize(2);
        assertThat(model.getMeterProvider().getReaders().get(1).getPeriodic().getExporter().getAdditionalProperties())
                .containsOnlyKeys("my-exporter");
    }

    @Test
    void eachLogExporterGetsItsOwnProcessor() {
        OpenTelemetryConfigurationModel model = buildModel(Map.of("quarkus.otel.logs.exporter", "cdi,logging"));

        assertThat(model.getLoggerProvider().getProcessors()).hasSize(2);
        assertThat(model.getLoggerProvider().getProcessors().getFirst().getBatch()).isNotNull();
        assertThat(model.getLoggerProvider().getProcessors().get(1).getSimple()).isNotNull();
    }

    @Test
    void simpleKeepsBatchSpanProcessorWhenCdiSpanProcessorsPresent() {
        // the legacy SpanProcessorCustomizer only swapped batch for simple when no SpanProcessor beans existed
        OpenTelemetryConfigurationModel model = buildModel(Map.of(
                "quarkus.otel.simple", "true",
                "quarkus.otel.traces.exporter", "otlp"), true, false);
        assertThat(model.getTracerProvider().getProcessors().getFirst().getBatch()).isNotNull();

        model = buildModel(Map.of(
                "quarkus.otel.simple", "true",
                "quarkus.otel.traces.exporter", "otlp"), false, false);
        assertThat(model.getTracerProvider().getProcessors().getFirst().getSimple()).isNotNull();
    }

    @Test
    void simpleAffectsLogsOnlyWithResolvableCdiLogRecordExporter() {
        // the legacy SimpleLogRecordProcessorCustomizer only replaced the batch processor when a default-qualified
        // LogRecordExporter bean was resolvable
        OpenTelemetryConfigurationModel model = buildModel(Map.of(
                "quarkus.otel.simple", "true",
                "quarkus.otel.logs.exporter", "otlp"), false, false);
        assertThat(model.getLoggerProvider().getProcessors().getFirst().getBatch()).isNotNull();

        model = buildModel(Map.of(
                "quarkus.otel.simple", "true",
                "quarkus.otel.logs.exporter", "otlp"), false, true);
        assertThat(model.getLoggerProvider().getProcessors().getFirst().getSimple()).isNotNull();
    }

    // The former DeclarativeConfiguration.create() round-trip tests live in QuarkusDeclarativeConfigurationTest
    // now, exercising the Quarkus-internal interpreter (upstream's create() requires jackson-databind and
    // snakeyaml-engine, which this module excludes - Jackson 2 is not allowed in Quarkus 4).

    static OpenTelemetryConfigurationModel buildModel(Map<String, String> properties) {
        return buildModel(properties, Attributes.empty(), Set.of(), Set.of());
    }

    private static OpenTelemetryConfigurationModel buildModel(Map<String, String> properties, Attributes resourceAttributes) {
        return buildModel(properties, resourceAttributes, Set.of(), Set.of());
    }

    static OpenTelemetryConfigurationModel buildModel(Map<String, String> properties,
            Set<String> nonApplicationUriTargets, Set<String> staticResourceTargets) {
        return buildModel(properties, Attributes.empty(), nonApplicationUriTargets, staticResourceTargets);
    }

    private static OpenTelemetryConfigurationModel buildModel(Map<String, String> properties,
            boolean cdiSpanProcessorsPresent, boolean cdiLogRecordExporterResolvable) {
        return buildModel(properties, Attributes.empty(), Set.of(), Set.of(), cdiSpanProcessorsPresent,
                cdiLogRecordExporterResolvable);
    }

    private static OpenTelemetryConfigurationModel buildModel(Map<String, String> properties, Attributes resourceAttributes,
            Set<String> nonApplicationUriTargets, Set<String> staticResourceTargets) {
        return buildModel(properties, resourceAttributes, nonApplicationUriTargets, staticResourceTargets, false, false);
    }

    private static OpenTelemetryConfigurationModel buildModel(Map<String, String> properties, Attributes resourceAttributes,
            Set<String> nonApplicationUriTargets, Set<String> staticResourceTargets,
            boolean cdiSpanProcessorsPresent, boolean cdiLogRecordExporterResolvable) {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .addDiscoveredConverters()
                .withCustomizers(builder -> new OtlpExporterConfigBuilder().configBuilder(builder))
                .withMapping(OTelBuildConfig.class)
                .withMapping(OTelRuntimeConfig.class)
                .withMapping(OtlpExporterRuntimeConfig.class)
                .withDefaultValues(properties)
                .build();

        return QuarkusOTelModelBuilder
                .forConfig(
                        config.getConfigMapping(OTelBuildConfig.class),
                        config.getConfigMapping(OTelRuntimeConfig.class),
                        config.getConfigMapping(OtlpExporterRuntimeConfig.class),
                        otelConfigProperties(config))
                .resourceAttributes(resourceAttributes)
                .nonApplicationUriTargets(nonApplicationUriTargets)
                .staticResourceTargets(staticResourceTargets)
                .cdiSpanProcessorsPresent(cdiSpanProcessorsPresent)
                .cdiLogRecordExporterResolvable(cdiLogRecordExporterResolvable)
                .build();
    }

    /**
     * The flat otel.* view of the configuration, mirroring OpenTelemetryRecorder.getOtelConfigs() in its default
     * (non-MP-compatibility) precedence: every quarkus.otel.* value, mapping defaults included, under its
     * quarkus-stripped name, overriding any bare otel.* property. The recorder additionally converts
     * Quarkus-only duration formats; tests use autoconfigure-parsable values directly.
     */
    private static ConfigProperties otelConfigProperties(SmallRyeConfig config) {
        Map<String, String> otelConfigs = new HashMap<>();
        for (String propertyName : config.getPropertyNames()) {
            if (propertyName.startsWith("otel.")) {
                ConfigValue value = config.getConfigValue(propertyName);
                if (value.getValue() != null) {
                    otelConfigs.put(propertyName, value.getValue());
                }
            }
        }
        for (String propertyName : config.getPropertyNames()) {
            if (propertyName.startsWith("quarkus.otel.")) {
                ConfigValue value = config.getConfigValue(propertyName);
                if (value.getValue() != null) {
                    otelConfigs.put(propertyName.substring("quarkus.".length()), value.getValue());
                }
            }
        }
        return DefaultConfigProperties.createFromMap(otelConfigs);
    }
}
