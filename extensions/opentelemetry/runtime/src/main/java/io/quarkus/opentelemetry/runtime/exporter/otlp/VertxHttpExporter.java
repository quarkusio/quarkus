package io.quarkus.opentelemetry.runtime.exporter.otlp;

import static io.quarkus.opentelemetry.runtime.exporter.otlp.OTelExporterUtil.getPort;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.time.Duration;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.GZIPOutputStream;

import io.opentelemetry.exporter.internal.http.HttpExporter;
import io.opentelemetry.exporter.internal.http.HttpSender;
import io.opentelemetry.exporter.internal.otlp.traces.TraceRequestMarshaler;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.internal.ThrottlingLogger;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import io.quarkus.vertx.core.runtime.BufferOutputStream;
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

final class VertxHttpExporter implements SpanExporter {

    private static final Logger internalLogger = Logger.getLogger(VertxHttpExporter.class.getName());
    private static final ThrottlingLogger logger = new ThrottlingLogger(internalLogger);

    private final HttpExporter<TraceRequestMarshaler> delegate;

    VertxHttpExporter(HttpExporter<TraceRequestMarshaler> delegate) {
        this.delegate = delegate;
    }

    @Override
    public CompletableResultCode export(Collection<SpanData> spans) {
        TraceRequestMarshaler exportRequest = TraceRequestMarshaler.create(spans);
        return delegate.export(exportRequest, spans.size());
    }

    @Override
    public CompletableResultCode flush() {
        return CompletableResultCode.ofSuccess();
    }

    @Override
    public CompletableResultCode shutdown() {
        return delegate.shutdown();
    }

    static final class VertxHttpSender implements HttpSender {

        private static final String TRACES_PATH = "/v1/traces";
        private final String basePath;
        private final boolean compressionEnabled;
        private final Map<String, String> headers;
        private final String contentType;
        private final HttpClient client;

        VertxHttpSender(
                URI baseUri,
                boolean compressionEnabled,
                Duration timeout,
                Map<String, String> headersMap,
                String contentType,
                Consumer<HttpClientOptions> clientOptionsCustomizer,
                Vertx vertx) {
            this.basePath = determineBasePath(baseUri);
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
        public void send(Consumer<OutputStream> marshaler,
                int contentLength,
                Consumer<Response> onResponse,
                Consumer<Throwable> onError) {

            client.request(HttpMethod.POST, basePath + TRACES_PATH)
                    .onSuccess(new Handler<>() {
                        @Override
                        public void handle(HttpClientRequest request) {

                            HttpClientRequest clientRequest = request.response(new Handler<>() {
                                @Override
                                public void handle(AsyncResult<HttpClientResponse> callResult) {
                                    if (callResult.succeeded()) {
                                        HttpClientResponse clientResponse = callResult.result();
                                        clientResponse.body(new Handler<>() {
                                            @Override
                                            public void handle(AsyncResult<Buffer> bodyResult) {
                                                if (bodyResult.succeeded()) {
                                                    onResponse.accept(new Response() {
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
                                                    onError.accept(bodyResult.cause());
                                                }
                                            }
                                        });
                                    } else {
                                        onError.accept(callResult.cause());
                                    }
                                }
                            })
                                    .putHeader("Content-Type", contentType);

                            Buffer buffer = Buffer.buffer(contentLength);
                            OutputStream os = new BufferOutputStream(buffer);
                            if (compressionEnabled) {
                                clientRequest.putHeader("Content-Encoding", "gzip");
                                try (var gzos = new GZIPOutputStream(os)) {
                                    marshaler.accept(gzos);
                                } catch (IOException e) {
                                    throw new IllegalStateException(e);
                                }
                            } else {
                                marshaler.accept(os);
                            }

                            if (!headers.isEmpty()) {
                                for (var entry : headers.entrySet()) {
                                    clientRequest.putHeader(entry.getKey(), entry.getValue());
                                }
                            }

                            clientRequest.send(buffer);

                        }
                    })
                    .onFailure(onError::accept);
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
    }
}
