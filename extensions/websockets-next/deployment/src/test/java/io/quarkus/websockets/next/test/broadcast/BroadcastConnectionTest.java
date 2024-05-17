package io.quarkus.websockets.next.test.broadcast;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.vertx.core.Vertx;
import io.vertx.core.http.WebSocket;
import io.vertx.core.http.WebSocketClient;

public class BroadcastConnectionTest {

    @RegisterExtension
    public static final QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot(root -> {
                root.addClasses(LoConnection.class);
            });

    @TestHTTPResource("lo-connection")
    URI loConnectionUri;

    @Inject
    Vertx vertx;

    @Test
    public void testBroadcast() throws Exception {
        WebSocketClient client1 = null, client2 = null, client3 = null;
        try {
            List<String> messages = new CopyOnWriteArrayList<>();
            client1 = connect(vertx, "C1", messages);
            client2 = connect(vertx, "C2", messages);
            client3 = connect(vertx, "C3", messages);
            // All client are connected
            assertEquals(3, messages.size());
            for (String message : messages) {
                assertTrue(message.equals("c1") || message.equals("c2") || message.equals("c3"));
            }
        } finally {
            if (client1 != null) {
                client1.close().toCompletionStage().toCompletableFuture().get();
            }
            if (client2 != null) {
                client2.close().toCompletionStage().toCompletableFuture().get();
            }
            if (client3 != null) {
                client3.close().toCompletionStage().toCompletableFuture().get();
            }
        }
    }

    WebSocketClient connect(Vertx vertx, String clientId, List<String> messages) throws InterruptedException {
        WebSocketClient client = vertx.createWebSocketClient();
        CountDownLatch connectedLatch = new CountDownLatch(1);
        CountDownLatch messageLatch = new CountDownLatch(1);
        client
                .connect(loConnectionUri.getPort(), loConnectionUri.getHost(), loConnectionUri.getPath() + "/" + clientId)
                .onComplete(r -> {
                    if (r.succeeded()) {
                        WebSocket ws = r.result();
                        ws.textMessageHandler(msg -> {
                            messages.add(msg);
                            messageLatch.countDown();
                        });
                        connectedLatch.countDown();
                    } else {
                        throw new IllegalStateException(r.cause());
                    }
                });
        assertTrue(connectedLatch.await(5, TimeUnit.SECONDS));
        assertTrue(messageLatch.await(5, TimeUnit.SECONDS));
        return client;
    }

}
