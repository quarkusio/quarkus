package io.quarkus.websockets.next.test.pingpong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.websockets.next.OnClose;
import io.quarkus.websockets.next.OnOpen;
import io.quarkus.websockets.next.OnPingMessage;
import io.quarkus.websockets.next.WebSocket;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.WebSocketClient;

public class ClientPingServerPongTest {

    @RegisterExtension
    public static final QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot(root -> {
                root.addClasses(Endpoint.class);
            });

    @Inject
    Vertx vertx;

    @TestHTTPResource("endpoint")
    URI endUri;

    @Test
    void testPingPong() throws InterruptedException, ExecutionException {
        WebSocketClient client = vertx.createWebSocketClient();
        try {
            Buffer ping = Buffer.buffer("ping");
            LinkedBlockingDeque<Buffer> message = new LinkedBlockingDeque<>();
            client
                    .connect(endUri.getPort(), endUri.getHost(), endUri.getPath())
                    .onComplete(r -> {
                        if (r.succeeded()) {
                            r.result().pongHandler(pong -> message.add(pong));
                            r.result().writePing(ping);
                        } else {
                            throw new IllegalStateException(r.cause());
                        }
                    });
            assertTrue(Endpoint.OPENED.await(5, TimeUnit.SECONDS));
            // The pong message should be sent by the server automatically and should be identical to the ping message
            assertEquals(ping, message.poll(10, TimeUnit.SECONDS));
            // The server endpoint should have been notified of the ping
            assertTrue(Endpoint.PING.await(5, TimeUnit.SECONDS));
            assertEquals(ping, Endpoint.PING_MESSAGE.get());
        } finally {
            client.close().toCompletionStage().toCompletableFuture().get();
            assertTrue(Endpoint.CLOSED.await(5, TimeUnit.SECONDS));
        }
    }

    @WebSocket(path = "/endpoint")
    public static class Endpoint {

        static final CountDownLatch OPENED = new CountDownLatch(1);
        static final CountDownLatch PING = new CountDownLatch(1);
        static final CountDownLatch CLOSED = new CountDownLatch(1);
        static final AtomicReference<Buffer> PING_MESSAGE = new AtomicReference<>();

        @OnOpen
        void open() {
            OPENED.countDown();
        }

        @OnPingMessage
        void ping(Buffer message) {
            PING_MESSAGE.set(message);
            PING.countDown();
        }

        @OnClose
        void close() {
            CLOSED.countDown();
        }

    }

}
