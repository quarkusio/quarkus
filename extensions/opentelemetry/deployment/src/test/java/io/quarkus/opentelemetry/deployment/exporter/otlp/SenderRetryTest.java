package io.quarkus.opentelemetry.deployment.exporter.otlp;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.OutputStream;
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
import io.opentelemetry.sdk.common.export.MessageWriter;
import io.quarkus.opentelemetry.runtime.exporter.otlp.sender.VertxHttpSender;
import io.quarkus.test.QuarkusExtensionTest;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.core.net.NetClient;
import io.vertx.core.net.NetServer;

/**
 * Failures that are not caused by the sender being shut down must still be retried, and an export whose
 * retry succeeds must be reported through the response callback.
 */
public class SenderRetryTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class));

    @Inject
    Vertx vertx;

    private HttpServer collector;
    private NetServer front;
    private NetClient frontClient;
    private VertxHttpSender sender;

    @AfterEach
    void cleanup() throws Exception {
        if (sender != null) {
            sender.shutdown();
        }
        if (front != null) {
            front.close().toCompletionStage().toCompletableFuture().get(10, TimeUnit.SECONDS);
        }
        if (frontClient != null) {
            frontClient.close().toCompletionStage().toCompletableFuture().get(10, TimeUnit.SECONDS);
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
            requests.incrementAndGet();
            request.response().setStatusCode(200).end();
        });
        int collectorPort = collector.listen(0, "localhost").toCompletionStage().toCompletableFuture()
                .get(10, TimeUnit.SECONDS).actualPort();

        // Closes the first connection before any byte is exchanged, proxies every later one to the collector
        AtomicInteger connections = new AtomicInteger();
        frontClient = vertx.createNetClient();
        front = vertx.createNetServer().connectHandler(socket -> {
            if (connections.incrementAndGet() == 1) {
                socket.close();
                return;
            }
            socket.pause();
            frontClient.connect(collectorPort, "localhost").onSuccess(upstream -> {
                socket.pipeTo(upstream);
                upstream.pipeTo(socket);
            }).onFailure(t -> socket.close());
        });
        int frontPort = front.listen(0, "localhost").toCompletionStage().toCompletableFuture()
                .get(10, TimeUnit.SECONDS).actualPort();

        HttpResponse response = export(frontPort);

        assertThat(response.getStatusCode()).isEqualTo(200);
        assertThat(connections).hasValue(2);
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

    private static final class EmptyMessageWriter implements MessageWriter {

        @Override
        public void writeMessage(OutputStream output) {
        }

        @Override
        public int getContentLength() {
            return 0;
        }
    }
}
