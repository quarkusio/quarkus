package io.quarkus.opentelemetry.runtime.declarativeconfig;

import static io.quarkus.opentelemetry.runtime.config.build.ExporterType.Constants.CDI_VALUE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.assertj.core.groups.Tuple;
import org.junit.jupiter.api.Test;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.Value;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.AlwaysOffSamplerModel;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.AlwaysOnSamplerModel;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.BatchSpanProcessorModel;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.ExemplarFilterModel;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.IdGeneratorModel;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.LogRecordProcessorModel;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.MetricReaderModel;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.OpenTelemetryConfigurationModel;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.ParentBasedSamplerModel;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.SamplerModel;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.SpanExporterModel;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.SpanLimitsModel;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.SpanProcessorModel;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.TextMapPropagatorModel;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.TraceIdRatioBasedSamplerModel;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigurationException;
import io.opentelemetry.sdk.autoconfigure.spi.internal.DefaultConfigProperties;
import io.opentelemetry.sdk.resources.Resource;
import io.quarkus.opentelemetry.runtime.config.build.OTelBuildConfig;
import io.quarkus.opentelemetry.runtime.config.runtime.OTelRuntimeConfig;
import io.quarkus.opentelemetry.runtime.config.runtime.exporter.OtlpExporterConfigBuilder;
import io.quarkus.opentelemetry.runtime.config.runtime.exporter.OtlpExporterRuntimeConfig;
import io.smallrye.config.ConfigValue;
import io.smallrye.config.SmallRyeConfig;
import io.smallrye.config.SmallRyeConfigBuilder;

class QuarkusOTelModelBuilderTest {

    private static final SamplerModel CDI_SAMPLER = new SamplerModel()
            .setExtensionProperty(QuarkusOTelModelBuilder.QUARKUS_CDI, Map.of());
    private static final SpanProcessorModel CDI_PROCESSOR = new SpanProcessorModel()
            .setExtensionProperty(QuarkusOTelModelBuilder.QUARKUS_CDI, Map.of());
    private static final SpanProcessorModel DROP_ALL = new SpanProcessorModel()
            .setExtensionProperty(QuarkusOTelModelBuilder.QUARKUS_DROP_ALL, Map.of());

    @Test
    void defaultConfigUsesCdiExporterBridge() {
        OpenTelemetryConfigurationModel model = buildModel(Map.of());

        assertThat(model.getFileFormat()).isEqualTo("1.2");
        assertThat(model.getDisabled()).isFalse();
        assertThat(model.getTracerProvider().getSampler()).isEqualTo(
                new SamplerModel().setParentBased(new ParentBasedSamplerModel()
                        .setRoot(new SamplerModel().setTraceIdRatioBased(
                                new TraceIdRatioBasedSamplerModel().setRatio(0.1d)))));

        // default exporter is "cdi": delegate to whatever SpanExporter CDI beans exist, same as today's
        // SpanExporterCDIProvider, rather than building anything OTLP-specific in the model.
        SpanExporterModel exporter = model.getTracerProvider().getProcessors().getFirst().getBatch().getExporter();
        assertThat(exporter.getOtlpHttp()).isNull();
        assertThat(exporter.getOtlpGrpc()).isNull();
        assertThat(composed(exporter.getExtensionProperties())).containsExactly(CDI_VALUE);
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
    void valueTypedResourceAttributeIsSkipped() {
        // The model's AttributeTypeModel has no counterpart for AttributeType.VALUE
        OpenTelemetryConfigurationModel model = buildModel(Map.of(), Attributes.builder()
                .put(AttributeKey.stringKey("service.name"), "my-service")
                .put(AttributeKey.valueKey("structured"), Value.of(new byte[] { 1 }))
                .build());

        assertThat(model.getResource().getAttributes())
                .extracting("name", "value")
                .containsExactly(Tuple.tuple("service.name", "my-service"));
    }

    @Test
    void resourceSchemaUrlIsMapped() {
        OpenTelemetryConfigurationModel model = buildModel(Map.of(),
                Resource.create(Attributes.empty(), "https://opentelemetry.io/schemas/1.40.0"));

        assertThat(model.getResource().getSchemaUrl()).isEqualTo("https://opentelemetry.io/schemas/1.40.0");
    }

    @Test
    void alwaysOnSampler() {
        OpenTelemetryConfigurationModel model = buildModel(Map.of("quarkus.otel.traces.sampler", "always_on"));

        assertThat(model.getTracerProvider().getSampler())
                .isEqualTo(new SamplerModel().setAlwaysOn(new AlwaysOnSamplerModel()));
    }

    @Test
    void parentBasedAlwaysOffSampler() {
        OpenTelemetryConfigurationModel model = buildModel(
                Map.of("quarkus.otel.traces.sampler", "parentbased_always_off"));

        assertThat(model.getTracerProvider().getSampler()).isEqualTo(
                new SamplerModel().setParentBased(new ParentBasedSamplerModel()
                        .setRoot(new SamplerModel().setAlwaysOff(new AlwaysOffSamplerModel()))));
    }

    @Test
    void customSamplerNameIsKeptAsExtensionProperty() {
        OpenTelemetryConfigurationModel model = buildModel(Map.of("quarkus.otel.traces.sampler", "my_custom_sampler"));

        assertThat(model.getTracerProvider().getSampler())
                .isEqualTo(new SamplerModel().setExtensionProperty("my_custom_sampler", Map.of()));
    }

    @Test
    void customSamplerToleratesNonNumericSamplerArg() {
        OpenTelemetryConfigurationModel model = buildModel(Map.of(
                "quarkus.otel.traces.sampler", "my_custom_sampler",
                "quarkus.otel.traces.sampler.arg", "rate:5"));

        assertThat(model.getTracerProvider().getSampler())
                .isEqualTo(new SamplerModel().setExtensionProperty("my_custom_sampler", Map.of()));
    }

    @Test
    void customExporterNameIsKeptAsExtensionProperty() {
        OpenTelemetryConfigurationModel model = buildModel(Map.of("quarkus.otel.traces.exporter", "my-exporter"));

        assertThat(model.getTracerProvider().getProcessors()).hasSize(1);
        assertThat(composed(
                model.getTracerProvider().getProcessors().getFirst().getBatch().getExporter().getExtensionProperties()))
                .containsExactly("my-exporter");
    }

    @Test
    void dropTargetsWrapSamplerWhenNonApplicationUrisSuppressed() {
        // suppress-non-application-uris defaults to true, so any candidate targets supplied by other
        // extensions (normally TracerRecorder.dropNonApplicationUriTargets) get dropped.
        OpenTelemetryConfigurationModel model = buildModel(Map.of("quarkus.otel.traces.sampler", "always_on"),
                Set.of("/q/health"), Set.of());

        SamplerModel wrapped = model.getTracerProvider().getSampler();
        @SuppressWarnings("unchecked")
        Map<String, Object> dropTargets = (Map<String, Object>) wrapped.getExtensionProperties()
                .get(QuarkusOTelModelBuilder.QUARKUS_DROP_TARGETS);
        assertThat(dropTargets).isNotNull();
        assertThat(dropTargets)
                .containsEntry("delegate", new SamplerModel().setAlwaysOn(new AlwaysOnSamplerModel()))
                .containsEntry("drop_targets", Set.of("/q/health"));
    }

    @Test
    void dropTargetsIncludeStaticResourcesByDefault() {
        // include-static-resources defaults to false, meaning static resources ARE dropped by default.
        OpenTelemetryConfigurationModel model = buildModel(Map.of(
                "quarkus.otel.traces.sampler", "always_on",
                "quarkus.otel.traces.suppress-non-application-uris", "false"),
                Set.of(), Set.of("/static"));

        @SuppressWarnings("unchecked")

        Map<String, Object> dropTargets = (Map<String, Object>) model.getTracerProvider().getSampler()
                .getExtensionProperties().get(QuarkusOTelModelBuilder.QUARKUS_DROP_TARGETS);
        assertThat(dropTargets.get("drop_targets"))
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

        @SuppressWarnings("unchecked")

        Map<String, Object> dropTargets = (Map<String, Object>) model.getTracerProvider().getSampler()
                .getExtensionProperties().get(QuarkusOTelModelBuilder.QUARKUS_DROP_TARGETS);
        assertThat(dropTargets.get("drop_targets"))
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
                .isEqualTo(new SamplerModel().setAlwaysOn(new AlwaysOnSamplerModel()));
    }

    @Test
    void otlpTracesExporterIsResolvedByNameBehindABatchProcessor() {
        OpenTelemetryConfigurationModel model = buildModel(Map.of("quarkus.otel.traces.exporter", "otlp"));

        SpanProcessorModel processor = model.getTracerProvider().getProcessors().getFirst();
        assertThat(processor.getSimple()).isNull();
        BatchSpanProcessorModel batch = processor.getBatch();
        assertThat(batch.getScheduleDelay()).isEqualTo(5_000);
        assertThat(batch.getMaxQueueSize()).isEqualTo(2048);
        assertThat(batch.getMaxExportBatchSize()).isEqualTo(512);
        assertThat(batch.getExportTimeout()).isEqualTo(30_000);
        // upstream's otlp provider reads the flat otel.exporter.otlp.* properties, as on the autoconfigure path
        assertThat(composed(batch.getExporter().getExtensionProperties())).containsExactly("otlp");
    }

    @Test
    void parentbasedJaegerRemoteWrapsTheJaegerRemoteSampler() {
        // autoconfigure built parentBased around the sampler the jaeger_remote provider supplies
        assertThat(buildModel(Map.of("quarkus.otel.traces.sampler", "parentbased_jaeger_remote"))
                .getTracerProvider().getSampler())
                .isEqualTo(new SamplerModel().setParentBased(new ParentBasedSamplerModel()
                        .setRoot(new SamplerModel().setExtensionProperty("jaeger_remote", Map.of()))));
    }

    @Test
    void simpleProcessorIsUsedWhenConfigured() {
        OpenTelemetryConfigurationModel model = buildModel(Map.of(
                "quarkus.otel.simple", "true",
                "quarkus.otel.traces.exporter", "otlp"));

        // As the legacy SpanProcessorCustomizer, which created the simple processor without the SDK meter provider
        assertThat(simpleExporters(model.getTracerProvider().getProcessors().getFirst())).containsExactly("otlp");
    }

    @Test
    void metricExportIntervalUsesQuarkusDurationSemantics() {
        // The interval reads the typed config, so Quarkus duration parsing applies: ISO-8601 works and a bare
        // number means seconds.
        OpenTelemetryConfigurationModel model = buildModel(Map.of(
                "quarkus.otel.metrics.exporter", "cdi",
                "quarkus.otel.metric.export.interval", "PT30S"));

        assertThat(periodic(model.getMeterProvider().getReaders().getFirst())).containsEntry("interval", 30000);

        model = buildModel(Map.of(
                "quarkus.otel.metrics.exporter", "cdi",
                "quarkus.otel.metric.export.interval", "10"));

        assertThat(periodic(model.getMeterProvider().getReaders().getFirst())).containsEntry("interval", 10000);
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
                .isEqualTo(new SamplerModel().setAlwaysOff(new AlwaysOffSamplerModel()));
        assertThat(model.getTracerProvider().getProcessors()).isNullOrEmpty();
    }

    @Test
    void noneExporterProducesTheDropAllProcessor() {
        // As the legacy TracerProviderCustomizer: tracing is on, so a processor keeps the SDK tracers real
        OpenTelemetryConfigurationModel model = buildModel(Map.of("quarkus.otel.traces.exporter", "none"));

        assertThat(model.getTracerProvider().getProcessors()).containsExactly(DROP_ALL);
    }

    @Test
    void noneExporterWithCdiSpanProcessorsHasOnlyTheCdiProcessor() {
        OpenTelemetryConfigurationModel model = buildModel(Map.of("quarkus.otel.traces.exporter", "none"), true);

        assertThat(model.getTracerProvider().getProcessors()).containsExactly(CDI_PROCESSOR);
    }

    @Test
    void cdiSpanProcessorsComeAfterTheConfiguredProcessors() {
        OpenTelemetryConfigurationModel model = buildModel(Map.of("quarkus.otel.traces.exporter", "cdi,console"), true);

        List<SpanProcessorModel> processors = model.getTracerProvider().getProcessors();
        assertThat(processors).hasSize(3);
        assertThat(processors.get(0).getSimple()).isNotNull();
        assertThat(processors.get(1).getBatch()).isNotNull();
        assertThat(processors.get(2)).isEqualTo(CDI_PROCESSOR);
    }

    @Test
    void cdiExporterWithoutBeansGetsTheDropAllProcessor() {
        // As the legacy SpanProcessorCustomizer, which replaced the batch processor around the no-op exporter
        OpenTelemetryConfigurationModel model = modelBuilder(Map.of("quarkus.otel.traces.exporter", "cdi,console"))
                .cdiSpanExportersPresent(false)
                .build();

        List<SpanProcessorModel> processors = model.getTracerProvider().getProcessors();
        assertThat(processors).hasSize(2);
        assertThat(processors.get(0).getSimple()).isNotNull();
        assertThat(processors.get(1)).isEqualTo(DROP_ALL);
    }

    @Test
    void dropsCdiSpansOnlyWhenCdiAloneIsBatchedWithoutBeans() {
        assertThat(modelBuilder(Map.of()).cdiSpanExportersPresent(false).dropsCdiSpans()).isTrue();
        assertThat(modelBuilder(Map.of("quarkus.otel.traces.exporter", "cdi,console"))
                .cdiSpanExportersPresent(false).dropsCdiSpans()).isTrue();

        assertThat(modelBuilder(Map.of()).dropsCdiSpans()).as("SpanExporter beans").isFalse();
        assertThat(modelBuilder(Map.of()).cdiSpanExportersPresent(false).cdiSpanProcessorsPresent(true)
                .dropsCdiSpans()).as("SpanProcessor beans").isFalse();
        assertThat(modelBuilder(Map.of("quarkus.otel.traces.exporter", "cdi,otlp"))
                .cdiSpanExportersPresent(false).dropsCdiSpans()).as("composed with another exporter").isFalse();
        assertThat(modelBuilder(Map.of("quarkus.otel.simple", "true"))
                .cdiSpanExportersPresent(false).dropsCdiSpans()).as("simple processor").isFalse();
        assertThat(modelBuilder(Map.of("quarkus.otel.traces.enabled", "false"))
                .cdiSpanExportersPresent(false).dropsCdiSpans()).as("traces disabled").isFalse();
        assertThat(modelBuilder(Map.of("quarkus.otel.traces.exporter", "none"))
                .cdiSpanExportersPresent(false).dropsCdiSpans()).as("no cdi exporter").isFalse();
    }

    @Test
    void cdiExporterWithoutBeansKeepsTheBatchProcessorWithCdiSpanProcessors() {
        OpenTelemetryConfigurationModel model = modelBuilder(Map.of())
                .cdiSpanExportersPresent(false)
                .cdiSpanProcessorsPresent(true)
                .build();

        List<SpanProcessorModel> processors = model.getTracerProvider().getProcessors();
        assertThat(processors).hasSize(2);
        assertThat(processors.get(0).getBatch()).isNotNull();
        assertThat(processors.get(1)).isEqualTo(CDI_PROCESSOR);
    }

    @Test
    void cdiExporterWithoutBeansKeepsTheSimpleProcessor() {
        OpenTelemetryConfigurationModel model = modelBuilder(Map.of("quarkus.otel.simple", "true"))
                .cdiSpanExportersPresent(false)
                .build();

        assertThat(simpleExporters(model.getTracerProvider().getProcessors().getFirst())).containsExactly(CDI_VALUE);
    }

    @Test
    void cdiExporterWithoutBeansComposedWithAnotherExporterKeepsTheBatchProcessor() {
        // The composite of the no-op exporter and another exporter is not the no-op exporter
        OpenTelemetryConfigurationModel model = modelBuilder(Map.of("quarkus.otel.traces.exporter", "cdi,otlp"))
                .cdiSpanExportersPresent(false)
                .build();

        assertThat(model.getTracerProvider().getProcessors()).singleElement()
                .satisfies(processor -> assertThat(processor.getBatch()).isNotNull());
    }

    @Test
    void consoleExporterGetsItsOwnSimpleProcessorFirst() {
        OpenTelemetryConfigurationModel model = buildModel(Map.of("quarkus.otel.traces.exporter", "cdi,console"));

        assertThat(model.getTracerProvider().getProcessors()).hasSize(2);
        // console and logging always use a simple processor, created before the batch processor, as autoconfigure did
        assertThat(composed(
                model.getTracerProvider().getProcessors().get(0).getSimple().getExporter().getExtensionProperties()))
                .containsExactly("console");
        assertThat(composed(
                model.getTracerProvider().getProcessors().get(1).getBatch().getExporter().getExtensionProperties()))
                .containsExactly(CDI_VALUE);
    }

    @Test
    void duplicateExportersFail() {
        // As autoconfigure, which read the exporter names with DefaultConfigProperties.getSet
        assertThatThrownBy(() -> buildModel(Map.of("quarkus.otel.traces.exporter", "cdi,cdi")))
                .isInstanceOf(ConfigurationException.class)
                .hasMessage("otel.traces.exporter contains duplicates: [cdi]");
        assertThatThrownBy(() -> buildModel(Map.of("quarkus.otel.metrics.exporter", "cdi,cdi")))
                .isInstanceOf(ConfigurationException.class)
                .hasMessage("otel.metrics.exporter contains duplicates: [cdi]");
        assertThatThrownBy(() -> buildModel(Map.of("quarkus.otel.logs.exporter", "cdi,cdi")))
                .isInstanceOf(ConfigurationException.class)
                .hasMessage("otel.logs.exporter contains duplicates: [cdi]");
    }

    @Test
    void noneCombinedWithOtherExportersFails() {
        assertThatThrownBy(() -> buildModel(Map.of("quarkus.otel.traces.exporter", "none,cdi")))
                .isInstanceOf(ConfigurationException.class)
                .hasMessage("otel.traces.exporter contains none along with other exporters");
    }

    @Test
    void eachMetricExporterGetsItsOwnReader() {
        OpenTelemetryConfigurationModel model = buildModel(Map.of("quarkus.otel.metrics.exporter", "cdi,my-exporter"));

        assertThat(model.getMeterProvider().getReaders()).hasSize(2);
        assertThat(periodic(model.getMeterProvider().getReaders().get(1)))
                .containsEntry("exporter", Map.of("my-exporter", Map.of()));
    }

    @Test
    void exemplarFilterIsMapped() {
        assertThat(buildModel(Map.of()).getMeterProvider().getExemplarFilter())
                .isEqualTo(ExemplarFilterModel.TRACE_BASED);
        assertThat(buildModel(Map.of("otel.metrics.exemplar.filter", "ALWAYS_ON")).getMeterProvider()
                .getExemplarFilter()).isEqualTo(ExemplarFilterModel.ALWAYS_ON);
        // autoconfigure fell back to trace_based for an unknown value instead of failing
        assertThat(buildModel(Map.of("otel.metrics.exemplar.filter", "sometimes")).getMeterProvider()
                .getExemplarFilter()).isEqualTo(ExemplarFilterModel.TRACE_BASED);
    }

    @Test
    void cardinalityLimitIsSetOnEachReader() {
        OpenTelemetryConfigurationModel model = buildModel(Map.of(
                "quarkus.otel.metrics.exporter", "cdi,my-exporter",
                "otel.java.metrics.cardinality.limit", "500"));

        assertThat(model.getMeterProvider().getReaders())
                .extracting(reader -> reader.getPull().getCardinalityLimits().getDefault())
                .containsExactly(500, 500);
        assertThat(buildModel(Map.of()).getMeterProvider().getReaders().getFirst().getPull()
                .getCardinalityLimits()).isNull();
    }

    @Test
    void devUiMetricReaderIsAddedLastWithoutCardinalityLimit() {
        OpenTelemetryConfigurationModel model = buildModel(Map.of("otel.java.metrics.cardinality.limit", "500"),
                Resource.empty(), Set.of(), Set.of(), false, true);

        List<MetricReaderModel> readers = model.getMeterProvider().getReaders();
        assertThat(readers).hasSize(2);
        assertThat(readers.get(1).getPull().getExporter().getExtensionProperties())
                .containsOnlyKeys(QuarkusOTelModelBuilder.QUARKUS_DEVUI);
        assertThat(readers.get(1).getPull().getCardinalityLimits()).isNull();
    }

    @Test
    void cardinalityLimitBelowOneFails() {
        assertThatThrownBy(() -> buildModel(Map.of("otel.java.metrics.cardinality.limit", "0")))
                .isInstanceOf(ConfigurationException.class)
                .hasMessage("otel.java.metrics.cardinality.limit must be >= 1");
    }

    @Test
    void generalAttributeCountLimitAlsoLimitsEventAndLinkAttributes() {
        // As autoconfigure: the span-specific limit overrides span attributes only
        SpanLimitsModel limits = buildModel(Map.of(
                "quarkus.otel.attribute.count.limit", "64",
                "quarkus.otel.span.attribute.count.limit", "32")).getTracerProvider().getLimits();

        assertThat(limits.getAttributeCountLimit()).isEqualTo(32);
        assertThat(limits.getEventAttributeCountLimit()).isEqualTo(64);
        assertThat(limits.getLinkAttributeCountLimit()).isEqualTo(64);
    }

    @Test
    void loggingExporterGetsItsOwnSimpleProcessorFirst() {
        OpenTelemetryConfigurationModel model = buildModel(Map.of("quarkus.otel.logs.exporter", "cdi,logging"));

        assertThat(model.getLoggerProvider().getProcessors()).hasSize(2);
        assertThat(model.getLoggerProvider().getProcessors().getFirst().getSimple()).isNotNull();
        assertThat(model.getLoggerProvider().getProcessors().get(1).getBatch()).isNotNull();
    }

    @Test
    void otherExportersShareOneProcessor() {
        // autoconfigure composes every exporter other than console/logging behind a single batch processor
        OpenTelemetryConfigurationModel model = buildModel(Map.of(
                "quarkus.otel.traces.exporter", "cdi,console,my-exporter",
                "quarkus.otel.logs.exporter", "cdi,my-exporter"));

        assertThat(model.getTracerProvider().getProcessors()).hasSize(2);
        assertThat(composed(
                model.getTracerProvider().getProcessors().get(0).getSimple().getExporter().getExtensionProperties()))
                .containsExactly("console");
        assertThat(composed(
                model.getTracerProvider().getProcessors().get(1).getBatch().getExporter().getExtensionProperties()))
                .containsExactly(CDI_VALUE, "my-exporter");

        assertThat(model.getLoggerProvider().getProcessors()).hasSize(1);
        assertThat(composed(
                model.getLoggerProvider().getProcessors().getFirst().getBatch().getExporter().getExtensionProperties()))
                .containsExactly(CDI_VALUE, "my-exporter");
    }

    @Test
    void simpleKeepsBatchSpanProcessorWhenCdiSpanProcessorsPresent() {
        // the legacy SpanProcessorCustomizer only swapped batch for simple when no SpanProcessor beans existed
        OpenTelemetryConfigurationModel model = buildModel(Map.of(
                "quarkus.otel.simple", "true",
                "quarkus.otel.traces.exporter", "otlp"), true);
        assertThat(model.getTracerProvider().getProcessors().getFirst().getBatch()).isNotNull();

        model = buildModel(Map.of(
                "quarkus.otel.simple", "true",
                "quarkus.otel.traces.exporter", "otlp"), false);
        assertThat(simpleExporters(model.getTracerProvider().getProcessors().getFirst())).containsExactly("otlp");
    }

    @Test
    void simpleKeepsTheLogBatchProcessorWithoutTheDefaultLogRecordExporterBean() {
        // As the legacy SimpleLogRecordProcessorCustomizer, which only acted when that bean was resolvable
        OpenTelemetryConfigurationModel model = buildModel(Map.of(
                "quarkus.otel.simple", "true",
                "quarkus.otel.logs.exporter", "otlp"), false);
        assertThat(model.getLoggerProvider().getProcessors().getFirst().getBatch()).isNotNull();
    }

    @Test
    void simpleReplacesTheLogBatchProcessorWithTheDefaultLogRecordExporterBean() {
        OpenTelemetryConfigurationModel model = modelBuilder(Map.of(
                "quarkus.otel.simple", "true",
                "quarkus.otel.logs.exporter", "otlp,console"))
                .defaultLogRecordExporterPresent(true)
                .build();

        // the console exporter keeps its own simple processor
        List<LogRecordProcessorModel> processors = model.getLoggerProvider().getProcessors();
        assertThat(processors).hasSize(2);
        assertThat(processors.get(0).getSimple()).isNotNull();
        assertThat(processors.get(1)).isEqualTo(new LogRecordProcessorModel()
                .setExtensionProperty(QuarkusOTelModelBuilder.QUARKUS_SIMPLE, Map.of()));
    }

    @Test
    void defaultLogRecordExporterBeanIsIgnoredWithoutSimple() {
        OpenTelemetryConfigurationModel model = modelBuilder(Map.of("quarkus.otel.logs.exporter", "otlp"))
                .defaultLogRecordExporterPresent(true)
                .build();

        assertThat(model.getLoggerProvider().getProcessors().getFirst().getBatch()).isNotNull();
    }

    @Test
    void cdiSamplerReplacesTheConfiguredSampler() {
        OpenTelemetryConfigurationModel model = modelBuilder(Map.of("quarkus.otel.traces.sampler", "always_off"))
                .cdiSamplerPresent(true)
                .build();

        assertThat(model.getTracerProvider().getSampler()).isEqualTo(CDI_SAMPLER);
    }

    @Test
    void cdiSamplerStillValidatesTheConfiguredRatio() {
        // As on main, where autoconfigure built the configured sampler before the bean replaced it
        assertThatThrownBy(() -> modelBuilder(Map.of(
                "quarkus.otel.traces.sampler", "traceidratio",
                "quarkus.otel.traces.sampler.arg", "not-a-ratio"))
                .cdiSamplerPresent(true)
                .build())
                .isInstanceOf(ConfigurationException.class)
                .hasMessageContaining("otel.traces.sampler.arg");
    }

    @Test
    void cdiSamplerKeepsTheDropTargets() {
        OpenTelemetryConfigurationModel model = modelBuilder(Map.of("quarkus.otel.traces.sampler", "always_off"))
                .nonApplicationUriTargets(Set.of("/q/health"))
                .cdiSamplerPresent(true)
                .build();

        assertThat(extension(model.getTracerProvider().getSampler(), QuarkusOTelModelBuilder.QUARKUS_DROP_TARGETS))
                .containsEntry("delegate", CDI_SAMPLER)
                .containsEntry("drop_targets", Set.of("/q/health"));
    }

    @Test
    void cdiSamplerDoesNotReplaceTheDisabledTracesSampler() {
        OpenTelemetryConfigurationModel model = modelBuilder(Map.of("quarkus.otel.traces.enabled", "false"))
                .cdiSamplerPresent(true)
                .build();

        assertThat(model.getTracerProvider().getSampler())
                .isEqualTo(new SamplerModel().setAlwaysOff(new AlwaysOffSamplerModel()));
    }

    @Test
    void samplerCustomizerWrapsTheDropTargetsSampler() {
        OpenTelemetryConfigurationModel model = modelBuilder(Map.of("quarkus.otel.traces.sampler", "always_on"))
                .nonApplicationUriTargets(Set.of("/q/health"))
                .samplerCustomized(true)
                .build();

        Map<String, Object> customized = extension(model.getTracerProvider().getSampler(),
                QuarkusOTelModelBuilder.QUARKUS_CUSTOMIZED);
        assertThat(customized).containsOnlyKeys("delegate");
        assertThat(extension((SamplerModel) customized.get("delegate"), QuarkusOTelModelBuilder.QUARKUS_DROP_TARGETS))
                .containsEntry("delegate", new SamplerModel().setAlwaysOn(new AlwaysOnSamplerModel()));
    }

    @Test
    void samplerCustomizerWrapsTheConfiguredSamplerWithoutDropTargets() {
        OpenTelemetryConfigurationModel model = modelBuilder(Map.of("quarkus.otel.traces.sampler", "always_on"))
                .samplerCustomized(true)
                .build();

        assertThat(extension(model.getTracerProvider().getSampler(), QuarkusOTelModelBuilder.QUARKUS_CUSTOMIZED))
                .containsExactly(Map.entry("delegate", new SamplerModel().setAlwaysOn(new AlwaysOnSamplerModel())));
    }

    @Test
    void samplerCustomizerWrapsTheDisabledTracesSampler() {
        // As on main, where the SPI sampler customizers ran over the always_off sampler of disabled traces
        OpenTelemetryConfigurationModel model = modelBuilder(Map.of("quarkus.otel.traces.enabled", "false"))
                .samplerCustomized(true)
                .build();

        assertThat(extension(model.getTracerProvider().getSampler(), QuarkusOTelModelBuilder.QUARKUS_CUSTOMIZED))
                .containsExactly(Map.entry("delegate", new SamplerModel().setAlwaysOff(new AlwaysOffSamplerModel())));
    }

    @Test
    void cdiIdGeneratorIsAnExtensionNode() {
        OpenTelemetryConfigurationModel model = modelBuilder(Map.of())
                .cdiIdGeneratorPresent(true)
                .build();

        assertThat(model.getTracerProvider().getIdGenerator())
                .isEqualTo(new IdGeneratorModel().setExtensionProperty(QuarkusOTelModelBuilder.QUARKUS_CDI, Map.of()));
    }

    @Test
    void idGeneratorIsUnsetWithoutABean() {
        assertThat(buildModel(Map.of()).getTracerProvider().getIdGenerator()).isNull();
    }

    @Test
    void cdiIdGeneratorIsIgnoredWhenTracesAreDisabled() {
        OpenTelemetryConfigurationModel model = modelBuilder(Map.of("quarkus.otel.traces.enabled", "false"))
                .cdiIdGeneratorPresent(true)
                .build();

        assertThat(model.getTracerProvider().getIdGenerator()).isNull();
    }

    @Test
    void propagatorsAreACompositeListWithoutCustomizers() {
        OpenTelemetryConfigurationModel model = buildModel(Map.of("quarkus.otel.propagators", "tracecontext,b3"));

        assertThat(model.getPropagator().getCompositeList()).isEqualTo("tracecontext,b3");
        assertThat(model.getPropagator().getComposite()).isNullOrEmpty();
    }

    @Test
    void customizedPropagatorsAreCompositeEntries() {
        // Upstream de-duplicates composite_list names but not composite entries, so the names are de-duplicated here
        OpenTelemetryConfigurationModel model = modelBuilder(Map.of(
                "quarkus.otel.propagators", "tracecontext,b3,tracecontext"))
                .propagatorsCustomized(true)
                .build();

        assertThat(model.getPropagator().getCompositeList()).isNull();
        assertThat(model.getPropagator().getComposite()).containsExactly(
                customizedPropagator("tracecontext"), customizedPropagator("b3"));
    }

    @Test
    void customizedPropagatorsSkipNone() {
        OpenTelemetryConfigurationModel model = modelBuilder(Map.of("quarkus.otel.propagators", "none"))
                .propagatorsCustomized(true)
                .build();

        assertThat(model.getPropagator().getComposite()).isNullOrEmpty();
    }

    @Test
    void propagatorNamesAreTrimmedAndDeduplicatedWithoutNone() {
        assertThat(QuarkusOTelModelBuilder.propagatorNames(List.of(" tracecontext", "baggage", "", "tracecontext ")))
                .containsExactly("tracecontext", "baggage");
        assertThat(QuarkusOTelModelBuilder.propagatorNames(List.of("none"))).isEmpty();
    }

    @Test
    void noneCombinedWithOtherPropagatorsFails() {
        assertThatThrownBy(() -> buildModel(Map.of("quarkus.otel.propagators", "tracecontext,none")))
                .isInstanceOf(ConfigurationException.class)
                .hasMessage("otel.propagators contains 'none' along with other propagators");
        // The disabled SDK resolves its propagators through propagatorNames too
        assertThatThrownBy(() -> QuarkusOTelModelBuilder.propagatorNames(List.of("none", " baggage")))
                .isInstanceOf(ConfigurationException.class)
                .hasMessage("otel.propagators contains 'none' along with other propagators");
        assertThatThrownBy(() -> QuarkusOTelModelBuilder.propagatorNames(List.of("none", "none")))
                .isInstanceOf(ConfigurationException.class)
                .hasMessage("otel.propagators contains 'none' along with other propagators");
    }

    private static TextMapPropagatorModel customizedPropagator(String name) {
        return new TextMapPropagatorModel()
                .setExtensionProperty(QuarkusOTelModelBuilder.QUARKUS_CUSTOMIZED, Map.of("name", name));
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

    private static OpenTelemetryConfigurationModel buildModel(Map<String, String> properties, Resource resource) {
        return buildModel(properties, resource, Set.of(), Set.of(), false);
    }

    static OpenTelemetryConfigurationModel buildModel(Map<String, String> properties,
            Set<String> nonApplicationUriTargets, Set<String> staticResourceTargets) {
        return buildModel(properties, Attributes.empty(), nonApplicationUriTargets, staticResourceTargets);
    }

    private static OpenTelemetryConfigurationModel buildModel(Map<String, String> properties,
            boolean cdiSpanProcessorsPresent) {
        return buildModel(properties, Resource.empty(), Set.of(), Set.of(), cdiSpanProcessorsPresent);
    }

    private static OpenTelemetryConfigurationModel buildModel(Map<String, String> properties, Attributes resourceAttributes,
            Set<String> nonApplicationUriTargets, Set<String> staticResourceTargets) {
        return buildModel(properties, Resource.create(resourceAttributes), nonApplicationUriTargets, staticResourceTargets,
                false);
    }

    private static OpenTelemetryConfigurationModel buildModel(Map<String, String> properties, Resource resource,
            Set<String> nonApplicationUriTargets, Set<String> staticResourceTargets,
            boolean cdiSpanProcessorsPresent) {
        return buildModel(properties, resource, nonApplicationUriTargets, staticResourceTargets,
                cdiSpanProcessorsPresent, false);
    }

    private static OpenTelemetryConfigurationModel buildModel(Map<String, String> properties, Resource resource,
            Set<String> nonApplicationUriTargets, Set<String> staticResourceTargets,
            boolean cdiSpanProcessorsPresent, boolean devUiMetricReaderPresent) {
        return modelBuilder(properties)
                .resource(resource)
                .nonApplicationUriTargets(nonApplicationUriTargets)
                .staticResourceTargets(staticResourceTargets)
                .cdiSpanProcessorsPresent(cdiSpanProcessorsPresent)
                .devUiMetricReaderPresent(devUiMetricReaderPresent)
                .build();
    }

    // A builder over the given configuration, with every other input at its default
    static QuarkusOTelModelBuilder modelBuilder(Map<String, String> properties) {
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
                        otelConfigProperties(config));
    }

    // The exporter names, in order, of the quarkus_composite node that every exporter node is
    @SuppressWarnings("unchecked")
    static List<String> composed(Map<String, Object> exporterExtensionProperties) {
        assertThat(exporterExtensionProperties).containsOnlyKeys(QuarkusOTelModelBuilder.QUARKUS_COMPOSITE);
        Map<String, Object> composite = (Map<String, Object>) exporterExtensionProperties
                .get(QuarkusOTelModelBuilder.QUARKUS_COMPOSITE);
        List<String> names = new ArrayList<>();
        for (Map<String, Object> child : (List<Map<String, Object>>) composite.get("exporters")) {
            assertThat(child).hasSize(1);
            names.add(child.keySet().iterator().next());
        }
        return names;
    }

    // The exporter names of a quarkus_simple span processor node
    @SuppressWarnings("unchecked")
    static List<String> simpleExporters(SpanProcessorModel processor) {
        assertThat(processor.getExtensionProperties()).containsOnlyKeys(QuarkusOTelModelBuilder.QUARKUS_SIMPLE);
        Map<String, Object> simple = (Map<String, Object>) processor.getExtensionProperties()
                .get(QuarkusOTelModelBuilder.QUARKUS_SIMPLE);
        return composed((Map<String, Object>) simple.get("exporter"));
    }

    // The map an extension node holds under name
    @SuppressWarnings("unchecked")
    static Map<String, Object> extension(SamplerModel sampler, String name) {
        return (Map<String, Object>) sampler.getExtensionProperties().get(name);
    }

    // The quarkus_periodic node of a pull reader
    @SuppressWarnings("unchecked")
    static Map<String, Object> periodic(MetricReaderModel reader) {
        return (Map<String, Object>) reader.getPull().getExporter().getExtensionProperties()
                .get(QuarkusOTelModelBuilder.QUARKUS_PERIODIC);
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
