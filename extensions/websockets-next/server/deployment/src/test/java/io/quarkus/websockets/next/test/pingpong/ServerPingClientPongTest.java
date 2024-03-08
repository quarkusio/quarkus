package io.quarkus.websockets.next.test.pingpong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.websockets.next.OnOpen;
import io.quarkus.websockets.next.OnPongMessage;
import io.quarkus.websockets.next.WebSocket;
import io.quarkus.websockets.next.WebSocketConnection;
import io.smallrye.mutiny.Uni;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.WebSocketClient;

public class ServerPingClientPongTest {

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
            CountDownLatch connectedLatch = new CountDownLatch(1);
            client
                    .connect(endUri.getPort(), endUri.getHost(), endUri.getPath())
                    .onComplete(r -> {
                        if (r.succeeded()) {
                            connectedLatch.countDown();
                        } else {
                            throw new IllegalStateException(r.cause());
                        }
                    });
            assertTrue(connectedLatch.await(5, TimeUnit.SECONDS));
            // The pong message should be sent by the client automatically and should be identical to the ping message
            assertTrue(Endpoint.PONG.await(5, TimeUnit.SECONDS));
        } finally {
            client.close().toCompletionStage().toCompletableFuture().get();
        }
    }

    @WebSocket(path = "/endpoint")
    public static class Endpoint {

        private final Buffer ping = Buffer.buffer("ping");
        static final CountDownLatch PONG = new CountDownLatch(1);

        @Inject
        WebSocketConnection connection;

        @OnOpen
        Uni<Void> open() {
            return connection.sendPing(ping);
        }

        @OnPongMessage
        void pong(Buffer data) {
            assertEquals(ping, data);
            PONG.countDown();
        }

    }

}
