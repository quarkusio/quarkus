package io.quarkus.websockets.next.test.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import jakarta.enterprise.event.ObservesAsync;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.websockets.next.Closed;
import io.quarkus.websockets.next.OnClose;
import io.quarkus.websockets.next.OnOpen;
import io.quarkus.websockets.next.OnTextMessage;
import io.quarkus.websockets.next.Open;
import io.quarkus.websockets.next.WebSocket;
import io.quarkus.websockets.next.WebSocketClient;
import io.quarkus.websockets.next.WebSocketClientConnection;
import io.quarkus.websockets.next.WebSocketConnector;
import io.quarkus.websockets.next.test.utils.WSClient;

public class ClientConnectionEventsTest {

    @RegisterExtension
    public static final QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot(root -> {
                root.addClasses(Endpoint.class, ObservingBean.class, WSClient.class);
            });

    @TestHTTPResource("/")
    URI baseUri;

    @Inject
    WebSocketConnector<EndpointClient> connector;

    @Test
    void testEvents() throws Exception {
        // Open connection, EndpointClient sends a message with client connection id
        WebSocketClientConnection connection = connector
                .baseUri(baseUri)
                .connectAndAwait();
        // Wait for the message
        assertTrue(Endpoint.MESSAGE_LATCH.await(5, TimeUnit.SECONDS));
        // Assert the @Open event was fired
        assertTrue(ObservingBean.OPEN_LATCH.await(5, TimeUnit.SECONDS));
        assertNotNull(ObservingBean.OPEN_CONN.get());
        assertEquals(connection.id(), ObservingBean.OPEN_CONN.get().id());
        assertEquals(connection.id(), Endpoint.MESSAGE.get());
        // Close the connection
        connection.closeAndAwait();
        assertTrue(EndpointClient.CLOSED_LATCH.await(5, TimeUnit.SECONDS));
        // Assert the @Closed event was fired
        assertTrue(ObservingBean.CLOSED_LATCH.await(5, TimeUnit.SECONDS));
        assertNotNull(ObservingBean.CLOSED_CONN.get());
        assertEquals(connection.id(), ObservingBean.CLOSED_CONN.get().id());
    }

    @WebSocket(path = "/endpoint")
    public static class Endpoint {

        static final AtomicReference<String> MESSAGE = new AtomicReference<>();

        static final CountDownLatch MESSAGE_LATCH = new CountDownLatch(1);

        @OnTextMessage
        void message(String message) {
            MESSAGE.set(message);
            MESSAGE_LATCH.countDown();
        }

    }

    @WebSocketClient(path = "/endpoint")
    public static class EndpointClient {

        static final CountDownLatch CLOSED_LATCH = new CountDownLatch(1);

        @OnOpen
        String open(WebSocketClientConnection connection) {
            return connection.id();
        }

        @OnClose
        void close() {
            CLOSED_LATCH.countDown();
        }

    }

    @Singleton
    public static class ObservingBean {

        static final CountDownLatch OPEN_LATCH = new CountDownLatch(1);
        static final CountDownLatch CLOSED_LATCH = new CountDownLatch(1);

        static final AtomicReference<WebSocketClientConnection> OPEN_CONN = new AtomicReference<>();
        static final AtomicReference<WebSocketClientConnection> CLOSED_CONN = new AtomicReference<>();

        void onOpen(@ObservesAsync @Open WebSocketClientConnection connection) {
            OPEN_CONN.set(connection);
            OPEN_LATCH.countDown();
        }

        void onClose(@ObservesAsync @Closed WebSocketClientConnection connection) {
            CLOSED_CONN.set(connection);
            CLOSED_LATCH.countDown();
        }

    }

}
