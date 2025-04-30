package io.quarkus.websockets.next.test.pingpong;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URI;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.websockets.next.OnOpen;
import io.quarkus.websockets.next.WebSocket;
import io.quarkus.websockets.next.WebSocketConnection;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.WebSocketClient;

public class ServerUnsolicitedPongTest {

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
            LinkedBlockingDeque<Buffer> message = new LinkedBlockingDeque<>();
            client
                    .connect(endUri.getPort(), endUri.getHost(), endUri.getPath())
                    .onComplete(r -> {
                        if (r.succeeded()) {
                            r.result().pongHandler(pong -> message.add(pong));
                        } else {
                            throw new IllegalStateException(r.cause());
                        }
                    });
            assertEquals(Buffer.buffer("pong"), message.poll(10, TimeUnit.SECONDS));
        } finally {
            client.close().toCompletionStage().toCompletableFuture().get();
        }
    }

    @WebSocket(path = "/endpoint")
    public static class Endpoint {

        @Inject
        WebSocketConnection connection;

        @OnOpen
        void open() {
            connection.sendPongAndAwait(Buffer.buffer("pong"));
        }

    }

}
