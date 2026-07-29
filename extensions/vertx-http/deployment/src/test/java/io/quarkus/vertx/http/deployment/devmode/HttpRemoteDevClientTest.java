package io.quarkus.vertx.http.deployment.devmode;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.Closeable;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import com.sun.net.httpserver.HttpServer;

import io.quarkus.dev.spi.RemoteDevState;
import io.quarkus.vertx.http.runtime.devmode.RemoteSyncHandler;

class HttpRemoteDevClientTest {

    @Test
    void bodyRequestsUseFixedLengthAndResponsePolicyDistinguishesRetryableFromTerminal() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        CountDownLatch devRequest = new CountDownLatch(1);
        AtomicInteger devRequests = new AtomicInteger();
        AtomicLong connectLength = new AtomicLong(-1);
        AtomicLong devLength = new AtomicLong(-1);
        AtomicReference<String> connectTransferEncoding = new AtomicReference<>();
        AtomicReference<String> devTransferEncoding = new AtomicReference<>();
        server.createContext("/", exchange -> {
            try (exchange; var input = exchange.getRequestBody()) {
                input.readAllBytes();
                if (exchange.getRequestURI().getPath().endsWith(RemoteSyncHandler.CONNECT)) {
                    connectLength.set(parseContentLength(exchange.getRequestHeaders().getFirst("Content-Length")));
                    connectTransferEncoding.set(exchange.getRequestHeaders().getFirst("Transfer-Encoding"));
                    exchange.getResponseHeaders().set(RemoteSyncHandler.QUARKUS_SESSION, "session");
                    exchange.sendResponseHeaders(200, 0);
                } else if (exchange.getRequestURI().getPath().endsWith(RemoteSyncHandler.DEV)) {
                    int request = devRequests.incrementAndGet();
                    devLength.set(parseContentLength(exchange.getRequestHeaders().getFirst("Content-Length")));
                    devTransferEncoding.set(exchange.getRequestHeaders().getFirst("Transfer-Encoding"));
                    if (request == 1) {
                        exchange.getResponseHeaders().set(RemoteSyncHandler.QUARKUS_ERROR,
                                "Remote dev request-body capacity is temporarily unavailable");
                        exchange.getResponseHeaders().set("Retry-After", "0");
                        exchange.sendResponseHeaders(503, -1);
                    } else {
                        exchange.getResponseHeaders().set(RemoteSyncHandler.QUARKUS_ERROR,
                                "Remote dev request body exceeds quarkus.http.limits.max-body-size");
                        exchange.sendResponseHeaders(413, -1);
                        devRequest.countDown();
                    }
                } else {
                    exchange.sendResponseHeaders(404, -1);
                }
            }
        });
        server.start();

        Closeable session = null;
        try {
            var client = new HttpRemoteDevClient(
                    "http://localhost:" + server.getAddress().getPort(),
                    "secret", Duration.ofSeconds(1), Duration.ofMillis(10), 5);
            session = client.sendConnectRequest(new RemoteDevState(Map.of(), null), ignored -> Map.of(), () -> null);

            assertThat(devRequest.await(5, TimeUnit.SECONDS)).isTrue();
            Thread.sleep(100);

            assertThat(connectLength).hasValueGreaterThan(0);
            assertThat(devLength).hasValueGreaterThan(0);
            assertThat(connectTransferEncoding).hasValue(null);
            assertThat(devTransferEncoding).hasValue(null);
            assertThat(devRequests).hasValue(2);
        } finally {
            if (session != null) {
                session.close();
            }
            server.stop(0);
        }
    }

    private static long parseContentLength(String value) {
        return value == null ? -1 : Long.parseLong(value);
    }
}
