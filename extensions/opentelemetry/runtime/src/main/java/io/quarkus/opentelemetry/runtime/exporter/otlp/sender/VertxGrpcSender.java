package io.quarkus.opentelemetry.runtime.exporter.otlp.sender;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

import io.netty.handler.codec.http.QueryStringDecoder;
import io.opentelemetry.exporter.internal.grpc.GrpcResponse;
import io.opentelemetry.exporter.internal.grpc.GrpcSender;
import io.opentelemetry.exporter.internal.marshal.Marshaler;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.internal.ThrottlingLogger;
import io.quarkus.opentelemetry.runtime.exporter.otlp.OTelExporterUtil;
import io.quarkus.vertx.core.runtime.BufferOutputStream;
import io.smallrye.mutiny.Uni;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.net.SocketAddress;
import io.vertx.core.tracing.TracingPolicy;
import io.vertx.grpc.client.GrpcClient;
import io.vertx.grpc.client.GrpcClientRequest;
import io.vertx.grpc.client.GrpcClientResponse;
import io.vertx.grpc.common.GrpcError;
import io.vertx.grpc.common.GrpcStatus;
import io.vertx.grpc.common.ServiceName;

public final class VertxGrpcSender implements GrpcSender {

    public static final String GRPC_TRACE_SERVICE_NAME = "opentelemetry.proto.collector.trace.v1.TraceService";
    public static final String GRPC_METRIC_SERVICE_NAME = "opentelemetry.proto.collector.metrics.v1.MetricsService";
    public static final String GRPC_LOG_SERVICE_NAME = "opentelemetry.proto.collector.logs.v1.LogsService";
    private static final String GRPC_METHOD_NAME = "Export";

    private static final String GRPC_STATUS = "grpc-status";
    private static final String GRPC_MESSAGE = "grpc-message";

    private static final Logger internalLogger = Logger.getLogger(VertxGrpcSender.class.getName());
    private static final int MAX_ATTEMPTS = 3;

    private final ThrottlingLogger logger = new ThrottlingLogger(internalLogger); // TODO: is there something in JBoss Logging we can use?

    // We only log unimplemented once since it's a configuration issue that won't be recovered.
    private final AtomicBoolean loggedUnimplemented = new AtomicBoolean();
    private final AtomicBoolean isShutdown = new AtomicBoolean();
    private final CompletableResultCode shutdownResult = new CompletableResultCode();
    private final SocketAddress server;
    private final boolean compressionEnabled;
    private final Map<String, String> headers;
    private final String grpcEndpointPath;

    private final GrpcClient client;

    public VertxGrpcSender(
            URI grpcBaseUri,
            String grpcEndpointPath,
            boolean compressionEnabled,
            Duration timeout,
            Map<String, String> headersMap,
            Consumer<HttpClientOptions> clientOptionsCustomizer,
            Vertx vertx) {
        this.grpcEndpointPath = grpcEndpointPath;
        this.server = SocketAddress.inetSocketAddress(OTelExporterUtil.getPort(grpcBaseUri), grpcBaseUri.getHost());
        this.compressionEnabled = compressionEnabled;
        this.headers = headersMap;
        var httpClientOptions = new HttpClientOptions()
                .setHttp2ClearTextUpgrade(false) // needed otherwise connections get closed immediately
                .setReadIdleTimeout((int) timeout.getSeconds())
                .setTracingPolicy(TracingPolicy.IGNORE); // needed to avoid tracing the calls from this gRPC client
        clientOptionsCustomizer.accept(httpClientOptions);
        this.client = GrpcClient.client(vertx, httpClientOptions);
    }

    @Override
    public void send(Marshaler request, Consumer onSuccess, Consumer onError) {
        if (isShutdown.get()) {
            return;
        }

        final String marshalerType = request.getClass().getSimpleName();
        var onSuccessHandler = new ClientRequestOnSuccessHandler(client, server, headers, compressionEnabled,
                request,
                loggedUnimplemented, logger, marshalerType, onSuccess, onError, 1, grpcEndpointPath,
                isShutdown::get);

        initiateSend(client, server, MAX_ATTEMPTS, onSuccessHandler, new Consumer<>() {
            @Override
            public void accept(Throwable throwable) {
                failOnClientRequest(marshalerType, throwable, onError);
            }
        });
    }

    @Override
    public CompletableResultCode shutdown() {
        if (!isShutdown.compareAndSet(false, true)) {
            logger.log(Level.FINE, "Calling shutdown() multiple times.");
            return shutdownResult;
        }

        client.close()
                .onSuccess(
                        new Handler<>() {
                            @Override
                            public void handle(Void event) {
                                shutdownResult.succeed();
                            }
                        })
                .onFailure(new Handler<>() {
                    @Override
                    public void handle(Throwable event) {
                        shutdownResult.fail();
                    }
                });
        return shutdownResult;
    }

    private static void initiateSend(GrpcClient client, SocketAddress server,
            int numberOfAttempts,
            Handler<GrpcClientRequest<Buffer, Buffer>> onSuccessHandler,
            Consumer<Throwable> onFailureCallback) {
        Uni.createFrom().completionStage(new Supplier<CompletionStage<GrpcClientRequest<Buffer, Buffer>>>() {
            @Override
            public CompletionStage<GrpcClientRequest<Buffer, Buffer>> get() {
                return client.request(server).toCompletionStage();
            }
        })
                .onFailure(new Predicate<Throwable>() {
                    @Override
                    public boolean test(Throwable t) {
                        // Will not retry on shutdown
                        return t instanceof IllegalStateException ||
                                t instanceof RejectedExecutionException;
                    }
                })
                .recoverWithUni(new Supplier<Uni<? extends GrpcClientRequest<Buffer, Buffer>>>() {
                    @Override
                    public Uni<? extends GrpcClientRequest<Buffer, Buffer>> get() {
                        return Uni.createFrom().nothing();
                    }
                })
                .onFailure()
                .retry()
                .withBackOff(Duration.ofMillis(100))
                .atMost(numberOfAttempts)
                .subscribe().with(
                        new Consumer<>() {
                            @Override
                            public void accept(GrpcClientRequest<Buffer, Buffer> request) {
                                onSuccessHandler.handle(request);
                            }
                        }, onFailureCallback);
    }

    private void failOnClientRequest(String type, Throwable t, Consumer<Throwable> onError) {
        String message = "Failed to export "
                + type
                + "s. The request could not be executed. Full error message: "
                + (t.getMessage() == null ? t.getClass().getName() : t.getMessage());
        logger.log(Level.WARNING, message);
        onError.accept(t);
    }

    private static final class ClientRequestOnSuccessHandler implements Handler<GrpcClientRequest<Buffer, Buffer>> {

        private final GrpcClient client;
        private final SocketAddress server;
        private final Map<String, String> headers;
        private final boolean compressionEnabled;

        private final Marshaler marshaler;
        private final AtomicBoolean loggedUnimplemented;
        private final ThrottlingLogger logger;
        private final String type;
        private final Consumer<GrpcResponse> onSuccess;
        private final Consumer<Throwable> onError;
        private final String grpcEndpointPath;

        private final int attemptNumber;
        private final Supplier<Boolean> isShutdown;

        public ClientRequestOnSuccessHandler(GrpcClient client,
                SocketAddress server,
                Map<String, String> headers,
                boolean compressionEnabled,
                Marshaler marshaler,
                AtomicBoolean loggedUnimplemented,
                ThrottlingLogger logger,
                String type,
                Consumer<GrpcResponse> onSuccess,
                Consumer<Throwable> onError,
                int attemptNumber,
                String grpcEndpointPath,
                Supplier<Boolean> isShutdown) {
            this.client = client;
            this.server = server;
            this.grpcEndpointPath = grpcEndpointPath;
            this.headers = headers;
            this.compressionEnabled = compressionEnabled;
            this.marshaler = marshaler;
            this.loggedUnimplemented = loggedUnimplemented;
            this.logger = logger;
            this.type = type;
            this.onSuccess = onSuccess;
            this.onError = onError;
            this.attemptNumber = attemptNumber;
            this.isShutdown = isShutdown;
        }

        @Override
        public void handle(GrpcClientRequest<Buffer, Buffer> request) {
            if (compressionEnabled) {
                request.encoding("gzip");
            }

            // Set the service name and the method to call
            request.serviceName(ServiceName.create(grpcEndpointPath));
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
                        response.exceptionHandler(new Handler<>() {
                            @Override
                            public void handle(Throwable t) {
                                if (attemptNumber <= MAX_ATTEMPTS && !isShutdown.get()) {
                                    // retry
                                    initiateSend(client, server,
                                            MAX_ATTEMPTS - attemptNumber,
                                            newAttempt(),
                                            new Consumer<>() {
                                                @Override
                                                public void accept(Throwable throwable) {
                                                    failOnClientRequest(throwable, onError, attemptNumber);
                                                }
                                            });

                                } else {
                                    failOnClientRequest(t, onError, attemptNumber);
                                }
                            }
                        }).errorHandler(new Handler<>() {
                            @Override
                            public void handle(GrpcError error) {
                                handleError(error.status, response);
                            }
                        }).endHandler(new Handler<>() {
                            @Override
                            public void handle(Void ignored) {
                                GrpcStatus status = getStatus(response);
                                if (status == GrpcStatus.OK) {
                                    onSuccess.accept(GrpcResponse.create(status.code, status.toString()));
                                } else {
                                    handleError(status, response);
                                }
                            }
                        });
                    }

                    private void handleError(GrpcStatus status, GrpcClientResponse<Buffer, Buffer> response) {
                        String statusMessage = getStatusMessage(response);
                        logAppropriateWarning(status, statusMessage);
                        onError.accept(new IllegalStateException(statusMessage));
                    }

                    private void logAppropriateWarning(GrpcStatus status,
                            String statusMessage) {
                        if (status == GrpcStatus.UNIMPLEMENTED) {
                            if (loggedUnimplemented.compareAndSet(false, true)) {
                                logUnimplemented(internalLogger, type, statusMessage);
                            }
                        } else if (status == GrpcStatus.UNAVAILABLE) {
                            logger.log(
                                    Level.WARNING,
                                    "Failed to export "
                                            + type
                                            + "s. Server is UNAVAILABLE. "
                                            + "Make sure your collector is running and reachable from this network. "
                                            + "Full error message:"
                                            + statusMessage);
                        } else {
                            if (status == null) {
                                if (statusMessage == null) {
                                    logger.log(
                                            Level.WARNING,
                                            "Failed to export "
                                                    + type
                                                    + "s. Perhaps the collector does not support collecting traces using grpc? Try configuring 'quarkus.otel.exporter.otlp.traces.protocol=http/protobuf'");
                                } else {
                                    logger.log(
                                            Level.WARNING,
                                            "Failed to export "
                                                    + type
                                                    + "s. Server responded with error message: "
                                                    + statusMessage);
                                }
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
                                Level.WARNING,
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
                        if (attemptNumber <= MAX_ATTEMPTS && !isShutdown.get()) {
                            // retry
                            initiateSend(client, server,
                                    MAX_ATTEMPTS - attemptNumber,
                                    newAttempt(),
                                    new Consumer<>() {
                                        @Override
                                        public void accept(Throwable throwable) {
                                            failOnClientRequest(throwable, onError, attemptNumber);
                                        }
                                    });
                        } else {
                            failOnClientRequest(t, onError, attemptNumber);
                        }
                    }
                });
            } catch (IOException e) {
                final String message = "Failed to export "
                        + type
                        + "s. Unable to serialize payload. Full error message: "
                        + (e.getMessage() == null ? e.getClass().getName() : e.getMessage());
                logger.log(Level.WARNING, message);
                onError.accept(e);
            }
        }

        private void failOnClientRequest(Throwable t, Consumer<Throwable> onError, int attemptNumber) {
            final String message = "Failed to export "
                    + type
                    + "s. The request could not be executed after " + attemptNumber
                    + " attempts. Full error message: "
                    + (t != null ? t.getMessage() : "");
            logger.log(Level.WARNING, message);
            onError.accept(t);
        }

        public ClientRequestOnSuccessHandler newAttempt() {
            return new ClientRequestOnSuccessHandler(client, server, headers, compressionEnabled, marshaler,
                    loggedUnimplemented, logger, type, onSuccess, onError, attemptNumber + 1,
                    grpcEndpointPath, isShutdown);
        }
    }
}
