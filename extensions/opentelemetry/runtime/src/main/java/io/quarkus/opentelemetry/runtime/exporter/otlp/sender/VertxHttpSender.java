package io.quarkus.opentelemetry.runtime.exporter.otlp.sender;

import static io.quarkus.opentelemetry.runtime.exporter.otlp.OTelExporterUtil.getPort;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
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
import java.util.zip.GZIPOutputStream;

import io.opentelemetry.exporter.internal.http.HttpSender;
import io.opentelemetry.exporter.internal.marshal.Marshaler;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.internal.ThrottlingLogger;
import io.quarkus.vertx.core.runtime.BufferOutputStream;
import io.smallrye.common.annotation.SuppressForbidden;
import io.smallrye.mutiny.Uni;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.tracing.TracingPolicy;

public final class VertxHttpSender implements HttpSender {

    public static final String TRACES_PATH = "/v1/traces";
    public static final String METRICS_PATH = "/v1/metrics";
    public static final String LOGS_PATH = "/v1/logs";

    private static final Logger internalLogger = Logger.getLogger(VertxHttpSender.class.getName());
    private static final ThrottlingLogger logger = new ThrottlingLogger(internalLogger);

    private static final int MAX_ATTEMPTS = 3;

    private final String basePath;
    private final boolean compressionEnabled;
    private final Map<String, String> headers;
    private final String contentType;
    private final HttpClient client;
    private final String signalPath;

    public VertxHttpSender(
            URI baseUri,
            String signalPath,
            boolean compressionEnabled,
            Duration timeout,
            Map<String, String> headersMap,
            String contentType,
            Consumer<HttpClientOptions> clientOptionsCustomizer,
            Vertx vertx) {
        this.basePath = determineBasePath(baseUri);
        this.signalPath = signalPath;
        this.compressionEnabled = compressionEnabled;
        this.headers = headersMap;
        this.contentType = contentType;
        var httpClientOptions = new HttpClientOptions()
                .setReadIdleTimeout((int) timeout.getSeconds())
                .setDefaultHost(baseUri.getHost())
                .setDefaultPort(getPort(baseUri))
                .setTracingPolicy(TracingPolicy.IGNORE); // needed to avoid tracing the calls from this http client
        clientOptionsCustomizer.accept(httpClientOptions);
        this.client = vertx.createHttpClient(httpClientOptions);
    }

    private final AtomicBoolean isShutdown = new AtomicBoolean();
    private final CompletableResultCode shutdownResult = new CompletableResultCode();

    private static String determineBasePath(URI baseUri) {
        String path = baseUri.getPath();
        if (path.isEmpty() || path.equals("/")) {
            return "";
        }
        if (path.endsWith("/")) { // strip ending slash
            path = path.substring(0, path.length() - 1);
        }
        if (!path.startsWith("/")) { // prepend leading slash
            path = "/" + path;
        }
        return path;
    }

    @Override
    public void send(Marshaler marshaler,
            int contentLength,
            Consumer<Response> onHttpResponseRead,
            Consumer<Throwable> onError) {
        if (isShutdown.get()) {
            return;
        }

        String marshalerType = marshaler.getClass().getSimpleName();
        String requestURI = basePath + signalPath;
        var clientRequestSuccessHandler = new ClientRequestSuccessHandler(client, requestURI, headers, compressionEnabled,
                contentType,
                contentLength, onHttpResponseRead,
                onError, marshaler, 1, isShutdown::get);
        initiateSend(client, requestURI, MAX_ATTEMPTS, clientRequestSuccessHandler, new Consumer<>() {
            @Override
            public void accept(Throwable throwable) {
                failOnClientRequest(marshalerType, throwable, onError);
            }
        });
    }

    @SuppressForbidden(reason = "The use of ThrottlingLogger mandates the use of java.util.logging")
    private void failOnClientRequest(String type, Throwable t, Consumer<Throwable> onError) {
        String message = "Failed to export "
                + type
                + ". The request could not be executed. Full error message: "
                + (t.getMessage() == null ? t.getClass().getName() : t.getMessage());
        logger.log(Level.WARNING, message);
        onError.accept(t);
    }

    private static void initiateSend(HttpClient client, String requestURI,
            int numberOfAttempts,
            Handler<HttpClientRequest> clientRequestSuccessHandler,
            Consumer<Throwable> onFailureCallback) {
        Uni.createFrom().completionStage(new Supplier<CompletionStage<HttpClientRequest>>() {
            @Override
            public CompletionStage<HttpClientRequest> get() {
                return client.request(HttpMethod.POST, requestURI).toCompletionStage();
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
                .recoverWithUni(new Supplier<Uni<? extends HttpClientRequest>>() {
                    @Override
                    public Uni<? extends HttpClientRequest> get() {
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
                            public void accept(HttpClientRequest request) {
                                clientRequestSuccessHandler.handle(request);
                            }
                        }, onFailureCallback);
    }

    @Override
    @SuppressForbidden(reason = "The use of ThrottlingLogger mandates the use of java.util.logging")
    public CompletableResultCode shutdown() {
        if (!isShutdown.compareAndSet(false, true)) {
            logger.log(Level.FINE, "Calling shutdown() multiple times.");
            return shutdownResult;
        }

        try {
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
        } catch (RejectedExecutionException e) {
            internalLogger.log(Level.FINE, "Unable to complete shutdown", e);
            // if Netty's ThreadPool has been closed, this onSuccess() will immediately throw RejectedExecutionException
            // which we need to handle
            shutdownResult.fail();
        }
        return shutdownResult;
    }

    private static class ClientRequestSuccessHandler implements Handler<HttpClientRequest> {
        private final HttpClient client;
        private final String requestURI;
        private final Map<String, String> headers;
        private final boolean compressionEnabled;
        private final String contentType;
        private final int contentLength;
        private final Consumer<Response> onHttpResponseRead;
        private final Consumer<Throwable> onError;
        private final Marshaler marshaler;

        private final int attemptNumber;
        private final Supplier<Boolean> isShutdown;

        public ClientRequestSuccessHandler(HttpClient client,
                String requestURI, Map<String, String> headers,
                boolean compressionEnabled,
                String contentType,
                int contentLength,
                Consumer<Response> onHttpResponseRead,
                Consumer<Throwable> onError,
                Marshaler marshaler,
                int attemptNumber,
                Supplier<Boolean> isShutdown) {
            this.client = client;
            this.requestURI = requestURI;
            this.headers = headers;
            this.compressionEnabled = compressionEnabled;
            this.contentType = contentType;
            this.contentLength = contentLength;
            this.onHttpResponseRead = onHttpResponseRead;
            this.onError = onError;
            this.marshaler = marshaler;
            this.attemptNumber = attemptNumber;
            this.isShutdown = isShutdown;
        }

        @Override
        public void handle(HttpClientRequest request) {

            HttpClientRequest clientRequest = request.response(new Handler<>() {
                @Override
                public void handle(AsyncResult<HttpClientResponse> callResult) {
                    if (callResult.succeeded()) {
                        HttpClientResponse clientResponse = callResult.result();
                        Throwable cause = callResult.cause();
                        clientResponse.body(new Handler<>() {
                            @Override
                            public void handle(AsyncResult<Buffer> bodyResult) {
                                if (bodyResult.succeeded()) {
                                    if (clientResponse.statusCode() >= 500) {
                                        if (attemptNumber <= MAX_ATTEMPTS && !isShutdown.get()) {
                                            // we should retry for 5xx error as they might be recoverable
                                            initiateSend(client, requestURI,
                                                    MAX_ATTEMPTS - attemptNumber,
                                                    newAttempt(),
                                                    onError);
                                            return;
                                        }
                                    }
                                    onHttpResponseRead.accept(new Response() {
                                        @Override
                                        public int statusCode() {
                                            return clientResponse.statusCode();
                                        }

                                        @Override
                                        public String statusMessage() {
                                            return clientResponse.statusMessage();
                                        }

                                        @Override
                                        public byte[] responseBody() {
                                            return bodyResult.result().getBytes();
                                        }
                                    });
                                } else {
                                    if (attemptNumber <= MAX_ATTEMPTS && !isShutdown.get()) {
                                        // retry
                                        initiateSend(client, requestURI,
                                                MAX_ATTEMPTS - attemptNumber,
                                                newAttempt(),
                                                onError);
                                    } else {
                                        onError.accept(bodyResult.cause());
                                    }
                                }
                            }
                        });
                    } else {
                        if (attemptNumber <= MAX_ATTEMPTS && !isShutdown.get()) {
                            // retry
                            initiateSend(client, requestURI,
                                    MAX_ATTEMPTS - attemptNumber,
                                    newAttempt(),
                                    onError);
                        } else {
                            onError.accept(callResult.cause());
                        }
                    }
                }
            })
                    .putHeader("Content-Type", contentType);

            Buffer buffer = Buffer.buffer(contentLength);
            OutputStream os = new BufferOutputStream(buffer);
            if (compressionEnabled) {
                clientRequest.putHeader("Content-Encoding", "gzip");
                try (var gzos = new GZIPOutputStream(os)) {
                    marshaler.writeBinaryTo(gzos);
                } catch (IOException e) {
                    throw new IllegalStateException(e);
                }
            } else {
                try {
                    marshaler.writeBinaryTo(os);
                } catch (IOException e) {
                    throw new IllegalStateException(e);
                }
            }

            if (!headers.isEmpty()) {
                for (var entry : headers.entrySet()) {
                    clientRequest.putHeader(entry.getKey(), entry.getValue());
                }
            }

            clientRequest.send(buffer);
        }

        public ClientRequestSuccessHandler newAttempt() {
            return new ClientRequestSuccessHandler(client, requestURI, headers, compressionEnabled,
                    contentType, contentLength, onHttpResponseRead,
                    onError, marshaler, attemptNumber + 1, isShutdown);
        }
    }
}
