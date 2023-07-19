package io.quarkus.opentelemetry.runtime.exporter.otlp;

import static io.quarkus.opentelemetry.runtime.config.runtime.exporter.OtlpExporterRuntimeConfig.DEFAULT_GRPC_BASE_URI;
import static io.quarkus.opentelemetry.runtime.config.runtime.exporter.OtlpExporterTracesConfig.Protocol.HTTP_PROTOBUF;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.util.TypeLiteral;

import io.opentelemetry.api.metrics.MeterProvider;
import io.opentelemetry.exporter.internal.ExporterBuilderUtil;
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

@SuppressWarnings("deprecation")
@Recorder
public class OtlpRecorder {

    public Function<SyntheticCreationalContext<LateBoundBatchSpanProcessor>, LateBoundBatchSpanProcessor> batchSpanProcessorForOtlp(
            OTelRuntimeConfig otelRuntimeConfig,
            OtlpExporterRuntimeConfig exporterRuntimeConfig,
            TlsConfig tlsConfig, Supplier<Vertx> vertx) {
        URI grpcBaseUri = getGrpcBaseUri(exporterRuntimeConfig); // do the creation and validation here in order to preserve backward compatibility
        return new Function<>() {
            @Override
            public LateBoundBatchSpanProcessor apply(
                    SyntheticCreationalContext<LateBoundBatchSpanProcessor> context) {
                if (otelRuntimeConfig.sdkDisabled() || grpcBaseUri == null) {
                    return RemoveableLateBoundBatchSpanProcessor.INSTANCE;
                }
                // Only create the OtlpGrpcSpanExporter if an endpoint was set in runtime config and was properly validated at startup
                Instance<SpanExporter> spanExporters = context.getInjectedReference(new TypeLiteral<>() {
                });
                if (!spanExporters.isUnsatisfied()) {
                    return RemoveableLateBoundBatchSpanProcessor.INSTANCE;
                }

                try {
                    var spanExporter = createOtlpGrpcSpanExporter(exporterRuntimeConfig, vertx.get(), grpcBaseUri);

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

            private SpanExporter createOtlpGrpcSpanExporter(OtlpExporterRuntimeConfig exporterRuntimeConfig,
                    Vertx vertx, final URI grpcBaseUri) {

                OtlpExporterTracesConfig tracesConfig = exporterRuntimeConfig.traces();
                if (tracesConfig.protocol().isPresent()) {
                    if (!tracesConfig.protocol().get().equals(HTTP_PROTOBUF)) {
                        throw new IllegalStateException("Only the GRPC Exporter is currently supported. " +
                                "Please check `quarkus.otel.exporter.otlp.traces.protocol` property");
                    }
                }

                boolean compressionEnabled = false;
                if (tracesConfig.compression().isPresent()) {
                    compressionEnabled = (tracesConfig.compression().get() == CompressionType.GZIP);
                }

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

                return new VertxGrpcExporter(
                        "otlp", // use the same as OTel does
                        "span", // use the same as OTel does
                        MeterProvider::noop,
                        grpcBaseUri,
                        compressionEnabled,
                        tracesConfig.timeout(),
                        headersMap,
                        new Consumer<>() {
                            @Override
                            public void accept(HttpClientOptions options) {
                                configureTLS(options);
                            }

                            private void configureTLS(HttpClientOptions options) {
                                // TODO: this can reuse existing stuff when https://github.com/quarkusio/quarkus/pull/33228 is in
                                options.setKeyCertOptions(toPemKeyCertOptions(tracesConfig));
                                options.setPemTrustOptions(toPemTrustOptions(tracesConfig));

                                if (VertxGrpcExporter.isHttps(grpcBaseUri)) {
                                    options.setSsl(true);
                                    options.setUseAlpn(true);
                                }
                                if (tlsConfig.trustAll) {
                                    options.setTrustAll(true);
                                    options.setVerifyHost(false);
                                }
                            }

                            private KeyCertOptions toPemKeyCertOptions(OtlpExporterTracesConfig configuration) {
                                PemKeyCertOptions pemKeyCertOptions = new PemKeyCertOptions();
                                OtlpExporterTracesConfig.KeyCert keyCert = configuration.keyCert();
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

                            private PemTrustOptions toPemTrustOptions(OtlpExporterTracesConfig configuration) {
                                PemTrustOptions pemTrustOptions = new PemTrustOptions();
                                OtlpExporterTracesConfig.TrustCert trustCert = configuration.trustCert();
                                if (trustCert.certs().isPresent()) {
                                    for (String cert : trustCert.certs().get()) {
                                        pemTrustOptions.addCertPath(cert);
                                    }
                                }
                                return pemTrustOptions;
                            }
                        },
                        vertx);

            }
        };
    }

    private URI getGrpcBaseUri(OtlpExporterRuntimeConfig exporterRuntimeConfig) {
        String endpoint = resolveEndpoint(exporterRuntimeConfig).trim();
        if (endpoint.isEmpty()) {
            return null;
        }
        return ExporterBuilderUtil.validateEndpoint(endpoint);
    }

    static String resolveEndpoint(final OtlpExporterRuntimeConfig runtimeConfig) {
        String endpoint = runtimeConfig.traces().legacyEndpoint()
                .filter(OtlpRecorder::excludeDefaultEndpoint)
                .orElse(runtimeConfig.traces().endpoint()
                        .filter(OtlpRecorder::excludeDefaultEndpoint)
                        .orElse(runtimeConfig.endpoint()
                                .filter(OtlpRecorder::excludeDefaultEndpoint)
                                .orElse(DEFAULT_GRPC_BASE_URI)));
        return endpoint.trim();
    }

    private static boolean excludeDefaultEndpoint(String endpoint) {
        return !DEFAULT_GRPC_BASE_URI.equals(endpoint);
    }

}
