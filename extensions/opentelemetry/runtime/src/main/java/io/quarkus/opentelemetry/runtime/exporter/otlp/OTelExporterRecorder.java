package io.quarkus.opentelemetry.runtime.exporter.otlp;

import static io.quarkus.opentelemetry.runtime.config.build.ExporterType.Constants.OTLP_VALUE;
import static io.quarkus.opentelemetry.runtime.config.runtime.exporter.OtlpExporterConfig.Protocol.GRPC;
import static io.quarkus.opentelemetry.runtime.config.runtime.exporter.OtlpExporterConfig.Protocol.HTTP_PROTOBUF;
import static io.quarkus.opentelemetry.runtime.config.runtime.exporter.OtlpExporterRuntimeConfig.DEFAULT_GRPC_BASE_URI;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.util.TypeLiteral;

import io.opentelemetry.api.metrics.MeterProvider;
import io.opentelemetry.exporter.internal.ExporterBuilderUtil;
import io.opentelemetry.exporter.internal.grpc.GrpcExporter;
import io.opentelemetry.exporter.internal.http.HttpExporter;
import io.opentelemetry.exporter.internal.otlp.logs.LogsRequestMarshaler;
import io.opentelemetry.exporter.internal.otlp.metrics.MetricsRequestMarshaler;
import io.opentelemetry.exporter.internal.otlp.traces.TraceRequestMarshaler;
import io.opentelemetry.exporter.otlp.internal.OtlpUserAgent;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigurationException;
import io.opentelemetry.sdk.logs.export.LogRecordExporter;
import io.opentelemetry.sdk.metrics.Aggregation;
import io.opentelemetry.sdk.metrics.InstrumentType;
import io.opentelemetry.sdk.metrics.export.AggregationTemporalitySelector;
import io.opentelemetry.sdk.metrics.export.DefaultAggregationSelector;
import io.opentelemetry.sdk.metrics.export.MetricExporter;
import io.opentelemetry.sdk.metrics.internal.aggregator.AggregationUtil;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessorBuilder;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessorBuilder;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import io.quarkus.arc.SyntheticCreationalContext;
import io.quarkus.opentelemetry.runtime.config.build.OTelBuildConfig;
import io.quarkus.opentelemetry.runtime.config.runtime.BatchSpanProcessorConfig;
import io.quarkus.opentelemetry.runtime.config.runtime.OTelRuntimeConfig;
import io.quarkus.opentelemetry.runtime.config.runtime.exporter.*;
import io.quarkus.opentelemetry.runtime.exporter.otlp.logs.NoopLogRecordExporter;
import io.quarkus.opentelemetry.runtime.exporter.otlp.logs.VertxGrpcLogRecordExporter;
import io.quarkus.opentelemetry.runtime.exporter.otlp.logs.VertxHttpLogRecordExporter;
import io.quarkus.opentelemetry.runtime.exporter.otlp.metrics.NoopMetricExporter;
import io.quarkus.opentelemetry.runtime.exporter.otlp.metrics.VertxGrpcMetricExporter;
import io.quarkus.opentelemetry.runtime.exporter.otlp.metrics.VertxHttpMetricsExporter;
import io.quarkus.opentelemetry.runtime.exporter.otlp.sender.VertxGrpcSender;
import io.quarkus.opentelemetry.runtime.exporter.otlp.sender.VertxHttpSender;
import io.quarkus.opentelemetry.runtime.exporter.otlp.tracing.LateBoundSpanProcessor;
import io.quarkus.opentelemetry.runtime.exporter.otlp.tracing.RemoveableLateBoundSpanProcessor;
import io.quarkus.opentelemetry.runtime.exporter.otlp.tracing.VertxGrpcSpanExporter;
import io.quarkus.opentelemetry.runtime.exporter.otlp.tracing.VertxHttpSpanExporter;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;
import io.quarkus.tls.TlsConfiguration;
import io.quarkus.tls.TlsConfigurationRegistry;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.net.PemKeyCertOptions;
import io.vertx.core.net.PemTrustOptions;
import io.vertx.core.net.ProxyOptions;

@SuppressWarnings("deprecation")
@Recorder
public class OTelExporterRecorder {
    public static final String BASE2EXPONENTIAL_AGGREGATION_NAME = AggregationUtil
            .aggregationName(Aggregation.base2ExponentialBucketHistogram());

    private final OTelBuildConfig buildConfig;
    private final RuntimeValue<OTelRuntimeConfig> runtimeConfig;
    private final RuntimeValue<OtlpExporterRuntimeConfig> exporterRuntimeConfig;

    public OTelExporterRecorder(
            final OTelBuildConfig buildConfig,
            final RuntimeValue<OTelRuntimeConfig> runtimeConfig,
            final RuntimeValue<OtlpExporterRuntimeConfig> exporterRuntimeConfig) {
        this.buildConfig = buildConfig;
        this.runtimeConfig = runtimeConfig;
        this.exporterRuntimeConfig = exporterRuntimeConfig;
    }

    public Function<SyntheticCreationalContext<LateBoundSpanProcessor>, LateBoundSpanProcessor> spanProcessorForOtlp(
            Supplier<Vertx> vertx) {
        URI baseUri = getTracesUri(exporterRuntimeConfig.getValue()); // do the creation and validation here in order to preserve backward compatibility
        return new Function<>() {
            @Override
            public LateBoundSpanProcessor apply(
                    SyntheticCreationalContext<LateBoundSpanProcessor> context) {
                if (runtimeConfig.getValue().sdkDisabled() || baseUri == null) {
                    return RemoveableLateBoundSpanProcessor.INSTANCE;
                }
                // Only create the OtlpGrpcSpanExporter if an endpoint was set in runtime config and was properly validated at startup
                Instance<SpanExporter> spanExporters = context.getInjectedReference(new TypeLiteral<>() {
                });
                if (!spanExporters.isUnsatisfied()) {
                    return RemoveableLateBoundSpanProcessor.INSTANCE;
                }

                try {
                    TlsConfigurationRegistry tlsConfigurationRegistry = context
                            .getInjectedReference(TlsConfigurationRegistry.class);
                    var spanExporter = createSpanExporter(exporterRuntimeConfig.getValue(), vertx.get(), baseUri,
                            tlsConfigurationRegistry);

                    if (buildConfig.simple()) {
                        SimpleSpanProcessorBuilder processorBuilder = SimpleSpanProcessor.builder(spanExporter);
                        return new LateBoundSpanProcessor(processorBuilder.build());
                    } else {
                        BatchSpanProcessorBuilder processorBuilder = BatchSpanProcessor.builder(spanExporter);

                        BatchSpanProcessorConfig bspc = runtimeConfig.getValue().bsp();
                        processorBuilder.setScheduleDelay(bspc.scheduleDelay());
                        processorBuilder.setMaxQueueSize(bspc.maxQueueSize());
                        processorBuilder.setMaxExportBatchSize(bspc.maxExportBatchSize());
                        processorBuilder.setExporterTimeout(bspc.exportTimeout());
                        // processorBuilder.setMeterProvider() // TODO add meter provider to span processor.

                        return new LateBoundSpanProcessor(processorBuilder.build());
                    }
                } catch (IllegalArgumentException iae) {
                    throw new IllegalStateException("Unable to install OTLP Exporter", iae);
                }
            }

            private SpanExporter createSpanExporter(OtlpExporterRuntimeConfig exporterRuntimeConfig,
                    Vertx vertx,
                    URI baseUri,
                    TlsConfigurationRegistry tlsConfigurationRegistry) {
                OtlpExporterTracesConfig tracesConfig = exporterRuntimeConfig.traces();
                if (tracesConfig.protocol().isEmpty()) {
                    throw new IllegalStateException("No OTLP protocol specified. " +
                            "Please check `quarkus.otel.exporter.otlp.traces.protocol` property");
                }

                String protocol = tracesConfig.protocol().get();
                if (GRPC.equals(protocol)) {
                    return createOtlpGrpcSpanExporter(exporterRuntimeConfig, vertx, baseUri, tlsConfigurationRegistry);
                } else if (HTTP_PROTOBUF.equals(protocol)) {
                    return createHttpSpanExporter(exporterRuntimeConfig, vertx, baseUri, protocol, tlsConfigurationRegistry);
                }

                throw new IllegalArgumentException(String.format("Unsupported OTLP protocol %s specified. " +
                        "Please check `quarkus.otel.exporter.otlp.traces.protocol` property", protocol));
            }

            private SpanExporter createOtlpGrpcSpanExporter(OtlpExporterRuntimeConfig exporterRuntimeConfig,
                    Vertx vertx, final URI baseUri,
                    TlsConfigurationRegistry tlsConfigurationRegistry) {

                OtlpExporterTracesConfig tracesConfig = exporterRuntimeConfig.traces();

                return new VertxGrpcSpanExporter(new GrpcExporter<TraceRequestMarshaler>(
                        OTLP_VALUE, // use the same as OTel does
                        "span", // use the same as OTel does
                        new VertxGrpcSender(
                                baseUri,
                                VertxGrpcSender.GRPC_TRACE_SERVICE_NAME,
                                determineCompression(tracesConfig),
                                tracesConfig.timeout(),
                                populateTracingExportHttpHeaders(tracesConfig),
                                new HttpClientOptionsConsumer(tracesConfig, baseUri, tlsConfigurationRegistry),
                                vertx),
                        MeterProvider::noop));
            }

            private SpanExporter createHttpSpanExporter(OtlpExporterRuntimeConfig exporterRuntimeConfig, Vertx vertx,
                    URI baseUri, String protocol,
                    TlsConfigurationRegistry tlsConfigurationRegistry) {

                OtlpExporterTracesConfig tracesConfig = exporterRuntimeConfig.traces();

                boolean exportAsJson = false; //TODO: this will be enhanced in the future

                return new VertxHttpSpanExporter(new HttpExporter<TraceRequestMarshaler>(
                        OTLP_VALUE, // use the same as OTel does
                        "span", // use the same as OTel does
                        new VertxHttpSender(
                                baseUri,
                                VertxHttpSender.TRACES_PATH,
                                determineCompression(tracesConfig),
                                tracesConfig.timeout(),
                                populateTracingExportHttpHeaders(tracesConfig),
                                exportAsJson ? "application/json" : "application/x-protobuf",
                                new HttpClientOptionsConsumer(tracesConfig, baseUri, tlsConfigurationRegistry),
                                vertx),
                        MeterProvider::noop,
                        exportAsJson));
            }
        };
    }

    public Function<SyntheticCreationalContext<MetricExporter>, MetricExporter> createMetricExporter(Supplier<Vertx> vertx) {

        final URI baseUri = getMetricsUri(exporterRuntimeConfig.getValue());

        return new Function<>() {
            @Override
            public MetricExporter apply(SyntheticCreationalContext<MetricExporter> context) {

                if (runtimeConfig.getValue().sdkDisabled() || baseUri == null) {
                    return NoopMetricExporter.INSTANCE;
                }

                MetricExporter metricExporter;

                try {
                    TlsConfigurationRegistry tlsConfigurationRegistry = context
                            .getInjectedReference(TlsConfigurationRegistry.class);
                    OtlpExporterMetricsConfig metricsConfig = exporterRuntimeConfig.getValue().metrics();
                    if (metricsConfig.protocol().isEmpty()) {
                        throw new IllegalStateException("No OTLP protocol specified. " +
                                "Please check `quarkus.otel.exporter.otlp.metrics.protocol` property");
                    }

                    String protocol = metricsConfig.protocol().get();
                    if (GRPC.equals(protocol)) {
                        metricExporter = new VertxGrpcMetricExporter(
                                new GrpcExporter<MetricsRequestMarshaler>(
                                        OTLP_VALUE, // use the same as OTel does
                                        "metric", // use the same as OTel does
                                        new VertxGrpcSender(
                                                baseUri,
                                                VertxGrpcSender.GRPC_METRIC_SERVICE_NAME,
                                                determineCompression(metricsConfig),
                                                metricsConfig.timeout(),
                                                populateTracingExportHttpHeaders(metricsConfig),
                                                new HttpClientOptionsConsumer(metricsConfig, baseUri, tlsConfigurationRegistry),
                                                vertx.get()),
                                        MeterProvider::noop),
                                aggregationTemporalityResolver(metricsConfig),
                                aggregationResolver(metricsConfig));
                    } else if (HTTP_PROTOBUF.equals(protocol)) {
                        boolean exportAsJson = false; //TODO: this will be enhanced in the future
                        metricExporter = new VertxHttpMetricsExporter(
                                new HttpExporter<MetricsRequestMarshaler>(
                                        OTLP_VALUE, // use the same as OTel does
                                        "metric", // use the same as OTel does
                                        new VertxHttpSender(
                                                baseUri,
                                                VertxHttpSender.METRICS_PATH,
                                                determineCompression(metricsConfig),
                                                metricsConfig.timeout(),
                                                populateTracingExportHttpHeaders(metricsConfig),
                                                exportAsJson ? "application/json" : "application/x-protobuf",
                                                new HttpClientOptionsConsumer(metricsConfig, baseUri, tlsConfigurationRegistry),
                                                vertx.get()),
                                        MeterProvider::noop,
                                        exportAsJson),
                                aggregationTemporalityResolver(metricsConfig),
                                aggregationResolver(metricsConfig));
                    } else {
                        throw new IllegalArgumentException(String.format("Unsupported OTLP protocol %s specified. " +
                                "Please check `quarkus.otel.exporter.otlp.metrics.protocol` property", protocol));
                    }

                } catch (IllegalArgumentException iae) {
                    throw new IllegalStateException("Unable to install OTLP Exporter", iae);
                }
                return metricExporter;
            }
        };
    }

    public Function<SyntheticCreationalContext<LogRecordExporter>, LogRecordExporter> createLogRecordExporter(
            Supplier<Vertx> vertx) {
        final URI baseUri = getLogsUri(exporterRuntimeConfig.getValue());

        return new Function<>() {
            @Override
            public LogRecordExporter apply(SyntheticCreationalContext<LogRecordExporter> context) {

                if (runtimeConfig.getValue().sdkDisabled() || baseUri == null) {
                    return NoopLogRecordExporter.INSTANCE;
                }

                LogRecordExporter logRecordExporter;

                try {
                    TlsConfigurationRegistry tlsConfigurationRegistry = context
                            .getInjectedReference(TlsConfigurationRegistry.class);
                    OtlpExporterLogsConfig logsConfig = exporterRuntimeConfig.getValue().logs();
                    if (logsConfig.protocol().isEmpty()) {
                        throw new IllegalStateException("No OTLP protocol specified. " +
                                "Please check `quarkus.otel.exporter.otlp.logs.protocol` property");
                    }

                    String protocol = logsConfig.protocol().get();
                    if (GRPC.equals(protocol)) {
                        logRecordExporter = new VertxGrpcLogRecordExporter(
                                new GrpcExporter<LogsRequestMarshaler>(
                                        OTLP_VALUE, // use the same as OTel does
                                        "log", // use the same as OTel does
                                        new VertxGrpcSender(
                                                baseUri,
                                                VertxGrpcSender.GRPC_LOG_SERVICE_NAME,
                                                determineCompression(logsConfig),
                                                logsConfig.timeout(),
                                                populateTracingExportHttpHeaders(logsConfig),
                                                new HttpClientOptionsConsumer(logsConfig, baseUri, tlsConfigurationRegistry),
                                                vertx.get()),
                                        MeterProvider::noop));
                    } else if (HTTP_PROTOBUF.equals(protocol)) {
                        boolean exportAsJson = false; //TODO: this will be enhanced in the future
                        logRecordExporter = new VertxHttpLogRecordExporter(
                                new HttpExporter<LogsRequestMarshaler>(
                                        OTLP_VALUE, // use the same as OTel does
                                        "log", // use the same as OTel does
                                        new VertxHttpSender(
                                                baseUri,
                                                VertxHttpSender.LOGS_PATH,
                                                determineCompression(logsConfig),
                                                logsConfig.timeout(),
                                                populateTracingExportHttpHeaders(logsConfig),
                                                exportAsJson ? "application/json" : "application/x-protobuf",
                                                new HttpClientOptionsConsumer(logsConfig, baseUri, tlsConfigurationRegistry),
                                                vertx.get()),
                                        MeterProvider::noop,
                                        exportAsJson));
                    } else {
                        throw new IllegalArgumentException(String.format("Unsupported OTLP protocol %s specified. " +
                                "Please check `quarkus.otel.exporter.otlp.logs.protocol` property", protocol));
                    }

                } catch (IllegalArgumentException iae) {
                    throw new IllegalStateException("Unable to install OTLP Exporter", iae);
                }
                return logRecordExporter;
            }
        };
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

    private static boolean determineCompression(OtlpExporterConfig config) {
        if (config.compression().isPresent()) {
            return (config.compression().get() == CompressionType.GZIP);
        }
        return false;
    }

    private static Map<String, String> populateTracingExportHttpHeaders(OtlpExporterConfig config) {
        Map<String, String> headersMap = new HashMap<>();
        OtlpUserAgent.addUserAgentHeader(headersMap::put);
        if (config.headers().isPresent()) {
            List<String> headers = config.headers().get();
            if (!headers.isEmpty()) {
                for (String header : headers) {
                    if (header.isEmpty()) {
                        continue;
                    }
                    String[] parts = header.split("=", 2);
                    String key = parts[0].trim();
                    String value = parts[1].trim();
                    headersMap.put(key, value);
                }
            }
        }
        return headersMap;
    }

    private URI getTracesUri(OtlpExporterRuntimeConfig exporterRuntimeConfig) {
        String endpoint = resolveTraceEndpoint(exporterRuntimeConfig);
        if (endpoint.isEmpty()) {
            return null;
        }
        return ExporterBuilderUtil.validateEndpoint(endpoint);
    }

    private URI getMetricsUri(OtlpExporterRuntimeConfig exporterRuntimeConfig) {
        String endpoint = resolveMetricEndpoint(exporterRuntimeConfig);
        if (endpoint.isEmpty()) {
            return null;
        }
        return ExporterBuilderUtil.validateEndpoint(endpoint);
    }

    private URI getLogsUri(OtlpExporterRuntimeConfig exporterRuntimeConfig) {
        String endpoint = resolveLogsEndpoint(exporterRuntimeConfig);
        if (endpoint.isEmpty()) {
            return null;
        }
        return ExporterBuilderUtil.validateEndpoint(endpoint);
    }

    static String resolveTraceEndpoint(final OtlpExporterRuntimeConfig runtimeConfig) {
        String endpoint = runtimeConfig.traces().endpoint()
                .filter(OTelExporterRecorder::excludeDefaultEndpoint)
                .orElse(runtimeConfig.endpoint()
                        .filter(OTelExporterRecorder::excludeDefaultEndpoint)
                        .orElse(DEFAULT_GRPC_BASE_URI));
        return endpoint.trim();
    }

    static String resolveMetricEndpoint(final OtlpExporterRuntimeConfig runtimeConfig) {
        String endpoint = runtimeConfig.metrics().endpoint()
                .filter(OTelExporterRecorder::excludeDefaultEndpoint)
                .orElse(runtimeConfig.endpoint()
                        .filter(OTelExporterRecorder::excludeDefaultEndpoint)
                        .orElse(DEFAULT_GRPC_BASE_URI));
        return endpoint.trim();
    }

    static String resolveLogsEndpoint(final OtlpExporterRuntimeConfig runtimeConfig) {
        String endpoint = runtimeConfig.logs().endpoint()
                .filter(OTelExporterRecorder::excludeDefaultEndpoint)
                .orElse(runtimeConfig.endpoint()
                        .filter(OTelExporterRecorder::excludeDefaultEndpoint)
                        .orElse(DEFAULT_GRPC_BASE_URI));
        return endpoint.trim();
    }

    private static boolean excludeDefaultEndpoint(String endpoint) {
        return !DEFAULT_GRPC_BASE_URI.equals(endpoint);
    }

    static class HttpClientOptionsConsumer implements Consumer<HttpClientOptions> {
        private final OtlpExporterConfig config;
        private final URI baseUri;
        private final Optional<TlsConfiguration> maybeTlsConfiguration;
        private final TlsConfigurationRegistry tlsConfigurationRegistry;

        public HttpClientOptionsConsumer(OtlpExporterConfig config, URI baseUri,
                TlsConfigurationRegistry tlsConfigurationRegistry) {
            this.config = config;
            this.baseUri = baseUri;
            this.maybeTlsConfiguration = TlsConfiguration.from(tlsConfigurationRegistry, config.tlsConfigurationName());
            this.tlsConfigurationRegistry = tlsConfigurationRegistry;
        }

        @Override
        public void accept(HttpClientOptions options) {
            configureTLS(options);
            if (config.proxyOptions().enabled()) {
                configureProxyOptions(options);
            }
        }

        private void configureTLS(HttpClientOptions options) {
            configureKeyCertOptions(options);
            configureTrustOptions(options);

            if (OTelExporterUtil.isHttps(baseUri)) {
                options.setSsl(true);
                options.setUseAlpn(true);
            }

            boolean trustAll = maybeTlsConfiguration.map(TlsConfiguration::isTrustAll).orElseGet(
                    new Supplier<>() {
                        @Override
                        public Boolean get() {
                            return tlsConfigurationRegistry.getDefault().map(TlsConfiguration::isTrustAll).orElse(false);
                        }
                    });
            if (trustAll) {
                options.setTrustAll(true);
                options.setVerifyHost(false);
            }
        }

        private void configureProxyOptions(HttpClientOptions options) {
            var proxyConfig = config.proxyOptions();
            Optional<String> proxyHost = proxyConfig.host();
            if (proxyHost.isPresent()) {
                ProxyOptions proxyOptions = new ProxyOptions()
                        .setHost(proxyHost.get());
                if (proxyConfig.port().isPresent()) {
                    proxyOptions.setPort(proxyConfig.port().getAsInt());
                }
                if (proxyConfig.username().isPresent()) {
                    proxyOptions.setUsername(proxyConfig.username().get());
                }
                if (proxyConfig.password().isPresent()) {
                    proxyOptions.setPassword(proxyConfig.password().get());
                }
                options.setProxyOptions(proxyOptions);
            } else {
                configureProxyOptionsFromJDKSysProps(options);
            }
        }

        private void configureProxyOptionsFromJDKSysProps(HttpClientOptions options) {
            String proxyHost = options.isSsl()
                    ? System.getProperty("https.proxyHost", "none")
                    : System.getProperty("http.proxyHost", "none");
            String proxyPortAsString = options.isSsl()
                    ? System.getProperty("https.proxyPort", "443")
                    : System.getProperty("http.proxyPort", "80");
            int proxyPort = Integer.parseInt(proxyPortAsString);

            if (!"none".equals(proxyHost)) {
                ProxyOptions proxyOptions = new ProxyOptions().setHost(proxyHost).setPort(proxyPort);
                String proxyUser = options.isSsl()
                        ? System.getProperty("https.proxyUser")
                        : System.getProperty("http.proxyUser");
                if (proxyUser != null && !proxyUser.isBlank()) {
                    proxyOptions.setUsername(proxyUser);
                }
                String proxyPassword = options.isSsl()
                        ? System.getProperty("https.proxyPassword")
                        : System.getProperty("http.proxyPassword");
                if (proxyPassword != null && !proxyPassword.isBlank()) {
                    proxyOptions.setPassword(proxyPassword);
                }
                options.setProxyOptions(proxyOptions);
            }
        }

        private void configureKeyCertOptions(HttpClientOptions options) {
            if (maybeTlsConfiguration.isPresent()) {
                options.setKeyCertOptions(maybeTlsConfiguration.get().getKeyStoreOptions());
                return;
            }

            OtlpExporterTracesConfig.KeyCert keyCert = config.keyCert();
            if (keyCert.certs().isEmpty() && keyCert.keys().isEmpty()) {
                return;
            }

            PemKeyCertOptions pemKeyCertOptions = new PemKeyCertOptions();
            if (keyCert.certs().isPresent()) {
                for (String cert : keyCert.certs().get()) {
                    pemKeyCertOptions.addCertPath(cert);
                }
            }
            if (keyCert.keys().isPresent()) {
                for (String cert : keyCert.keys().get()) {
                    pemKeyCertOptions.addKeyPath(cert);
                }
            }
            options.setKeyCertOptions(pemKeyCertOptions);
        }

        private void configureTrustOptions(HttpClientOptions options) {
            if (maybeTlsConfiguration.isPresent()) {
                options.setTrustOptions(maybeTlsConfiguration.get().getTrustStoreOptions());
                return;
            }

            OtlpExporterTracesConfig.TrustCert trustCert = config.trustCert();
            if (trustCert.certs().isPresent()) {
                List<String> certs = trustCert.certs().get();
                if (!certs.isEmpty()) {
                    PemTrustOptions pemTrustOptions = new PemTrustOptions();
                    for (String cert : trustCert.certs().get()) {
                        pemTrustOptions.addCertPath(cert);
                    }
                    options.setPemTrustOptions(pemTrustOptions);
                }
            }
        }
    }
}
