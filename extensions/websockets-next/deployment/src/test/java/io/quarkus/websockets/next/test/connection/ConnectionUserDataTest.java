package io.quarkus.websockets.next.test.connection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.net.URI;
import java.util.List;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.websockets.next.OnOpen;
import io.quarkus.websockets.next.OnTextMessage;
import io.quarkus.websockets.next.OpenConnections;
import io.quarkus.websockets.next.UserData.TypedKey;
import io.quarkus.websockets.next.WebSocket;
import io.quarkus.websockets.next.WebSocketConnection;
import io.quarkus.websockets.next.test.utils.WSClient;
import io.vertx.core.Vertx;

public class ConnectionUserDataTest {

    @RegisterExtension
    public static final QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot(root -> {
                root.addClasses(MyEndpoint.class, WSClient.class);
            });

    @Inject
    Vertx vertx;

    @TestHTTPResource("/end")
    URI baseUri;

    @Inject
    OpenConnections connections;

    @Test
    void testConnectionData() {
        try (WSClient client = WSClient.create(vertx).connect(baseUri)) {
            assertEquals("5", client.sendAndAwaitReply("bar").toString());
            assertNotNull(connections.stream().filter(c -> c.userData().get(TypedKey.forString("username")) != null).findFirst()
                    .orElse(null));
            assertEquals("FOOMartin", client.sendAndAwaitReply("foo").toString());
            assertEquals("0", client.sendAndAwaitReply("bar").toString());
        }
    }

    @WebSocket(path = "/end")
    public static class MyEndpoint {

        @OnOpen
        void onOpen(WebSocketConnection connection) {
            connection.userData().put(TypedKey.forInt("baz"), 5);
            connection.userData().put(TypedKey.forLong("foo"), 42l);
            connection.userData().put(TypedKey.forString("username"), "Martin");
            connection.userData().put(TypedKey.forBoolean("isActive"), true);
            connection.userData().put(new TypedKey<List<String>>("list"), List.of());
        }

        @OnTextMessage
        public String onMessage(String message, WebSocketConnection connection) {
            if ("bar".equals(message)) {
                return connection.userData().size() + "";
            }
            try {
                connection.userData().get(TypedKey.forString("foo")).toString();
                throw new IllegalStateException();
            } catch (ClassCastException expected) {
            }
            if (!connection.userData().get(TypedKey.forBoolean("isActive"))
                    || !connection.userData().get(new TypedKey<List<String>>("list")).isEmpty()) {
                return "NOK";
            }
            if (connection.userData().remove(TypedKey.forLong("foo")) != 42l) {
                throw new IllegalStateException();
            }
            if (connection.userData().remove(TypedKey.forInt("baz")) != 5) {
                throw new IllegalStateException();
            }
            String ret = message.toUpperCase() + connection.userData().get(TypedKey.forString("username"));
            connection.userData().clear();
            return ret;
        }

    }

}
