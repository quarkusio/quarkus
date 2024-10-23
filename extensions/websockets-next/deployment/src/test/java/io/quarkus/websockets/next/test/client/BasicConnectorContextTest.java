package io.quarkus.websockets.next.test.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.websockets.next.BasicWebSocketConnector;
import io.quarkus.websockets.next.OnClose;
import io.quarkus.websockets.next.OnOpen;
import io.quarkus.websockets.next.WebSocket;
import io.quarkus.websockets.next.WebSocketClientConnection;

public class BasicConnectorContextTest {

    @RegisterExtension
    public static final QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot(root -> {
                root.addClasses(ServerEndpoint.class);
            });

    @TestHTTPResource("/end")
    URI uri;

    static final CountDownLatch MESSAGE_LATCH = new CountDownLatch(2);

    static final Set<String> THREADS = ConcurrentHashMap.newKeySet();

    static final CountDownLatch CLOSED_LATCH = new CountDownLatch(2);

    @Test
    void testClient() throws InterruptedException {
        BasicWebSocketConnector connector = BasicWebSocketConnector.create();
        connector
                .executionModel(BasicWebSocketConnector.ExecutionModel.NON_BLOCKING)
                .onTextMessage((c, m) -> {
                    String thread = Thread.currentThread().getName();
                    THREADS.add(thread);
                    MESSAGE_LATCH.countDown();
                })
                .onClose((c, cr) -> {
                    CLOSED_LATCH.countDown();
                })
                .baseUri(uri);
        WebSocketClientConnection conn1 = connector.connectAndAwait();
        WebSocketClientConnection conn2 = connector.connectAndAwait();
        assertTrue(MESSAGE_LATCH.await(10, TimeUnit.SECONDS));
        if (Runtime.getRuntime().availableProcessors() > 1) {
            // Each client should be executed on a dedicated event loop thread
            assertEquals(2, THREADS.size());
        } else {
            // Single core - the event pool is shared
            // Due to some CI weirdness it might happen that the system incorrectly reports single core
            // Therefore, the assert checks if the number of threads used is >= 1
            assertTrue(THREADS.size() >= 1);
        }
        conn1.closeAndAwait();
        conn2.closeAndAwait();
        assertTrue(ServerEndpoint.CLOSED_LATCH.await(5, TimeUnit.SECONDS));
        assertTrue(CLOSED_LATCH.await(5, TimeUnit.SECONDS));
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

}
