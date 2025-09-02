package io.quarkus.websockets.next.test.connection;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

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
import io.quarkus.websockets.next.test.utils.WSClient;

public class ConnectionIdleTimeoutTest {

    @RegisterExtension
    public static final QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot(root -> {
                root.addClasses(ServerEndpoint.class, ClientEndpoint.class, WSClient.class);
            }).overrideConfigKey("quarkus.websockets-next.client.connection-idle-timeout", "500ms");;

    @TestHTTPResource("/")
    URI uri;

    @Inject
    WebSocketConnector<ClientEndpoint> connector;

    @Test
    public void testTimeout() throws InterruptedException {
        WebSocketClientConnection conn = connector.baseUri(uri.toString()).connectAndAwait();
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            TimeUnit.MILLISECONDS.sleep(500);
            executor.execute(() -> {
                try {
                    conn.sendTextAndAwait("ok");
                } catch (Throwable ignored) {
                }
            });
        } finally {
            executor.shutdownNow();
        }
        assertTrue(ServerEndpoint.CLOSED.await(5, TimeUnit.SECONDS));
        assertTrue(ClientEndpoint.CLOSED.await(5, TimeUnit.SECONDS));
        assertFalse(ServerEndpoint.MESSAGE.get());
    }

    @WebSocket(path = "/end")
    public static class ServerEndpoint {

        static final CountDownLatch CLOSED = new CountDownLatch(1);
        static final AtomicBoolean MESSAGE = new AtomicBoolean();

        @OnTextMessage
        void onText(String message) {
            MESSAGE.set(true);
        }

        @OnClose
        void close() {
            CLOSED.countDown();
        }

    }

    @WebSocketClient(path = "/end")
    public static class ClientEndpoint {

        static final CountDownLatch CLOSED = new CountDownLatch(1);

        @OnOpen
        void open() {
        }

        @OnClose
        void close(WebSocketClientConnection conn) {
            CLOSED.countDown();
        }

    }
}
