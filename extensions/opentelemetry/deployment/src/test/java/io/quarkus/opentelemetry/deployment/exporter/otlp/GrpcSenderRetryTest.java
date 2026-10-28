package io.quarkus.opentelemetry.deployment.exporter.otlp;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.inject.Inject;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.opentelemetry.sdk.common.export.GrpcResponse;
import io.opentelemetry.sdk.common.export.GrpcStatusCode;
import io.quarkus.opentelemetry.runtime.exporter.otlp.sender.VertxGrpcSender;
import io.quarkus.test.QuarkusExtensionTest;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServer;
import io.vertx.grpc.common.GrpcMessage;
import io.vertx.grpc.server.GrpcServer;

/**
 * Failures that are not caused by the sender being shut down must still be retried, and an export whose
 * retry succeeds must be reported through the response callback - the gRPC counterpart of
 * {@link SenderRetryTest}.
 */
public class GrpcSenderRetryTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class).addClass(EmptyMessageWriter.class));

    @Inject
    Vertx vertx;

    private HttpServer collector;
    private VertxGrpcSender sender;

    @AfterEach
    void cleanup() throws Exception {
        if (sender != null) {
            sender.shutdown();
        }
        if (collector != null) {
            collector.close().toCompletionStage().toCompletableFuture().get(10, TimeUnit.SECONDS);
        }
    }

    @Test
    public void respondsQuicklyOnTheHappyPath() throws Exception {
        AtomicInteger requests = new AtomicInteger();
        collector = vertx.createHttpServer().requestHandler(GrpcServer.server(vertx).callHandler(request -> {
            requests.incrementAndGet();
            request.response().endMessage(GrpcMessage.message("identity", Buffer.buffer()));
        }));
        int port = collector.listen(0, "localhost").toCompletionStage().toCompletableFuture()
                .get(10, TimeUnit.SECONDS).actualPort();

        GrpcResponse response = export(port, Duration.ofSeconds(5));

        assertThat(response.getStatusCode()).isEqualTo(GrpcStatusCode.OK);
        assertThat(requests).hasValue(1);
    }

    /**
     * A connection that keeps producing traffic unrelated to the export (here, an HTTP/2 PING on the
     * connection itself, exactly what a load balancer/sidecar proxy/NAT keep-alive would do) never trips
     * {@code HttpClientOptions#setReadIdleTimeout}, so the stalled Export stream needs its own deadline
     * independent of connection-level read activity, or the export hangs forever. See
     * <a href="https://github.com/quarkusio/quarkus/issues/57904">#57904</a>.
     */
    @Test
    public void retriesWhenConnectionStaysBusyWithoutARealResponse() throws Exception {
        AtomicInteger requests = new AtomicInteger();
        collector = vertx.createHttpServer().requestHandler(GrpcServer.server(vertx).callHandler(request -> {
            if (requests.incrementAndGet() == 1) {
                long pingTimer = vertx.setPeriodic(50, id -> request.connection().ping(Buffer.buffer("keepalive")));
                request.connection().closeHandler(v -> vertx.cancelTimer(pingTimer));
                // Never calls request.response().endMessage(...): the Export stream stalls forever.
            } else {
                request.response().endMessage(GrpcMessage.message("identity", Buffer.buffer()));
            }
        }));
        int port = collector.listen(0, "localhost").toCompletionStage().toCompletableFuture()
                .get(10, TimeUnit.SECONDS).actualPort();

        GrpcResponse response = export(port, Duration.ofMillis(500));

        assertThat(response.getStatusCode()).isEqualTo(GrpcStatusCode.OK);
        assertThat(requests).hasValue(2);
    }

    private GrpcResponse export(int port, Duration timeout) throws Exception {
        sender = new VertxGrpcSender(URI.create("http://localhost:" + port), VertxGrpcSender.GRPC_TRACE_SERVICE_NAME,
                false, timeout, Map.of(), options -> {
                }, vertx);
        CompletableFuture<GrpcResponse> result = new CompletableFuture<>();
        sender.send(new EmptyMessageWriter(), result::complete, result::completeExceptionally);
        return result.get(10, TimeUnit.SECONDS);
    }
}
