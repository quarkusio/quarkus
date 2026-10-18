package io.quarkus.vertx.http.deployment.devmode;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.Closeable;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;

import io.quarkus.dev.spi.RemoteDevState;
import io.quarkus.vertx.http.runtime.devmode.RemoteSyncHandler;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;

class HttpRemoteDevClientTest {

    @Test
    void bodyRequestsUseFixedLengthAndResponsePolicyDistinguishesRetryableFromTerminal() throws Exception {
        Vertx vertx = Vertx.vertx();
        AtomicInteger devRequests = new AtomicInteger();
        AtomicLong connectLength = new AtomicLong(-1);
        AtomicLong devLength = new AtomicLong(-1);
        AtomicReference<String> connectTransferEncoding = new AtomicReference<>();
        AtomicReference<String> devTransferEncoding = new AtomicReference<>();
        HttpServer server = await(vertx.createHttpServer().requestHandler(request -> request.bodyHandler(ignored -> {
            if (request.path().endsWith(RemoteSyncHandler.CONNECT)) {
                connectLength.set(parseContentLength(request.getHeader("Content-Length")));
                connectTransferEncoding.set(request.getHeader("Transfer-Encoding"));
                request.response().putHeader(RemoteSyncHandler.QUARKUS_SESSION, "session").end();
            } else if (request.path().endsWith(RemoteSyncHandler.DEV)) {
                int devRequest = devRequests.incrementAndGet();
                devLength.set(parseContentLength(request.getHeader("Content-Length")));
                devTransferEncoding.set(request.getHeader("Transfer-Encoding"));
                if (devRequest == 1) {
                    request.response()
                            .putHeader(RemoteSyncHandler.QUARKUS_ERROR,
                                    "Remote dev request-body capacity is temporarily unavailable")
                            .putHeader("Retry-After", "0")
                            .setStatusCode(503)
                            .end();
                } else {
                    request.response()
                            .putHeader(RemoteSyncHandler.QUARKUS_ERROR,
                                    "Remote dev request body exceeds quarkus.http.limits.max-body-size")
                            .setStatusCode(413)
                            .end();
                }
            } else {
                request.response().setStatusCode(404).end();
            }
        })).listen(0));

        Closeable session = null;
        try {
            var client = new HttpRemoteDevClient(
                    "http://localhost:" + server.actualPort(),
                    "secret", Duration.ofSeconds(1), Duration.ofMillis(10), 5);
            session = client.sendConnectRequest(new RemoteDevState(Map.of(), null), ignored -> Map.of(), () -> null);

            Awaitility.await().atMost(Duration.ofSeconds(5))
                    .untilAsserted(() -> assertThat(devRequests).hasValue(2));

            assertThat(connectLength).hasValueGreaterThan(0);
            assertThat(devLength).hasValueGreaterThan(0);
            assertThat(connectTransferEncoding).hasValue(null);
            assertThat(devTransferEncoding).hasValue(null);
            assertThat(devRequests).hasValue(2);
        } finally {
            if (session != null) {
                session.close();
            }
            await(server.close());
            await(vertx.close());
        }
    }

    private static <T> T await(Future<T> future) throws Exception {
        return future.toCompletionStage().toCompletableFuture().get(10, TimeUnit.SECONDS);
    }

    private static long parseContentLength(String value) {
        return value == null ? -1 : Long.parseLong(value);
    }
}
