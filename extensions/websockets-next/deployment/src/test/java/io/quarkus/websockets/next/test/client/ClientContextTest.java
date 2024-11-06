package io.quarkus.websockets.next.test.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.websockets.next.OnClose;
import io.quarkus.websockets.next.OnOpen;
import io.quarkus.websockets.next.OnTextMessage;
import io.quarkus.websockets.next.WebSocket;
import io.quarkus.websockets.next.WebSocketClient;
import io.quarkus.websockets.next.WebSocketClientConnection;
import io.quarkus.websockets.next.WebSocketConnector;
import io.smallrye.mutiny.Uni;

public class ClientContextTest {

    @RegisterExtension
    public static final QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot(root -> {
                root.addClasses(ServerEndpoint.class, ClientEndpoint.class);
            });

    @Inject
    WebSocketConnector<ClientEndpoint> connector;

    @TestHTTPResource("/")
    URI uri;

    @Test
    void testClient() throws InterruptedException {
        connector.baseUri(uri);
        WebSocketClientConnection conn1 = connector.connectAndAwait();
        WebSocketClientConnection conn2 = connector.connectAndAwait();
        assertTrue(ClientEndpoint.MESSAGE_LATCH.await(10, TimeUnit.SECONDS));
        if (Runtime.getRuntime().availableProcessors() > 1) {
            // Each client should be executed on a dedicated event loop thread
            assertEquals(2, ClientEndpoint.THREADS.size());
        } else {
            // Single core - the event pool is shared
            // Due to some CI weirdness it might happen that the system incorrectly reports single core
            // Therefore, the assert checks if the number of threads used is >= 1
            assertTrue(ClientEndpoint.THREADS.size() >= 1);
        }
        conn1.closeAndAwait();
        conn2.closeAndAwait();
        assertTrue(ClientEndpoint.CLOSED_LATCH.await(5, TimeUnit.SECONDS));
        assertTrue(ServerEndpoint.CLOSED_LATCH.await(5, TimeUnit.SECONDS));
    }

    @WebSocket(path = "/end")
    public static class ServerEndpoint {

        static final CountDownLatch CLOSED_LATCH = new CountDownLatch(1);

        @OnOpen
        String open() {
            return "Hello!";
        }

        @OnClose
        void close() {
            CLOSED_LATCH.countDown();
        }

    }

    @WebSocketClient(path = "/end")
    public static class ClientEndpoint {

        static final CountDownLatch MESSAGE_LATCH = new CountDownLatch(2);

        static final Set<String> THREADS = ConcurrentHashMap.newKeySet();

        static final CountDownLatch CLOSED_LATCH = new CountDownLatch(2);

        @OnTextMessage
        Uni<Void> onMessage(String message) {
            String thread = Thread.currentThread().getName();
            THREADS.add(thread);
            MESSAGE_LATCH.countDown();
            return Uni.createFrom().voidItem();
        }

        @OnClose
        void close() {
            CLOSED_LATCH.countDown();
        }

    }

}
