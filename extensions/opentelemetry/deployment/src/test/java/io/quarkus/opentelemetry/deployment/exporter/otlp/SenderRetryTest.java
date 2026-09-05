package io.quarkus.opentelemetry.deployment.exporter.otlp;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.InetAddress;
import java.net.ServerSocket;
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

import io.opentelemetry.sdk.common.export.HttpResponse;
import io.quarkus.opentelemetry.runtime.exporter.otlp.sender.VertxHttpSender;
import io.quarkus.test.QuarkusExtensionTest;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;

/**
 * Failures that are not caused by the sender being shut down must still be retried, and an export whose
 * retry succeeds must be reported through the response callback.
 */
public class SenderRetryTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class).addClass(EmptyMessageWriter.class));

    @Inject
    Vertx vertx;

    private HttpServer collector;
    private VertxHttpSender sender;

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
    public void retriesAfterServerError() throws Exception {
        AtomicInteger requests = new AtomicInteger();
        collector = vertx.createHttpServer().requestHandler(request -> {
            if (requests.incrementAndGet() == 1) {
                request.response().setStatusCode(503).end();
            } else {
                request.response().setStatusCode(200).end();
            }
        });
        int port = collector.listen(0, "localhost").toCompletionStage().toCompletableFuture()
                .get(10, TimeUnit.SECONDS).actualPort();

        HttpResponse response = export(port);

        assertThat(response.getStatusCode()).isEqualTo(200);
        assertThat(requests).hasValue(2);
    }

    @Test
    public void retriesAfterDroppedConnection() throws Exception {
        AtomicInteger requests = new AtomicInteger();
        collector = vertx.createHttpServer().requestHandler(request -> {
            if (requests.incrementAndGet() == 1) {
                request.connection().close();
            } else {
                request.response().setStatusCode(200).end();
            }
        });
        int port = collector.listen(0, "localhost").toCompletionStage().toCompletableFuture()
                .get(10, TimeUnit.SECONDS).actualPort();

        HttpResponse response = export(port);

        assertThat(response.getStatusCode()).isEqualTo(200);
        assertThat(requests).hasValue(2);
    }

    @Test
    public void retriesWhenTheConnectionIsRefused() throws Exception {
        // Nothing listens on the port when the export starts, the collector only appears during the back-off
        int port;
        try (ServerSocket socket = new ServerSocket(0, 1, InetAddress.getLoopbackAddress())) {
            port = socket.getLocalPort();
        }
        AtomicInteger requests = new AtomicInteger();
        collector = vertx.createHttpServer().requestHandler(request -> {
            requests.incrementAndGet();
            request.response().setStatusCode(200).end();
        });
        vertx.setTimer(150, id -> collector.listen(port, "localhost"));

        HttpResponse response = export(port);

        assertThat(response.getStatusCode()).isEqualTo(200);
        assertThat(requests).hasValue(1);
    }

    private HttpResponse export(int port) throws Exception {
        sender = new VertxHttpSender(URI.create("http://localhost:" + port), "/v1/traces", false,
                Duration.ofSeconds(5), Map.of(), "application/x-protobuf", options -> {
                }, vertx);
        CompletableFuture<HttpResponse> result = new CompletableFuture<>();
        sender.send(new EmptyMessageWriter(), result::complete, result::completeExceptionally);
        return result.get(10, TimeUnit.SECONDS);
    }
}
