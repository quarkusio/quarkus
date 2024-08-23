package io.quarkus.websockets.next.test.openconnections;

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
import io.quarkus.websockets.next.Open;
import io.quarkus.websockets.next.WebSocket;
import io.quarkus.websockets.next.WebSocketConnection;
import io.quarkus.websockets.next.test.utils.WSClient;
import io.vertx.core.Vertx;

public class ConnectionEventsTest {

    @RegisterExtension
    public static final QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot(root -> {
                root.addClasses(Endpoint.class, ObservingBean.class, WSClient.class);
            });

    @Inject
    Vertx vertx;

    @TestHTTPResource("endpoint")
    URI endUri;

    @Test
    void testEvents() throws Exception {
        String client1ConnectionId;
        try (WSClient client1 = WSClient.create(vertx).connect(endUri)) {
            client1.waitForMessages(1);
            client1ConnectionId = client1.getMessages().get(0).toString();
            assertTrue(ObservingBean.OPEN_LATCH.await(5, TimeUnit.SECONDS));
            assertNotNull(ObservingBean.OPEN_CONN.get());
            assertEquals(client1ConnectionId, ObservingBean.OPEN_CONN.get().id());
        }
        assertTrue(Endpoint.CLOSED_LATCH.await(5, TimeUnit.SECONDS));
        assertTrue(ObservingBean.CLOSED_LATCH.await(5, TimeUnit.SECONDS));
        assertNotNull(ObservingBean.CLOSED_CONN.get());
        assertEquals(client1ConnectionId, ObservingBean.CLOSED_CONN.get().id());
    }

    @WebSocket(path = "/endpoint")
    public static class Endpoint {

        static final CountDownLatch CLOSED_LATCH = new CountDownLatch(1);

        @OnOpen
        String open(WebSocketConnection connection) {
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

        static final AtomicReference<WebSocketConnection> OPEN_CONN = new AtomicReference<>();
        static final AtomicReference<WebSocketConnection> CLOSED_CONN = new AtomicReference<>();

        void onOpen(@ObservesAsync @Open WebSocketConnection connection) {
            OPEN_CONN.set(connection);
            OPEN_LATCH.countDown();
        }

        void onClose(@ObservesAsync @Closed WebSocketConnection connection) {
            CLOSED_CONN.set(connection);
            CLOSED_LATCH.countDown();
        }

    }

}
