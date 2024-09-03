package io.quarkus.websockets.next.test.client;

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
import io.quarkus.websockets.next.OnClose;
import io.quarkus.websockets.next.OnTextMessage;
import io.quarkus.websockets.next.WebSocket;
import io.quarkus.websockets.next.WebSocketClient;
import io.quarkus.websockets.next.WebSocketClientConnection;
import io.quarkus.websockets.next.WebSocketConnector;

public class ClientEndpointEmptyPathTest {

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
        WebSocketClientConnection connection = connector
                .baseUri(uri)
                .connectAndAwait();
        connection.sendTextAndAwait("Hi!");

        assertTrue(ClientEndpoint.MESSAGE_LATCH.await(5, TimeUnit.SECONDS));
        assertEquals("Hi!", ClientEndpoint.MESSAGES.get(0));

        connection.closeAndAwait();
        assertTrue(ClientEndpoint.CLOSED_LATCH.await(5, TimeUnit.SECONDS));
        assertTrue(ServerEndpoint.CLOSED_LATCH.await(5, TimeUnit.SECONDS));
    }

    @WebSocket(path = "")
    public static class ServerEndpoint {

        static final CountDownLatch CLOSED_LATCH = new CountDownLatch(1);

        @OnTextMessage
        String echo(String message) {
            return message;
        }

        @OnClose
        void close() {
            CLOSED_LATCH.countDown();
        }

    }

    @WebSocketClient(path = "")
    public static class ClientEndpoint {

        static final CountDownLatch MESSAGE_LATCH = new CountDownLatch(1);

        static final List<String> MESSAGES = new CopyOnWriteArrayList<>();

        static final CountDownLatch CLOSED_LATCH = new CountDownLatch(1);

        @OnTextMessage
        void onMessage(String message, WebSocketClientConnection connection) {
            MESSAGES.add(message);
            MESSAGE_LATCH.countDown();
        }

        @OnClose
        void close() {
            CLOSED_LATCH.countDown();
        }

    }

}
