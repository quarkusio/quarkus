package io.quarkus.websockets.next.test.closereason;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.websockets.next.BasicWebSocketConnector;
import io.quarkus.websockets.next.CloseReason;
import io.quarkus.websockets.next.OnClose;
import io.quarkus.websockets.next.OnOpen;
import io.quarkus.websockets.next.WebSocket;
import io.quarkus.websockets.next.WebSocketClientConnection;
import io.vertx.core.Vertx;

public class ClientCloseReasonTest {

    @RegisterExtension
    public static final QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot(root -> {
                root.addClasses(Closing.class);
            });

    @Inject
    Vertx vertx;

    @TestHTTPResource("closing")
    URI closingUri;

    @Test
    public void testClosed() throws InterruptedException {
        CountDownLatch closedClientLatch = new CountDownLatch(1);
        AtomicReference<Integer> closeStatusCode = new AtomicReference<>();
        AtomicReference<String> closeMessage = new AtomicReference<>();
        WebSocketClientConnection connection = BasicWebSocketConnector
                .create()
                .baseUri(closingUri)
                .onClose((c, cr) -> {
                    closeStatusCode.set((int) cr.getCode());
                    closeMessage.set(cr.getMessage());
                    closedClientLatch.countDown();
                })
                .connectAndAwait();
        connection.closeAndAwait(new CloseReason(4001, "foo"));
        assertTrue(Closing.CLOSED.await(5, TimeUnit.SECONDS));
        assertTrue(closedClientLatch.await(5, TimeUnit.SECONDS));
        assertEquals(4001, closeStatusCode.get());
        assertEquals("foo", closeMessage.get());
    }

    @WebSocket(path = "/closing")
    public static class Closing {

        static final CountDownLatch CLOSED = new CountDownLatch(1);

        @OnOpen
        public String open() {
            return "ready";
        }

        @OnClose
        void onClose(CloseReason reason) {
            assertEquals(4001, reason.getCode());
            assertEquals("foo", reason.getMessage());
            CLOSED.countDown();
        }

    }

}
