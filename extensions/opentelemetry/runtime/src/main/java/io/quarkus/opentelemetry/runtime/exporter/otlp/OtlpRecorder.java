package io.quarkus.opentelemetry.runtime.exporter.otlp;

import static io.quarkus.opentelemetry.runtime.config.runtime.exporter.OtlpExporterRuntimeConfig.DEFAULT_GRPC_BASE_URI;
import static io.quarkus.opentelemetry.runtime.config.runtime.exporter.OtlpExporterTracesConfig.Protocol.HTTP_PROTOBUF;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.spi.CDI;

import io.opentelemetry.api.metrics.MeterProvider;
import io.opentelemetry.exporter.internal.ExporterBuilderUtil;
import io.opentelemetry.exporter.internal.otlp.OtlpUserAgent;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessorBuilder;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import io.quarkus.opentelemetry.runtime.config.runtime.OTelRuntimeConfig;
import io.quarkus.opentelemetry.runtime.config.runtime.exporter.CompressionType;
import io.quarkus.opentelemetry.runtime.config.runtime.exporter.OtlpExporterRuntimeConfig;
import io.quarkus.opentelemetry.runtime.config.runtime.exporter.OtlpExporterTracesConfig;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.net.KeyCertOptions;
import io.vertx.core.net.PemKeyCertOptions;
import io.vertx.core.net.PemTrustOptions;

@Recorder
public class OtlpRecorder {

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

    public void installBatchSpanProcessorForOtlp(
            OTelRuntimeConfig otelRuntimeConfig,
            OtlpExporterRuntimeConfig exporterRuntimeConfig,
            RuntimeValue<Vertx> vertx,
            LaunchMode launchMode) {

        if (otelRuntimeConfig.sdkDisabled()) {
            return;
        }
        String grpcBaseUri = resolveEndpoint(exporterRuntimeConfig).trim();

        // Only create the OtlpGrpcSpanExporter if an endpoint was set in runtime config
        if (grpcBaseUri.length() > 0) {
            try {
                // Load span exporter if provided by user
                SpanExporter spanExporter = CDI.current()
                        .select(SpanExporter.class, Any.Literal.INSTANCE).stream().findFirst().orElse(null);
                // CDI exporter was already added to a processor by OTEL
                if (spanExporter == null) {
                    spanExporter = createOtlpGrpcSpanExporter(exporterRuntimeConfig, grpcBaseUri, vertx.getValue());

                    // Create BatchSpanProcessor for OTLP and install into LateBoundBatchSpanProcessor
                    LateBoundBatchSpanProcessor delayedProcessor = CDI.current()
                            .select(LateBoundBatchSpanProcessor.class, Any.Literal.INSTANCE).get();

                    BatchSpanProcessorBuilder processorBuilder = BatchSpanProcessor.builder(spanExporter);

                    processorBuilder.setScheduleDelay(otelRuntimeConfig.bsp().scheduleDelay());
                    processorBuilder.setMaxQueueSize(otelRuntimeConfig.bsp().maxQueueSize());
                    processorBuilder.setMaxExportBatchSize(otelRuntimeConfig.bsp().maxExportBatchSize());
                    processorBuilder.setExporterTimeout(otelRuntimeConfig.bsp().exportTimeout());
                    // processorBuilder.setMeterProvider() // TODO add meter provider to span processor.

                    delayedProcessor.setBatchSpanProcessorDelegate(processorBuilder.build());
                }
            } catch (IllegalArgumentException iae) {
                throw new IllegalStateException("Unable to install OTLP Exporter", iae);
            }
        }
    }

    private SpanExporter createOtlpGrpcSpanExporter(OtlpExporterRuntimeConfig exporterRuntimeConfig, String endpoint,
            Vertx vertx) {

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

        URI grpcBaseUri = ExporterBuilderUtil.validateEndpoint(endpoint);
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
}
