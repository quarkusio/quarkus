package io.quarkus.opentelemetry.deployment.exporter.otlp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URI;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import jakarta.inject.Inject;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.opentelemetry.sdk.common.export.GrpcResponse;
import io.opentelemetry.sdk.common.export.GrpcStatusCode;
import io.opentelemetry.sdk.common.export.HttpResponse;
import io.quarkus.opentelemetry.runtime.exporter.otlp.sender.VertxGrpcSender;
import io.quarkus.opentelemetry.runtime.exporter.otlp.sender.VertxHttpSender;
import io.quarkus.test.QuarkusExtensionTest;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;

/**
 * An export that starts on an idle connection must be sent once, even when the response is still pending at the
 * moment the connection has been idle for the exporter timeout.
 * <p>
 * The first export opens the connection. The second one starts 900 ms after its response, with a 1 s timeout, and
 * the collector takes 300 ms to answer it. A read idle timeout on the connection fires 1 s after the last read,
 * while the second request is in flight, closes the connection and makes the sender retry a request the collector
 * already received.
 */
public class SenderIdleConnectionTest {

    private static final Duration TIMEOUT = Duration.ofSeconds(1);

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class).addClass(EmptyMessageWriter.class));

    @Inject
    Vertx vertx;

    private HttpServer collector;
    private VertxGrpcSender grpcSender;
    private VertxHttpSender httpSender;

    @AfterEach
    void cleanup() throws Exception {
        if (grpcSender != null) {
            grpcSender.shutdown();
        }
        if (httpSender != null) {
            httpSender.shutdown();
        }
        if (collector != null) {
            collector.close().toCompletionStage().toCompletableFuture().get(10, TimeUnit.SECONDS);
        }
    }

    @Test
    public void grpcExportPendingWhenTheConnectionGoesIdleIsSentOnce() throws Exception {
        AtomicInteger requests = new AtomicInteger();
        int port = startCollector(requests, this::grpcOk);
        grpcSender = new VertxGrpcSender(URI.create("http://localhost:" + port), VertxGrpcSender.GRPC_TRACE_SERVICE_NAME,
                false, TIMEOUT, Map.of(), options -> {
                }, vertx);

        exportGrpc();
        Thread.sleep(900);
        GrpcResponse response = exportGrpc();

        assertThat(response.getStatusCode()).isEqualTo(GrpcStatusCode.OK);
        assertThat(requests).hasValue(2);
    }

    @Test
    public void httpExportPendingWhenTheConnectionGoesIdleIsSentOnce() throws Exception {
        AtomicInteger requests = new AtomicInteger();
        int port = startCollector(requests, response -> response.setStatusCode(200).end());
        httpSender = new VertxHttpSender(URI.create("http://localhost:" + port), "/v1/traces", false, TIMEOUT, Map.of(),
                "application/x-protobuf", options -> {
                }, vertx);

        exportHttp();
        Thread.sleep(900);
        HttpResponse response = exportHttp();

        assertThat(response.getStatusCode()).isEqualTo(200);
        assertThat(requests).hasValue(2);
    }

    @Test
    public void grpcExportWithoutResponseFailsAfterTheTimeout() throws Exception {
        int port = listen(request -> {
        });
        grpcSender = new VertxGrpcSender(URI.create("http://localhost:" + port), VertxGrpcSender.GRPC_TRACE_SERVICE_NAME,
                false, TIMEOUT, Map.of(), options -> {
                }, vertx);

        assertThatThrownBy(this::exportGrpc).isInstanceOf(ExecutionException.class);
    }

    @Test
    public void httpExportWithoutResponseFailsAfterTheTimeout() throws Exception {
        int port = listen(request -> {
        });
        httpSender = new VertxHttpSender(URI.create("http://localhost:" + port), "/v1/traces", false, TIMEOUT, Map.of(),
                "application/x-protobuf", options -> {
                }, vertx);

        assertThatThrownBy(this::exportHttp).isInstanceOf(ExecutionException.class);
    }

    private int startCollector(AtomicInteger requests, Consumer<HttpServerResponse> answer) throws Exception {
        return listen(request -> {
            long delay = requests.incrementAndGet() == 1 ? 1 : 300;
            request.endHandler(v -> vertx.setTimer(delay, id -> answer.accept(request.response())));
        });
    }

    private int listen(Handler<HttpServerRequest> handler) throws Exception {
        collector = vertx.createHttpServer().requestHandler(handler);
        return collector.listen(0, "localhost").toCompletionStage().toCompletableFuture()
                .get(10, TimeUnit.SECONDS).actualPort();
    }

    private void grpcOk(HttpServerResponse response) {
        response.putHeader("content-type", "application/grpc");
        // one empty message: no compression flag, zero length
        response.write(Buffer.buffer(new byte[5]));
        response.putTrailer("grpc-status", "0");
        response.end();
    }

    private GrpcResponse exportGrpc() throws Exception {
        CompletableFuture<GrpcResponse> result = new CompletableFuture<>();
        grpcSender.send(new EmptyMessageWriter(), result::complete, result::completeExceptionally);
        return result.get(10, TimeUnit.SECONDS);
    }

    private HttpResponse exportHttp() throws Exception {
        CompletableFuture<HttpResponse> result = new CompletableFuture<>();
        httpSender.send(new EmptyMessageWriter(), result::complete, result::completeExceptionally);
        return result.get(10, TimeUnit.SECONDS);
    }
}
