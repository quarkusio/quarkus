package io.quarkus.opentelemetry.runtime.exporter.otlp;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

import io.netty.handler.codec.http.QueryStringDecoder;
import io.opentelemetry.api.metrics.MeterProvider;
import io.opentelemetry.exporter.internal.ExporterMetrics;
import io.opentelemetry.exporter.internal.otlp.traces.TraceRequestMarshaler;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.internal.ThrottlingLogger;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import io.quarkus.vertx.core.runtime.BufferOutputStream;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.net.SocketAddress;
import io.vertx.core.tracing.TracingPolicy;
import io.vertx.grpc.client.GrpcClient;
import io.vertx.grpc.client.GrpcClientRequest;
import io.vertx.grpc.client.GrpcClientResponse;
import io.vertx.grpc.common.GrpcStatus;
import io.vertx.grpc.common.ServiceName;

final class VertxGrpcExporter implements SpanExporter {

    private static final String GRPC_SERVICE_NAME = "opentelemetry.proto.collector.trace.v1.TraceService";
    private static final String GRPC_METHOD_NAME = "Export";

    private static final String GRPC_STATUS = "grpc-status";
    private static final String GRPC_MESSAGE = "grpc-message";

    private static final Logger internalLogger = Logger.getLogger(VertxGrpcExporter.class.getName());

    private final ThrottlingLogger logger = new ThrottlingLogger(internalLogger); // TODO: is there something in JBoss Logging we can use?

    // We only log unimplemented once since it's a configuration issue that won't be recovered.
    private final AtomicBoolean loggedUnimplemented = new AtomicBoolean();
    private final AtomicBoolean isShutdown = new AtomicBoolean();
    private final String type;
    private final ExporterMetrics exporterMetrics;
    private final SocketAddress server;
    private final boolean compressionEnabled;
    private final Map<String, String> headers;

    private final GrpcClient client;

    VertxGrpcExporter(
            String exporterName,
            String type,
            Supplier<MeterProvider> meterProviderSupplier,
            URI grpcBaseUri, boolean compressionEnabled,
            Duration timeout,
            Map<String, String> headersMap,
            Consumer<HttpClientOptions> clientOptionsCustomizer,
            Vertx vertx) {
        this.type = type;
        this.exporterMetrics = ExporterMetrics.createGrpcOkHttp(exporterName, type, meterProviderSupplier);
        this.server = SocketAddress.inetSocketAddress(OtlpExporterUtil.getPort(grpcBaseUri), grpcBaseUri.getHost());
        this.compressionEnabled = compressionEnabled;
        this.headers = headersMap;
        var httpClientOptions = new HttpClientOptions()
                .setHttp2ClearTextUpgrade(false) // needed otherwise connections get closed immediately
                .setReadIdleTimeout((int) timeout.getSeconds())
                .setTracingPolicy(TracingPolicy.IGNORE); // needed to avoid tracing the calls from this gRPC client
        clientOptionsCustomizer.accept(httpClientOptions);
        this.client = GrpcClient.client(vertx, httpClientOptions);
    }

    private CompletableResultCode export(TraceRequestMarshaler marshaler, int numItems) {
        if (isShutdown.get()) {
            return CompletableResultCode.ofFailure();
        }

        exporterMetrics.addSeen(numItems);

        var result = new CompletableResultCode();
        var onSuccessHandler = new ClientRequestOnSuccessHandler(headers, compressionEnabled, exporterMetrics, marshaler,
                loggedUnimplemented, logger, type, numItems, result);
        client.request(server)
                .onSuccess(onSuccessHandler)
                .onFailure(new Handler<>() {
                    @Override
                    public void handle(Throwable t) {
                        // TODO: is there a better way todo retry?
                        // TODO: should we only retry on a specific errors?

                        client.request(server)
                                .onSuccess(onSuccessHandler)
                                .onFailure(new Handler<>() {
                                    @Override
                                    public void handle(Throwable event) {
                                        failOnClientRequest(numItems, t, result);
                                    }
                                });
                    }
                });

        return result;
    }

    private void failOnClientRequest(int numItems, Throwable t, CompletableResultCode result) {
        exporterMetrics.addFailed(numItems);
        logger.log(
                Level.SEVERE,
                "Failed to export "
                        + type
                        + "s. The request could not be executed. Full error message: "
                        + t.getMessage());
        result.fail();
    }

    @Override
    public CompletableResultCode export(Collection<SpanData> spans) {
        TraceRequestMarshaler request = TraceRequestMarshaler.create(spans);

        return export(request, spans.size());
    }

    @Override
    public CompletableResultCode flush() {
        return CompletableResultCode.ofSuccess();
    }

    @Override
    public CompletableResultCode shutdown() {
        if (!isShutdown.compareAndSet(false, true)) {
            logger.log(Level.INFO, "Calling shutdown() multiple times.");
            return CompletableResultCode.ofSuccess();
        }
        client.close();
        return CompletableResultCode.ofSuccess();
    }

    private static final class ClientRequestOnSuccessHandler implements Handler<GrpcClientRequest<Buffer, Buffer>> {

        private final Map<String, String> headers;
        private final boolean compressionEnabled;
        private final ExporterMetrics exporterMetrics;

        private final TraceRequestMarshaler marshaler;
        private final AtomicBoolean loggedUnimplemented;
        private final ThrottlingLogger logger;
        private final String type;
        private final int numItems;
        private final CompletableResultCode result;

        public ClientRequestOnSuccessHandler(Map<String, String> headers,
                boolean compressionEnabled,
                ExporterMetrics exporterMetrics,
                TraceRequestMarshaler marshaler,
                AtomicBoolean loggedUnimplemented,
                ThrottlingLogger logger,
                String type,
                int numItems,
                CompletableResultCode result) {
            this.headers = headers;
            this.compressionEnabled = compressionEnabled;
            this.exporterMetrics = exporterMetrics;
            this.marshaler = marshaler;
            this.loggedUnimplemented = loggedUnimplemented;
            this.logger = logger;
            this.type = type;
            this.numItems = numItems;
            this.result = result;
        }

        @Override
        public void handle(GrpcClientRequest<Buffer, Buffer> request) {
            if (compressionEnabled) {
                request.encoding("gzip");
            }

            // Set the service name and the method to call
            request.serviceName(ServiceName.create(GRPC_SERVICE_NAME));
            request.methodName(GRPC_METHOD_NAME);

            if (!headers.isEmpty()) {
                var vertxHeaders = request.headers();
                for (var entry : headers.entrySet()) {
                    vertxHeaders.set(entry.getKey(), entry.getValue());
                }
            }

            try {
                int messageSize = marshaler.getBinarySerializedSize();
                Buffer buffer = Buffer.buffer(messageSize);
                var os = new BufferOutputStream(buffer);
                marshaler.writeBinaryTo(os);
                request.send(buffer).onSuccess(new Handler<>() {
                    @Override
                    public void handle(GrpcClientResponse<Buffer, Buffer> response) {
                        GrpcStatus status = getStatus(response);
                        if (status == GrpcStatus.OK) {
                            exporterMetrics.addSuccess(numItems);
                            result.succeed();
                            return;
                        }
                        String statusMessage = getStatusMessage(response);
                        if (statusMessage == null) {
                            // TODO: this needs investigation, when this happened, the spans actually got to the server, but for some reason no status code was present in the result
                            exporterMetrics.addSuccess(numItems);
                            result.succeed();
                            return;
                        }

                        logAppropriateWarning(status, statusMessage);
                        exporterMetrics.addFailed(numItems);
                        result.fail();
                    }

                    private void logAppropriateWarning(GrpcStatus status,
                            String statusMessage) {
                        if (status == GrpcStatus.UNIMPLEMENTED) {
                            if (loggedUnimplemented.compareAndSet(false, true)) {
                                logUnimplemented(internalLogger, type, statusMessage);
                            }
                        } else if (status == GrpcStatus.UNAVAILABLE) {
                            logger.log(
                                    Level.SEVERE,
                                    "Failed to export "
                                            + type
                                            + "s. Server is UNAVAILABLE. "
                                            + "Make sure your collector is running and reachable from this network. "
                                            + "Full error message:"
                                            + statusMessage);
                        } else {
                            if (status == null) {
                                logger.log(
                                        Level.WARNING,
                                        "Failed to export "
                                                + type
                                                + "s. Server responded with error message: "
                                                + statusMessage);
                            } else {
                                logger.log(
                                        Level.WARNING,
                                        "Failed to export "
                                                + type
                                                + "s. Server responded with "
                                                + status.code
                                                + ". Error message: "
                                                + statusMessage);
                            }
                        }
                    }

                    private void logUnimplemented(Logger logger, String type, String fullErrorMessage) {
                        String envVar;
                        switch (type) {
                            case "span":
                                envVar = "OTEL_TRACES_EXPORTER";
                                break;
                            case "metric":
                                envVar = "OTEL_METRICS_EXPORTER";
                                break;
                            case "log":
                                envVar = "OTEL_LOGS_EXPORTER";
                                break;
                            default:
                                throw new IllegalStateException(
                                        "Unrecognized type, this is a programming bug in the OpenTelemetry SDK");
                        }

                        logger.log(
                                Level.SEVERE,
                                "Failed to export "
                                        + type
                                        + "s. Server responded with UNIMPLEMENTED. "
                                        + "This usually means that your collector is not configured with an otlp "
                                        + "receiver in the \"pipelines\" section of the configuration. "
                                        + "If export is not desired and you are using OpenTelemetry autoconfiguration or the javaagent, "
                                        + "disable export by setting "
                                        + envVar
                                        + "=none. "
                                        + "Full error message: "
                                        + fullErrorMessage);
                    }

                    private GrpcStatus getStatus(GrpcClientResponse<?, ?> response) {
                        // Status can either be in the headers or trailers depending on error
                        GrpcStatus result = response.status();
                        if (result == null) {
                            String statusFromTrailer = response.trailers().get(GRPC_STATUS);
                            if (statusFromTrailer != null) {
                                result = GrpcStatus.valueOf(Integer.parseInt(statusFromTrailer));
                            }
                        }
                        return result;
                    }

                    private String getStatusMessage(GrpcClientResponse<Buffer, Buffer> response) {
                        // Status message can either be in the headers or trailers depending on error
                        String result = response.statusMessage();
                        if (result == null) {
                            result = response.trailers().get(GRPC_MESSAGE);
                            if (result != null) {
                                result = QueryStringDecoder.decodeComponent(result, StandardCharsets.UTF_8);
                            }

                        }
                        return result;
                    }

                }).onFailure(new Handler<>() {
                    @Override
                    public void handle(Throwable t) {
                        exporterMetrics.addFailed(numItems);
                        logger.log(
                                Level.SEVERE,
                                "Failed to export "
                                        + type
                                        + "s. The request could not be executed. Full error message: "
                                        + t.getMessage());
                        result.fail();
                    }
                });
            } catch (IOException e) {
                exporterMetrics.addFailed(numItems);
                logger.log(
                        Level.SEVERE,
                        "Failed to export "
                                + type
                                + "s. Unable to serialize payload. Full error message: "
                                + e.getMessage());
                result.fail();
            }
        }
    }
}
