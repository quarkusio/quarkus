package io.quarkus.websockets.next.test.connection;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import jakarta.inject.Inject;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.websockets.next.OnClose;
import io.quarkus.websockets.next.OnTextMessage;
import io.quarkus.websockets.next.WebSocket;
import io.quarkus.websockets.next.WebSocketConnection;
import io.quarkus.websockets.next.test.utils.WSClient;
import io.smallrye.mutiny.Uni;
import io.vertx.core.Vertx;

public class ConnectionCloseTest {

    @RegisterExtension
    public static final QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot(root -> {
                root.addClasses(Closing.class, ClosingBlocking.class, WSClient.class);
            });

    @Inject
    Vertx vertx;

    @TestHTTPResource("closing")
    URI closingUri;

    @TestHTTPResource("closing-blocking")
    URI closingBlockingUri;

    @Test
    public void testClosed() throws InterruptedException {
        assertClosed(closingUri);
        assertTrue(Closing.CLOSED.await(5, TimeUnit.SECONDS));
    }

    @Test
    public void testClosedBlocking() throws InterruptedException {
        assertClosed(closingBlockingUri);
        assertTrue(ClosingBlocking.CLOSED.await(5, TimeUnit.SECONDS));
    }

    private void assertClosed(URI testUri) throws InterruptedException {
        WSClient client = WSClient.create(vertx).connect(testUri);
        client.sendAndAwait("foo");
        Awaitility.await().atMost(5, TimeUnit.SECONDS).until(() -> client.isClosed());
    }

    @WebSocket(path = "/closing")
    public static class Closing {

        static final CountDownLatch CLOSED = new CountDownLatch(1);

        @Inject
        WebSocketConnection connection;

        @OnTextMessage
        public Uni<Void> onMessage(String message) {
            return connection.close();
        }

        @OnClose
        void onClose() {
            CLOSED.countDown();
        }

    }

    @WebSocket(path = "/closing-blocking")
    public static class ClosingBlocking {

        static final CountDownLatch CLOSED = new CountDownLatch(1);

        @Inject
        WebSocketConnection connection;

        @OnTextMessage
        public void onMessage(String message) {
            connection.closeAndAwait();
        }

        @OnClose
        void onClose() {
            CLOSED.countDown();
        }

    }

}
