package io.quarkus.opentelemetry.runtime.exporter.otlp;

import static io.opentelemetry.sdk.common.internal.StandardComponentId.ExporterType.OTLP_GRPC_METRIC_EXPORTER;
import static io.quarkus.opentelemetry.runtime.config.runtime.exporter.OtlpExporterConfig.Protocol.GRPC;
import static io.quarkus.opentelemetry.runtime.config.runtime.exporter.OtlpExporterConfig.Protocol.HTTP_PROTOBUF;
import static io.quarkus.opentelemetry.runtime.exporter.otlp.OTelExporterUtil.determineCompression;
import static io.quarkus.opentelemetry.runtime.exporter.otlp.OTelExporterUtil.populateTracingExportHttpHeaders;

import java.net.URI;
import java.util.Locale;

import io.opentelemetry.api.metrics.MeterProvider;
import io.opentelemetry.exporter.otlp.internal.GrpcExporter;
import io.opentelemetry.exporter.otlp.internal.GrpcExporterBuilder;
import io.opentelemetry.exporter.otlp.internal.HttpExporter;
import io.opentelemetry.exporter.otlp.internal.HttpExporterBuilder;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigurationException;
import io.opentelemetry.sdk.common.InternalTelemetryVersion;
import io.opentelemetry.sdk.common.export.MemoryMode;
import io.opentelemetry.sdk.common.internal.ComponentId;
import io.opentelemetry.sdk.common.internal.StandardComponentId;
import io.opentelemetry.sdk.metrics.Aggregation;
import io.opentelemetry.sdk.metrics.InstrumentType;
import io.opentelemetry.sdk.metrics.export.AggregationTemporalitySelector;
import io.opentelemetry.sdk.metrics.export.DefaultAggregationSelector;
import io.opentelemetry.sdk.metrics.export.MetricExporter;
import io.opentelemetry.sdk.metrics.internal.aggregator.AggregationUtil;
import io.quarkus.arc.BeanCreator;
import io.quarkus.arc.SyntheticCreationalContext;
import io.quarkus.opentelemetry.runtime.OpenTelemetryUtil;
import io.quarkus.opentelemetry.runtime.config.runtime.OTelRuntimeConfig;
import io.quarkus.opentelemetry.runtime.config.runtime.exporter.OtlpExporterMetricsConfig;
import io.quarkus.opentelemetry.runtime.config.runtime.exporter.OtlpExporterRuntimeConfig;
import io.quarkus.opentelemetry.runtime.exporter.otlp.metrics.NoopMetricExporter;
import io.quarkus.opentelemetry.runtime.exporter.otlp.metrics.VertxGrpcMetricExporter;
import io.quarkus.opentelemetry.runtime.exporter.otlp.metrics.VertxHttpMetricsExporter;
import io.quarkus.opentelemetry.runtime.exporter.otlp.sender.VertxGrpcSender;
import io.quarkus.opentelemetry.runtime.exporter.otlp.sender.VertxHttpSender;
import io.quarkus.tls.TlsConfigurationRegistry;
import io.smallrye.config.Config;

/**
 * Creates the default OTLP {@link MetricExporter} bean. Its endpoint is validated at startup.
 */
@SuppressWarnings("deprecation")
public class OtlpMetricExporterCreator implements BeanCreator<MetricExporter> {
    public static final String BASE2EXPONENTIAL_AGGREGATION_NAME = AggregationUtil
            .aggregationName(Aggregation.base2ExponentialBucketHistogram());

    @Override
    public MetricExporter create(SyntheticCreationalContext<MetricExporter> context) {
        OpenTelemetryUtil.checkNotStaticInit(MetricExporter.class);
        Config config = Config.get();
        OtlpExporterRuntimeConfig exporterRuntimeConfig = config.getConfigMapping(OtlpExporterRuntimeConfig.class);
        URI baseUri = OtlpExporterEndpoints.uri(exporterRuntimeConfig, exporterRuntimeConfig.metrics());
        if (config.getConfigMapping(OTelRuntimeConfig.class).sdkDisabled() || baseUri == null) {
            return NoopMetricExporter.INSTANCE;
        }

        MemoryMode memoryMode = exporterRuntimeConfig.memoryMode();
        MetricExporter metricExporter;

        try {
            TlsConfigurationRegistry tlsConfigurationRegistry = context
                    .getInjectedReference(TlsConfigurationRegistry.class);
            OtlpExporterMetricsConfig metricsConfig = exporterRuntimeConfig.metrics();
            if (metricsConfig.protocol().isEmpty()) {
                throw new IllegalStateException("No OTLP protocol specified. " +
                        "Please check `quarkus.otel.exporter.otlp.metrics.protocol` property");
            }

            String protocol = metricsConfig.protocol().get();
            if (GRPC.equals(protocol)) {
                metricExporter = new VertxGrpcMetricExporter(
                        new GrpcExporter(
                                new VertxGrpcSender(
                                        baseUri,
                                        VertxGrpcSender.GRPC_METRIC_SERVICE_NAME,
                                        determineCompression(metricsConfig),
                                        metricsConfig.timeout(),
                                        populateTracingExportHttpHeaders(metricsConfig),
                                        new HttpClientOptionsConsumer(metricsConfig, baseUri, tlsConfigurationRegistry),
                                        OTelExporterUtil.vertx()),
                                InternalTelemetryVersion.LATEST,
                                ComponentId.generateLazy(OTLP_GRPC_METRIC_EXPORTER), // use the same as OTel does
                                MeterProvider::noop,
                                baseUri,
                                GrpcExporterBuilder.DEFAULT_MAX_REQUEST_MESSAGE_SIZE),
                        aggregationTemporalityResolver(metricsConfig),
                        aggregationResolver(metricsConfig),
                        memoryMode);
            } else if (HTTP_PROTOBUF.equals(protocol)) {
                boolean exportAsJson = false; //TODO: this will be enhanced in the future
                metricExporter = new VertxHttpMetricsExporter(
                        new HttpExporter(
                                ComponentId.generateLazy(
                                        StandardComponentId.ExporterType.OTLP_HTTP_METRIC_EXPORTER),
                                new VertxHttpSender(
                                        baseUri,
                                        VertxHttpSender.METRICS_PATH,
                                        determineCompression(metricsConfig),
                                        metricsConfig.timeout(),
                                        populateTracingExportHttpHeaders(metricsConfig),
                                        exportAsJson ? "application/json" : "application/x-protobuf",
                                        new HttpClientOptionsConsumer(metricsConfig, baseUri, tlsConfigurationRegistry),
                                        OTelExporterUtil.vertx()),
                                MeterProvider::noop,
                                InternalTelemetryVersion.LATEST,
                                baseUri,
                                false,
                                HttpExporterBuilder.DEFAULT_MAX_REQUEST_BODY_SIZE),
                        aggregationTemporalityResolver(metricsConfig),
                        aggregationResolver(metricsConfig),
                        memoryMode);
            } else {
                throw new IllegalArgumentException(String.format("Unsupported OTLP protocol %s specified. " +
                        "Please check `quarkus.otel.exporter.otlp.metrics.protocol` property", protocol));
            }

        } catch (IllegalArgumentException iae) {
            throw new IllegalStateException("Unable to install OTLP Exporter", iae);
        }
        return metricExporter;
    }

    private static DefaultAggregationSelector aggregationResolver(OtlpExporterMetricsConfig metricsConfig) {
        String defaultHistogramAggregation = metricsConfig.defaultHistogramAggregation()
                .map(s -> s.toLowerCase(Locale.ROOT))
                .orElse("explicit_bucket_histogram");

        DefaultAggregationSelector aggregationSelector;
        if (defaultHistogramAggregation.equals("explicit_bucket_histogram")) {
            aggregationSelector = DefaultAggregationSelector.getDefault();
        } else if (BASE2EXPONENTIAL_AGGREGATION_NAME.equalsIgnoreCase(defaultHistogramAggregation)) {

            aggregationSelector = DefaultAggregationSelector
                    .getDefault()
                    .with(InstrumentType.HISTOGRAM, Aggregation.base2ExponentialBucketHistogram());

        } else {
            throw new ConfigurationException(
                    "Unrecognized default histogram aggregation: " + defaultHistogramAggregation);
        }
        return aggregationSelector;
    }

    private static AggregationTemporalitySelector aggregationTemporalityResolver(OtlpExporterMetricsConfig metricsConfig) {
        String temporalityValue = metricsConfig.temporalityPreference()
                .map(s -> s.toLowerCase(Locale.ROOT))
                .orElse("cumulative");
        AggregationTemporalitySelector temporalitySelector = switch (temporalityValue) {
            case "cumulative" -> AggregationTemporalitySelector.alwaysCumulative();
            case "delta" -> AggregationTemporalitySelector.deltaPreferred();
            case "lowmemory" -> AggregationTemporalitySelector.lowMemory();
            default -> throw new ConfigurationException("Unrecognized aggregation temporality: " + temporalityValue);
        };
        return temporalitySelector;
    }
}
