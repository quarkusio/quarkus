package io.quarkus.websockets.next.test.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.websockets.next.BasicWebSocketConnector;
import io.quarkus.websockets.next.BasicWebSocketConnector.ExecutionModel;
import io.quarkus.websockets.next.OnClose;
import io.quarkus.websockets.next.OnOpen;
import io.quarkus.websockets.next.OnTextMessage;
import io.quarkus.websockets.next.PathParam;
import io.quarkus.websockets.next.UserData;
import io.quarkus.websockets.next.UserData.TypedKey;
import io.quarkus.websockets.next.WebSocket;
import io.quarkus.websockets.next.WebSocketClientConnection;
import io.vertx.core.Context;

public class BasicConnectorTest {

    @RegisterExtension
    public static final QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot(root -> {
                root.addClasses(ServerEndpoint.class);
            });

    @Inject
    BasicWebSocketConnector connector;

    @TestHTTPResource("/end")
    URI uri;

    @Test
    void testClient() throws InterruptedException {

        assertThrows(IllegalArgumentException.class, () -> connector.baseUri("foo\\"));
        assertThrows(NullPointerException.class, () -> connector.path(null));
        assertThrows(NullPointerException.class, () -> connector.addHeader(null, "foo"));
        assertThrows(NullPointerException.class, () -> connector.addHeader("foo", null));
        assertThrows(NullPointerException.class, () -> connector.pathParam(null, "foo"));
        assertThrows(NullPointerException.class, () -> connector.pathParam("foo", null));
        assertThrows(NullPointerException.class, () -> connector.addSubprotocol(null));
        assertThrows(NullPointerException.class, () -> connector.executionModel(null));
        assertThrows(NullPointerException.class, () -> connector.onBinaryMessage(null));
        assertThrows(NullPointerException.class, () -> connector.onTextMessage(null));
        assertThrows(NullPointerException.class, () -> connector.onOpen(null));
        assertThrows(NullPointerException.class, () -> connector.onClose(null));
        assertThrows(NullPointerException.class, () -> connector.onPing(null));
        assertThrows(NullPointerException.class, () -> connector.onPong(null));
        assertThrows(NullPointerException.class, () -> connector.onError(null));

        CountDownLatch openLatch = new CountDownLatch(1);
        AtomicReference<UserData> userData = new AtomicReference<>();
        CountDownLatch messageLatch = new CountDownLatch(2);
        List<String> messages = new CopyOnWriteArrayList<>();
        CountDownLatch closedLatch = new CountDownLatch(1);
        WebSocketClientConnection connection1 = connector
                .baseUri(uri)
                .path("/{name}")
                .pathParam("name", "Lu")
                .userData(TypedKey.forBoolean("boolean"), true)
                .userData(TypedKey.forInt("int"), Integer.MAX_VALUE)
                .userData(TypedKey.forLong("long"), Long.MAX_VALUE)
                .userData(TypedKey.forString("string"), "Lu")
                .onOpen(c -> {
                    userData.set(c.userData());
                    openLatch.countDown();
                })
                .onTextMessage((c, m) -> {
                    assertTrue(Context.isOnWorkerThread());
                    String name = c.pathParam("name");
                    messages.add(name + ":" + m);
                    messageLatch.countDown();
                })
                .onClose((c, s) -> closedLatch.countDown())
                .connectAndAwait();
        assertEquals("Lu", connection1.pathParam("name"));
        assertTrue(connection1.userData().get(TypedKey.forBoolean("boolean")));
        assertEquals(Integer.MAX_VALUE, connection1.userData().get(TypedKey.forInt("int")));
        assertEquals(Long.MAX_VALUE, connection1.userData().get(TypedKey.forLong("long")));
        assertEquals("Lu", connection1.userData().get(TypedKey.forString("string")));
        connection1.sendTextAndAwait("Hi!");

        assertTrue(openLatch.await(5, TimeUnit.SECONDS));
        assertNotNull(userData.get());
        assertTrue(userData.get().get(TypedKey.forBoolean("boolean")));
        assertEquals(Integer.MAX_VALUE, userData.get().get(TypedKey.forInt("int")));
        assertEquals(Long.MAX_VALUE, userData.get().get(TypedKey.forLong("long")));
        assertEquals("Lu", userData.get().get(TypedKey.forString("string")));

        assertTrue(messageLatch.await(5, TimeUnit.SECONDS));
        // Note that ordering is not guaranteed
        assertThat(messages.get(0)).isIn("Lu:Hello Lu!", "Lu:Hi!");
        assertThat(messages.get(1)).isIn("Lu:Hello Lu!", "Lu:Hi!");

        connection1.closeAndAwait();
        assertTrue(closedLatch.await(5, TimeUnit.SECONDS));
        assertTrue(ServerEndpoint.CLOSED_LATCH.await(5, TimeUnit.SECONDS));

        CountDownLatch conn2Latch = new CountDownLatch(1);
        WebSocketClientConnection connection2 = BasicWebSocketConnector
                .create()
                .baseUri(uri)
                .path("/Cool")
                .executionModel(ExecutionModel.NON_BLOCKING)
                .addHeader("X-Test", "foo")
                .onTextMessage((c, m) -> {
                    assertTrue(Context.isOnEventLoopThread());
                    // Path params not set
                    assertNull(c.pathParam("name"));
                    assertTrue(c.handshakeRequest().path().endsWith("Cool"));
                    assertEquals("foo", c.handshakeRequest().header("X-Test"));
                    conn2Latch.countDown();
                })
                .connectAndAwait();
        assertNotNull(connection2);
        assertTrue(conn2Latch.await(5, TimeUnit.SECONDS));
    }

    @WebSocket(path = "/end/{name}")
    public static class ServerEndpoint {

        static final CountDownLatch CLOSED_LATCH = new CountDownLatch(1);

        @OnOpen
        String open(@PathParam String name) {
            return "Hello " + name + "!";
        }

        @OnTextMessage
        String echo(String message) {
            return message;
        }

        @OnClose
        void close() {
            CLOSED_LATCH.countDown();
        }

    }

}
