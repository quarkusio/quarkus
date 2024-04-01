package io.quarkus.opentelemetry.runtime.exporter.otlp;

import static io.quarkus.opentelemetry.runtime.config.runtime.exporter.OtlpExporterRuntimeConfig.DEFAULT_GRPC_BASE_URI;
import static io.quarkus.opentelemetry.runtime.config.runtime.exporter.OtlpExporterTracesConfig.Protocol.GRPC;
import static io.quarkus.opentelemetry.runtime.config.runtime.exporter.OtlpExporterTracesConfig.Protocol.HTTP_PROTOBUF;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.util.TypeLiteral;

import io.opentelemetry.api.metrics.MeterProvider;
import io.opentelemetry.exporter.internal.ExporterBuilderUtil;
import io.opentelemetry.exporter.internal.http.HttpExporter;
import io.opentelemetry.exporter.internal.otlp.traces.TraceRequestMarshaler;
import io.opentelemetry.exporter.otlp.internal.OtlpUserAgent;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessorBuilder;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import io.quarkus.arc.SyntheticCreationalContext;
import io.quarkus.opentelemetry.runtime.config.runtime.OTelRuntimeConfig;
import io.quarkus.opentelemetry.runtime.config.runtime.exporter.CompressionType;
import io.quarkus.opentelemetry.runtime.config.runtime.exporter.OtlpExporterRuntimeConfig;
import io.quarkus.opentelemetry.runtime.config.runtime.exporter.OtlpExporterTracesConfig;
import io.quarkus.runtime.TlsConfig;
import io.quarkus.runtime.annotations.Recorder;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.net.KeyCertOptions;
import io.vertx.core.net.PemKeyCertOptions;
import io.vertx.core.net.PemTrustOptions;
import io.vertx.core.net.ProxyOptions;

@SuppressWarnings("deprecation")
@Recorder
public class OTelExporterRecorder {

    public Function<SyntheticCreationalContext<LateBoundBatchSpanProcessor>, LateBoundBatchSpanProcessor> batchSpanProcessorForOtlp(
            OTelRuntimeConfig otelRuntimeConfig,
            OtlpExporterRuntimeConfig exporterRuntimeConfig,
            TlsConfig tlsConfig, Supplier<Vertx> vertx) {
        URI baseUri = getBaseUri(exporterRuntimeConfig); // do the creation and validation here in order to preserve backward compatibility
        return new Function<>() {
            @Override
            public LateBoundBatchSpanProcessor apply(
                    SyntheticCreationalContext<LateBoundBatchSpanProcessor> context) {
                if (otelRuntimeConfig.sdkDisabled() || baseUri == null) {
                    return RemoveableLateBoundBatchSpanProcessor.INSTANCE;
                }
                // Only create the OtlpGrpcSpanExporter if an endpoint was set in runtime config and was properly validated at startup
                Instance<SpanExporter> spanExporters = context.getInjectedReference(new TypeLiteral<>() {
                });
                if (!spanExporters.isUnsatisfied()) {
                    return RemoveableLateBoundBatchSpanProcessor.INSTANCE;
                }

                try {
                    var spanExporter = createSpanExporter(exporterRuntimeConfig, vertx.get(), baseUri);

                    BatchSpanProcessorBuilder processorBuilder = BatchSpanProcessor.builder(spanExporter);

                    processorBuilder.setScheduleDelay(otelRuntimeConfig.bsp().scheduleDelay());
                    processorBuilder.setMaxQueueSize(otelRuntimeConfig.bsp().maxQueueSize());
                    processorBuilder.setMaxExportBatchSize(otelRuntimeConfig.bsp().maxExportBatchSize());
                    processorBuilder.setExporterTimeout(otelRuntimeConfig.bsp().exportTimeout());
                    // processorBuilder.setMeterProvider() // TODO add meter provider to span processor.

                    return new LateBoundBatchSpanProcessor(processorBuilder.build());
                } catch (IllegalArgumentException iae) {
                    throw new IllegalStateException("Unable to install OTLP Exporter", iae);
                }
            }

            private SpanExporter createSpanExporter(OtlpExporterRuntimeConfig exporterRuntimeConfig,
                    Vertx vertx, final URI baseUri) {
                OtlpExporterTracesConfig tracesConfig = exporterRuntimeConfig.traces();
                if (tracesConfig.protocol().isEmpty()) {
                    throw new IllegalStateException("No OTLP protocol specified. " +
                            "Please check `quarkus.otel.exporter.otlp.traces.protocol` property");
                }

                String protocol = tracesConfig.protocol().get();
                if (GRPC.equals(protocol)) {
                    return createOtlpGrpcSpanExporter(exporterRuntimeConfig, vertx, baseUri);
                } else if (HTTP_PROTOBUF.equals(protocol)) {
                    return createHttpSpanExporter(exporterRuntimeConfig, vertx, baseUri, protocol);
                }

                throw new IllegalArgumentException(String.format("Unsupported OTLP protocol %s specified. " +
                        "Please check `quarkus.otel.exporter.otlp.traces.protocol` property", protocol));
            }

            private SpanExporter createOtlpGrpcSpanExporter(OtlpExporterRuntimeConfig exporterRuntimeConfig,
                    Vertx vertx, final URI baseUri) {

                OtlpExporterTracesConfig tracesConfig = exporterRuntimeConfig.traces();

                return new VertxGrpcExporter(
                        "otlp", // use the same as OTel does
                        "span", // use the same as OTel does
                        MeterProvider::noop,
                        baseUri,
                        determineCompression(tracesConfig),
                        tracesConfig.timeout(),
                        populateTracingExportHttpHeaders(tracesConfig),
                        new HttpClientOptionsConsumer(tracesConfig, baseUri, tlsConfig),
                        vertx);

            }

            private SpanExporter createHttpSpanExporter(OtlpExporterRuntimeConfig exporterRuntimeConfig, Vertx vertx,
                    URI baseUri, String protocol) {

                OtlpExporterTracesConfig tracesConfig = exporterRuntimeConfig.traces();

                boolean exportAsJson = false; //TODO: this will be enhanced in the future

                return new VertxHttpExporter(new HttpExporter<TraceRequestMarshaler>(
                        "otlp", // use the same as OTel does
                        "span", // use the same as OTel does
                        new VertxHttpExporter.VertxHttpSender(
                                baseUri,
                                determineCompression(tracesConfig),
                                tracesConfig.timeout(),
                                populateTracingExportHttpHeaders(tracesConfig),
                                exportAsJson ? "application/json" : "application/x-protobuf",
                                new HttpClientOptionsConsumer(tracesConfig, baseUri, tlsConfig),
                                vertx),
                        MeterProvider::noop,
                        exportAsJson));
            }
        };
    }

    private static boolean determineCompression(OtlpExporterTracesConfig tracesConfig) {
        if (tracesConfig.compression().isPresent()) {
            return (tracesConfig.compression().get() == CompressionType.GZIP);
        }
        return false;
    }

    private static Map<String, String> populateTracingExportHttpHeaders(OtlpExporterTracesConfig tracesConfig) {
        Map<String, String> headersMap = new HashMap<>();
        OtlpUserAgent.addUserAgentHeader(headersMap::put);
        if (tracesConfig.headers().isPresent()) {
            List<String> headers = tracesConfig.headers().get();
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

    private URI getBaseUri(OtlpExporterRuntimeConfig exporterRuntimeConfig) {
        String endpoint = resolveEndpoint(exporterRuntimeConfig).trim();
        if (endpoint.isEmpty()) {
            return null;
        }
        return ExporterBuilderUtil.validateEndpoint(endpoint);
    }

    static String resolveEndpoint(final OtlpExporterRuntimeConfig runtimeConfig) {
        String endpoint = runtimeConfig.traces().legacyEndpoint()
                .filter(OTelExporterRecorder::excludeDefaultEndpoint)
                .orElse(runtimeConfig.traces().endpoint()
                        .filter(OTelExporterRecorder::excludeDefaultEndpoint)
                        .orElse(runtimeConfig.endpoint()
                                .filter(OTelExporterRecorder::excludeDefaultEndpoint)
                                .orElse(DEFAULT_GRPC_BASE_URI)));
        return endpoint.trim();
    }

    private static boolean excludeDefaultEndpoint(String endpoint) {
        return !DEFAULT_GRPC_BASE_URI.equals(endpoint);
    }

    static class HttpClientOptionsConsumer implements Consumer<HttpClientOptions> {
        private final OtlpExporterTracesConfig tracesConfig;
        private final URI baseUri;
        private final TlsConfig tlsConfig;

        public HttpClientOptionsConsumer(OtlpExporterTracesConfig tracesConfig, URI baseUri, TlsConfig tlsConfig) {
            this.tracesConfig = tracesConfig;
            this.baseUri = baseUri;
            this.tlsConfig = tlsConfig;
        }

        @Override
        public void accept(HttpClientOptions options) {
            configureTLS(options);
            if (tracesConfig.proxyOptions().enabled()) {
                configureProxyOptions(options);
            }
        }

        private void configureTLS(HttpClientOptions options) {
            // TODO: this can reuse existing stuff when https://github.com/quarkusio/quarkus/pull/33228 is in
            options.setKeyCertOptions(toPemKeyCertOptions());
            options.setPemTrustOptions(toPemTrustOptions());

            if (OTelExporterUtil.isHttps(baseUri)) {
                options.setSsl(true);
                options.setUseAlpn(true);
            }
            if (tlsConfig.trustAll) {
                options.setTrustAll(true);
                options.setVerifyHost(false);
            }
        }

        private void configureProxyOptions(HttpClientOptions options) {
            var proxyConfig = tracesConfig.proxyOptions();
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

        private KeyCertOptions toPemKeyCertOptions() {
            OtlpExporterTracesConfig.KeyCert keyCert = tracesConfig.keyCert();
            if (keyCert.certs().isEmpty() && keyCert.keys().isEmpty()) {
                return null;
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
            return pemKeyCertOptions;
        }

        private PemTrustOptions toPemTrustOptions() {
            OtlpExporterTracesConfig.TrustCert trustCert = tracesConfig.trustCert();
            if (trustCert.certs().isPresent()) {
                List<String> certs = trustCert.certs().get();
                if (!certs.isEmpty()) {
                    PemTrustOptions pemTrustOptions = new PemTrustOptions();
                    for (String cert : trustCert.certs().get()) {
                        pemTrustOptions.addCertPath(cert);
                    }
                    return pemTrustOptions;
                }
            }
            return null;
        }
    }
}
